package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine.modules.RenderModule;
import com.farias.rengine.engine.modules.WindowModule;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;

/**
 * Cubo girando projetando sombra sobre um piso (shadow mapping: pass de
 * profundidade do ponto de vista da luz + pass forward que amostra o shadow
 * map). Ao contrario dos outros exemplos, este nao estende Game - monta
 * Engine/EngineContext/modulos e roda o loop na mao, para mostrar esse nivel
 * mais baixo de composicao tambem (o mesmo estilo de Example.java).
 */
public class ShadowCubeDemo {

    private Mesh boxMesh;
    private Mesh floorMesh;
    private Material boxMaterial;
    private Material floorMaterial;
    private Material shadowMaterial;

    private final Vector3f boxPosition = new Vector3f(0f, 0f, -2f);
    private final Vector3f boxRotation = new Vector3f();
    private final Vector3f boxScale = new Vector3f(1f, 1f, 1f);
    private final Matrix4f floorModelMatrix = new Matrix4f()
            .translate(0f, -1f, -2f)
            .scale(10f, 1f, 10f);

    private RenderView renderView;
    private RenderView shadowView;
    private Matrix4f lightSpaceMatrix;
    private float angle;

    private RenderPassDescriptor forwardDescriptor;
    private RenderPassDescriptor shadowDescriptor;
    private RenderCommand boxCommand;
    private RenderCommand floorCommand;
    private RenderCommand shadowBoxCommand;

    public static void launch() {
        new ShadowCubeDemo().run();
    }

    public static void main(String[] args) {
        launch();
    }

    private void run() {
        Engine engine = new Engine();
        engine.registerModule(new WindowModule(1280, 720, "Shadow Cube Demo"));
        engine.registerModule(new RenderModule());
        EngineContext context = engine.start();

        Window window = context.resolve(Window.class);
        Renderer renderer = context.resolve(Renderer.class);
        Timer timer = new Timer();

        createScene(renderer, window.getFramebufferWidth(), window.getFramebufferHeight());

        while (!window.shouldClose()) {
            glfwPollEvents();

            timer.update();
            float deltaTime = timer.deltaTime();

            angle += Math.toRadians(90.0) * deltaTime;
            boxRotation.set(0f, angle, 0f);

            renderer.beginFrame();

            Matrix4f boxModelMatrix = getBoxModelMatrix();
            boxCommand.setModelMatrix(boxModelMatrix);
            shadowBoxCommand.setModelMatrix(boxModelMatrix);

            RenderPassNode shadowPass = renderer.createPass(shadowDescriptor, shadowView);
            shadowPass.submit(shadowBoxCommand);

            RenderPassNode forwardPass = renderer.createPass(forwardDescriptor, renderView);
            forwardPass.submit(boxCommand);
            forwardPass.submit(floorCommand);

            renderer.endFrame();

            window.swapBuffers();
        }

        engine.terminate();
    }

    private Matrix4f getBoxModelMatrix() {
        return new Matrix4f()
                .translate(boxPosition)
                .rotateXYZ(boxRotation.x, boxRotation.y, boxRotation.z)
                .scale(boxScale);
    }

    private void createScene(Renderer renderer, int width, int height) {
        this.renderView = RenderView.createDefault(width, height);
        this.boxMesh = renderer.createMesh(MeshData.createCube());
        this.floorMesh = renderer.createMesh(MeshData.createPlane());

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

        this.boxCommand = new RenderCommand(boxMesh, boxMaterial, new Matrix4f());
        this.floorCommand = new RenderCommand(floorMesh, floorMaterial, floorModelMatrix);
        this.shadowBoxCommand = new RenderCommand(boxMesh, shadowMaterial, new Matrix4f());
    }

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
