package com.farias.rengine.renderer.example;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.OpenGLBackend;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;

public class Example {

    // Componentes de Cena e Transformação
    private Mesh boxMesh;
    private Mesh floorMesh;
    private Material boxMaterial;
    private Material floorMaterial;
    private Material shadowMaterial;
    private Transform boxTransform;
    private Transform floorTransform;
    private RenderView renderView;
    private RenderView shadowView;
    private Matrix4f lightSpaceMatrix;
    private float angle;

    // Cache de Objetos para evitar o Garbage Collector no Loop Principal
    private RenderPassDescriptor forwardDescriptor;
    private RenderPassDescriptor shadowDescriptor;
    private RenderCommand boxCommand;
    private RenderCommand floorCommand;
    private RenderCommand shadowBoxCommand;

    private void createScene(EngineContext ctx) {
        var renderer = ctx.resolve(Renderer.class);
        var window = ctx.resolve(Window.class);
        FramebufferSize framebufferSize = window.framebufferSize();

        // 1. Configuração da Câmera e Instanciação de Malhas
        this.renderView = RenderView.createDefault(framebufferSize.width(), framebufferSize.height());
        this.boxMesh = renderer.createMesh(MeshData.createCube());
        this.floorMesh = renderer.createMesh(MeshData.createPlane());

        // 2. Configuração Espacial (Transforms)
        this.boxTransform = new Transform();
        this.floorTransform = new Transform(
                new Vector3f(0.0f, -1.0f, -2.0f), 
                new Vector3f(10.0f, 1.0f, 10.0f)
        );

        // 3. Configuração da luz e do Shadow Map
        Texture shadowMap = renderer.createDepthTexture(2048, 2048);
        Vector3f lightDirection = new Vector3f(-0.45f, 1.0f, 0.35f).normalize();
        Matrix4f lightView = new Matrix4f()
                .lookAt(
                        new Vector3f(lightDirection).mul(8.0f).add(0.0f, 0.0f, -2.0f),
                        new Vector3f(0.0f, 0.0f, -2.0f),
                        new Vector3f(0.0f, 1.0f, 0.0f));
        Matrix4f lightProjection = new Matrix4f()
                .ortho(-7.0f, 7.0f, -7.0f, 7.0f, 1.0f, 18.0f);
        this.lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);
        this.shadowView = new RenderView(shadowMap.getWidth(), shadowMap.getHeight(), lightProjection, lightView);

        // 4. Configuração de Material e Shaders
        Shader forwardShader = renderer.createShader(VERTEX_SHADER, FRAGMENT_SHADER);
        Shader shadowShader = renderer.createShader(SHADOW_VERTEX_SHADER, SHADOW_FRAGMENT_SHADER);

        this.boxMaterial = renderer.createMaterial(forwardShader);
        boxMaterial.setVector3f("objectColor", new Vector3f(1.0f, 0.5f, 0.2f));
        boxMaterial.setVector3f("lightDirection", lightDirection);
        boxMaterial.setFloat("ambientStrength", 0.2f);
        boxMaterial.setFloat("diffuseStrength", 0.8f);
        boxMaterial.setFloat("shadowBias", 0.004f);
        boxMaterial.setMatrix4f("lightSpaceMatrix", lightSpaceMatrix);
        boxMaterial.setTexture("shadowMap", shadowMap, 1);

        this.floorMaterial = renderer.createMaterial(forwardShader);
        floorMaterial.setVector3f("objectColor", new Vector3f(0.72f, 0.78f, 0.68f));
        floorMaterial.setVector3f("lightDirection", lightDirection);
        floorMaterial.setFloat("ambientStrength", 0.25f);
        floorMaterial.setFloat("diffuseStrength", 0.75f);
        floorMaterial.setFloat("shadowBias", 0.002f);
        floorMaterial.setMatrix4f("lightSpaceMatrix", lightSpaceMatrix);
        floorMaterial.setTexture("shadowMap", shadowMap, 1);

        this.shadowMaterial = renderer.createMaterial(shadowShader);

        // 5. PRÉ-ALOCAÇÃO: Objetos criados uma única vez na memória
        this.shadowDescriptor = new RenderPassDescriptor("shadow")
                .depthAttachment(shadowMap)
                .clearColorBuffer(false)
                .clearDepthBuffer(true)
                .depthTest(true)
                .cullFace(false);

        this.forwardDescriptor = new RenderPassDescriptor("forward")
                .clearColor(0.1f, 0.2f, 0.3f, 1f)
                .depthTest(true)
                .cullFace(true);

        // Inicializa os comandos com matrizes identidade (serão atualizadas no loop)
        this.boxCommand = new RenderCommand(boxMesh, boxMaterial, new Matrix4f());
        this.floorCommand = new RenderCommand(floorMesh, floorMaterial, floorTransform.getTransformMatrix());
        this.shadowBoxCommand = new RenderCommand(boxMesh, shadowMaterial, new Matrix4f());
    }

    public void run() {
        var engine = new SampleEngine();
        engine.registerModule(new RenderModule(), RenderModule.class);
        engine.registerModule(new WindowModule(), WindowModule.class);
        engine.registerModule(new UtilsModule(), UtilsModule.class);
        
        var ctx = engine.run();
        var window = ctx.resolve(Window.class);
        var timer = ctx.resolve(Timer.class);
        var renderer = ctx.resolve(Renderer.class);

        this.createScene(ctx);

        // LOOP PRINCIPAL (Zero alocações por frame aqui dentro!)
        while (!window.shouldClose()) {
            window.pollEvents();
            timer.updateTicks();

            // Game Logic
            angle += Math.toRadians(90.0f) * timer.deltaTime();
            boxTransform.getRotation().set(0f, angle, 0f);

            // Rendering
            if (timer.shouldRender()) {
                renderer.beginFrame();

                // Reutiliza e atualiza as matrizes dos comandos em vez de dar 'new'
                boxCommand.setModelMatrix(boxTransform.getTransformMatrix());
                floorCommand.setModelMatrix(floorTransform.getTransformMatrix());
                shadowBoxCommand.setModelMatrix(boxTransform.getTransformMatrix());

                // Primeiro passe: renderiza profundidade do cubo do ponto de vista da luz
                RenderPassNode shadowPass = renderer.createPass(shadowDescriptor, shadowView);
                shadowPass.submit(shadowBoxCommand);

                // Segundo passe: renderiza cena principal usando o shadow map
                RenderPassNode forwardPass = renderer.createPass(forwardDescriptor, renderView);

                // Submete os mesmos objetos reutilizados da memória
                forwardPass.submit(boxCommand);
                forwardPass.submit(floorCommand);

                renderer.endFrame();
                
                window.swapBuffers();
            }
        }
        engine.terminate();
    }

    public static void main(String[] args) {
        new Example().run();
    }

    // =========================================================================
    // GLSL SHADERS SOURCE CODE
    // =========================================================================

    private static final String VERTEX_SHADER = """
        #version 330 core
        layout(location = 0) in vec3 vertices;
        layout(location = 1) in vec2 textures;
        layout(location = 2) in vec3 normals;

        out vec2 tex_coords;
        out vec3 fragNormal;
        out vec4 fragLightSpace;

        uniform mat4 uView;
        uniform mat4 uProjection;
        uniform mat4 uModel;
        uniform mat3 uNormal;
        uniform mat4 lightSpaceMatrix;

        void main() {
            tex_coords = textures;
            fragNormal = normalize(uNormal * normals);
            vec4 worldPosition = uModel * vec4(vertices, 1.0);
            fragLightSpace = lightSpaceMatrix * worldPosition;
            gl_Position = uProjection * uView * worldPosition;
        }
    """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core
        uniform vec3 objectColor;
        uniform vec3 lightDirection;
        uniform float ambientStrength;
        uniform float diffuseStrength;
        uniform float shadowBias;
        uniform sampler2D shadowMap;

        in vec2 tex_coords;
        in vec3 fragNormal;
        in vec4 fragLightSpace;
        out vec4 fragColor;

        float calculateShadow(vec4 lightSpacePosition, vec3 normal, vec3 lightDir) {
            vec3 projectedCoords = lightSpacePosition.xyz / lightSpacePosition.w;
            projectedCoords = projectedCoords * 0.5 + 0.5;

            if (projectedCoords.z > 1.0) {
                return 0.0;
            }

            float bias = max(shadowBias * (1.0 - dot(normal, lightDir)), shadowBias * 0.25);
            float shadow = 0.0;
            vec2 texelSize = 1.0 / textureSize(shadowMap, 0);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    float closestDepth = texture(shadowMap, projectedCoords.xy + vec2(x, y) * texelSize).r;
                    shadow += projectedCoords.z - bias > closestDepth ? 1.0 : 0.0;
                }
            }

            return shadow / 9.0;
        }

        void main() {
            vec4 texColor = vec4(objectColor, 1.0); 

            vec3 normal = normalize(fragNormal);
            if (length(normal) == 0.0) normal = vec3(0.0, 1.0, 0.0);

            vec3 lightDir = lightDirection;
            if (length(lightDir) == 0.0) lightDir = vec3(-0.45, 1.0, 0.35);
            lightDir = normalize(lightDir);

            float diffuse = max(dot(normal, lightDir), 0.0);
            float shadow = calculateShadow(fragLightSpace, normal, lightDir);
            float light = ambientStrength + ((1.0 - shadow) * diffuse * diffuseStrength);

            fragColor = texColor * light;
        }
    """;

    private static final String SHADOW_VERTEX_SHADER = """
        #version 330 core
        layout(location = 0) in vec3 vertices;

        uniform mat4 uView;
        uniform mat4 uProjection;
        uniform mat4 uModel;

        void main() {
            gl_Position = uProjection * uView * uModel * vec4(vertices, 1.0);
        }
    """;

    private static final String SHADOW_FRAGMENT_SHADER = """
        #version 330 core

        void main() {
        }
    """;
}

// GAME DOMAIN

class Transform {
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Vector3f position;
    private final Vector3f rotation;
    private final Vector3f scale;

    // Construtor padrão (com os valores que o seu cubo já usava)
    public Transform() {
        this.position = new Vector3f(0.0f, 0.0f, -2.0f);
        this.rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        this.scale = new Vector3f(1.0f, 1.0f, 1.0f);
    }

    // Construtor customizado para podermos criar o chão em posições e escalas
    // diferentes
    public Transform(Vector3f position, Vector3f scale) {
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(0.0f, 0.0f, 0.0f);
        this.scale = new Vector3f(scale);
    }

    public Matrix4f getTransformMatrix() {
        return modelMatrix
                .identity()
                .translate(position)
                .rotateXYZ(rotation)
                .scale(scale);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }
}

// ENGINE DOMAIN

class SampleEngine {
    private final EngineContext context = new EngineContext();
    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();

    public <T extends Module> void registerModule(Module module, Class<T> clazz) {
        if (!clazz.isAssignableFrom(module.getClass())) {
            throw new RuntimeException("Module class does not match the expected type.");
        }
        modules.put(clazz, module);
    }

    public EngineContext run() {
        for (Module module : modules.values()) {
            module.onRegister(context);
        }
        return context;
    }

    public void terminate() {
        for (Module module : modules.values()) {
            module.onTerminate(context);
        }
    }
}

class EngineContext {
    Map<Class<?>, Object> services = new HashMap<>();

    public void registerService(Class<?> clazz, Object service) {
        services.put(clazz, service);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> clazz) {
        return (T) services.get(clazz);
    }
}

interface Module {
    void onRegister(EngineContext context);

    void onTerminate(EngineContext context);
}

// RENDERER DOMAIN

class RenderModule implements Module {
    private Renderer renderer;

    @Override
    public void onRegister(EngineContext context) {
        renderer = new Renderer(new OpenGLBackend());
        renderer.initialize();
        context.registerService(Renderer.class, renderer);
    }

    @Override
    public void onTerminate(EngineContext context) {
        renderer.dispose();
    }
}

// WINDOW DOMAIN
class WindowModule implements Module {
    private Window window;

    @Override
    public void onRegister(EngineContext context) {
        window = new Window(false);
        context.registerService(Window.class, window);
    }

    @Override
    public void onTerminate(EngineContext context) {
        window.destroy();
    }

}

class Window {
    final long windowHandle;

    public Window(boolean vsyncEnabled) {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE);

        windowHandle = GLFW.glfwCreateWindow(1280, 720, "REngine Renderer Example", MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFW.glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();
        if (vsyncEnabled) {
            GLFW.glfwSwapInterval(1);
        }
    }

    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public void swapBuffers() {
        GLFW.glfwSwapBuffers(windowHandle);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowHandle);
    }

    public void destroy() {
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
    }

    public long getWindowHandle() {
        return windowHandle;
    }

    public FramebufferSize framebufferSize() {
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetFramebufferSize(windowHandle, width, height);
        return new FramebufferSize(width[0], height[0]);
    }
}

record FramebufferSize(int width, int height) {
}

class UtilsModule implements Module {

    @Override
    public void onRegister(EngineContext context) {
        context.registerService(Timer.class, new Timer(60.0f));
    }

    @Override
    public void onTerminate(EngineContext context) {

    }

}

class Timer {
    private long lastUpdateTime;
    private long lastRenderTime;
    private final long renderIntervalNs;
    private float updateDeltaTime;

    public Timer(float targetRenderFps) {
        this.renderIntervalNs = (long) (1_000_000_000.0 / targetRenderFps);
        long now = System.nanoTime();
        this.lastUpdateTime = now;
        this.lastRenderTime = now;
        this.updateDeltaTime = 0.0f;
    }

    /**
     * Calcula o delta time do Update. Chamado a cada iteração do loop ilimitado.
     */
    public void updateTicks() {
        long now = System.nanoTime();
        this.updateDeltaTime = (now - lastUpdateTime) / 1_000_000_000.0f;
        this.lastUpdateTime = now;
    }

    /**
     * Verifica se já se passou tempo suficiente para renderizar o próximo quadro
     * (1/60s).
     * Se sim, atualiza o cronômetro do render e autoriza o desenho.
     */
    public boolean shouldRender() {
        long now = System.nanoTime();
        if (now - lastRenderTime >= renderIntervalNs) {
            this.lastRenderTime = now;
            return true;
        }
        return false;
    }

    /**
     * Retorna o delta time ultra-curto do update ilimitado.
     */
    public float deltaTime() {
        return this.updateDeltaTime;
    }
}
