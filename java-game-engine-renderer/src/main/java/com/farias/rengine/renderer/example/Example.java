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

public class Example {

    // Componentes de Cena e Transformação
    private Mesh boxMesh;
    private Mesh floorMesh;
    private Material boxMaterial;
    private Transform boxTransform;
    private Transform floorTransform;
    private RenderView renderView;
    private float angle;

    // Cache de Objetos para evitar o Garbage Collector no Loop Principal
    private RenderPassDescriptor forwardDescriptor;
    private RenderCommand boxCommand;
    private RenderCommand floorCommand;

    private void createScene(EngineContext ctx) {
        var renderer = ctx.resolve(Renderer.class);

        // 1. Configuração da Câmera e Instanciação de Malhas
        this.renderView = RenderView.createDefault(1280, 720);
        this.boxMesh = renderer.createMesh(MeshData.createCube());
        this.floorMesh = renderer.createMesh(MeshData.createPlane());

        // 2. Configuração Espacial (Transforms)
        this.boxTransform = new Transform();
        this.floorTransform = new Transform(
                new Vector3f(0.0f, -1.0f, -2.0f), 
                new Vector3f(10.0f, 1.0f, 10.0f)
        );

        // 3. Configuração de Material e Shaders
        this.boxMaterial = renderer.createMaterial(new Shader(VERTEX_SHADER, FRAGMENT_SHADER), null);
        boxMaterial.setVector3f("lightDirection", new Vector3f(-1.0f, -1.0f, -1.0f));
        boxMaterial.setFloat("ambientStrength", 0.2f);
        boxMaterial.setFloat("diffuseStrength", 0.8f);

        // 4. PRÉ-ALOCAÇÃO: Objetos criados uma única vez na memória
        this.forwardDescriptor = new RenderPassDescriptor("forward")
                .clearColor(0.1f, 0.2f, 0.3f, 1f)
                .depthTest(true)
                .cullFace(true);

        // Inicializa os comandos com matrizes identidade (serão atualizadas no loop)
        this.boxCommand = new RenderCommand(boxMesh, boxMaterial, new Matrix4f());
        this.floorCommand = new RenderCommand(floorMesh, boxMaterial, floorTransform.getTransformMatrix());
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

                // O renderer consome o descritor pré-alocado
                RenderPassNode forwardPass = renderer.createPass(forwardDescriptor, renderView);

                // Reutiliza e atualiza as matrizes dos comandos em vez de dar 'new'
                boxCommand.setModelMatrix(boxTransform.getTransformMatrix());
                floorCommand.setModelMatrix(floorTransform.getTransformMatrix());

                // Submete os mesmos objetos reutilizados da memória
                forwardPass.submit(boxCommand);
                forwardPass.submit(floorCommand);

                renderer.submitPass(forwardPass);
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

        uniform mat4 uView;
        uniform mat4 uProjection;
        uniform mat4 uModel;
        uniform mat3 uNormal;

        void main() {
            tex_coords = textures;
            fragNormal = normalize(uNormal * normals);
            gl_Position = uProjection * uView * uModel * vec4(vertices, 1.0);
        }
    """;

    private static final String FRAGMENT_SHADER = """
        #version 330 core
        uniform vec3 lightDirection;
        uniform float ambientStrength;
        uniform float diffuseStrength;

        in vec2 tex_coords;
        in vec3 fragNormal;
        out vec4 fragColor;

        void main() {
            vec4 texColor = vec4(1.0, 0.5, 0.2, 1.0); 

            vec3 normal = normalize(fragNormal);
            if (length(normal) == 0.0) normal = vec3(0.0, 1.0, 0.0);

            vec3 lightDir = lightDirection;
            if (length(lightDir) == 0.0) lightDir = vec3(-1.0, -1.0, -1.0);
            lightDir = normalize(lightDir);

            float diffuse = max(dot(normal, lightDir), 0.0);
            float light = ambientStrength + (diffuse * diffuseStrength);

            fragColor = texColor * light;
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