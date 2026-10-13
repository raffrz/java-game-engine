package com.farias.jengine.examples;

import java.util.List;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.jengine.core.Engine;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Timer;
import com.farias.jengine.toolkit3d.DirectionalLight;
import com.farias.jengine.toolkit3d.Projections;
import com.farias.jengine.toolkit3d.ShapeData;
import com.farias.jengine.renderer.RendererModule;
import com.farias.jengine.platform.glfw.GlfwPlatformModule;
import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.RenderCommand;
import com.farias.jengine.renderer.RenderPassDescriptor;
import com.farias.jengine.renderer.RenderPass;
import com.farias.jengine.renderer.RenderView;
import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.Shader;
import com.farias.jengine.renderer.Texture;

/**
 * Cubo girando projetando sombra sobre um piso (shadow mapping: pass de
 * profundidade do ponto de vista da luz + pass forward que amostra o shadow
 * map). Ao contrario dos outros exemplos, este usa o Renderer direto, sem os
 * servicos dos toolkits (do 3D, so funcoes e valores puros): cria shaders,
 * materiais e passes na mao, para mostrar esse nivel mais baixo de composicao
 * tambem.
 *
 * Estilo funcional: o estado e um valor imutavel (State), a simulacao e a
 * geracao de comandos sao funcoes puras, e os efeitos colaterais (GPU/janela)
 * ficam so em render e run.
 */
public final class ShadowCubeDemo {

    private static final Matrix4f FLOOR_MODEL = new Matrix4f()
            .translate(0f, -1f, -2f)
            .scale(10f, 1f, 10f);

    private ShadowCubeDemo() {
    }

    // ---- Tipos (valores imutaveis) ----

    /** Todo o estado mutavel da simulacao: so o angulo do cubo. */
    private record State(float angle) {
        static final State INITIAL = new State(0f);

        State advance(float deltaTime) {
            return new State(angle + (float) Math.toRadians(90.0) * deltaTime);
        }
    }

    /** Tudo que e criado uma vez so (GPU), passado explicitamente. */
    private record Resources(
            RenderView view,
            RenderView shadowView,
            RenderPassDescriptor shadowPass,
            RenderPassDescriptor forwardPass,
            Mesh boxMesh,
            Mesh floorMesh,
            Material boxMaterial,
            Material floorMaterial,
            Material shadowMaterial) {
    }

    private record LitParams(Vector3f color, float ambient, float diffuse, float shadowBias) {
    }

    // ---- Ponto de entrada e loop ----

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        run();
    }

    private static void run() {
        Engine engine = new Engine();
        engine.registerModule(new GlfwPlatformModule(1280, 720, "Shadow Cube Demo"));
        engine.registerModule(new RendererModule());
        EngineContext context = engine.start();

        try {
            Window window = context.require(Window.class);
            Renderer renderer = context.require(Renderer.class);
            Resources resources = createResources(renderer, window.getFramebufferWidth(),
                    window.getFramebufferHeight());
            Timer timer = new Timer();

            State state = State.INITIAL;
            while (!window.shouldClose()) {
                window.pollEvents();
                timer.update();
                state = update(state, timer.deltaTime());
                render(renderer, resources, state);
            }
        } finally {
            engine.terminate();
        }
    }

    // ---- Simulacao (pura) ----

    private static State update(State state, float deltaTime) {
        return state.advance(deltaTime);
    }

    // ---- Renderizacao ----

    private static void render(Renderer renderer, Resources r, State state) {
        Matrix4f boxModel = boxModel(state);

        renderer.beginFrame();
        submitPass(renderer, r.shadowPass(), r.shadowView(), shadowCommands(r, boxModel));
        submitPass(renderer, r.forwardPass(), r.view(), forwardCommands(r, boxModel));
        renderer.endFrame();
    }

    private static void submitPass(Renderer renderer, RenderPassDescriptor descriptor, RenderView view,
            List<RenderCommand> commands) {
        RenderPass pass = renderer.createPass(descriptor, view);
        commands.forEach(pass::submit);
    }

    private static Matrix4f boxModel(State state) {
        return new Matrix4f()
                .translate(0f, 0f, -2f)
                .rotateY(state.angle());
    }

    private static List<RenderCommand> shadowCommands(Resources r, Matrix4f boxModel) {
        return List.of(new RenderCommand(r.boxMesh(), r.shadowMaterial(), boxModel));
    }

    private static List<RenderCommand> forwardCommands(Resources r, Matrix4f boxModel) {
        return List.of(
                new RenderCommand(r.boxMesh(), r.boxMaterial(), boxModel),
                new RenderCommand(r.floorMesh(), r.floorMaterial(), FLOOR_MODEL));
    }

    // ---- Criacao dos recursos (retorna um valor, nao grava em campos) ----

    private static Resources createResources(Renderer renderer, int width, int height) {
        Texture shadowMap = renderer.createDepthTexture(2048, 2048);
        DirectionalLight light = new DirectionalLight(
                new Vector3f(-0.45f, 1.0f, 0.35f), new Vector3f(0.0f, 0.0f, -2.0f), 8.0f, 7.0f, 1.0f, 18.0f);

        Shader forwardShader = renderer.createShader(VERTEX_SHADER, FRAGMENT_SHADER);
        Shader shadowShader = renderer.createShader(SHADOW_VERTEX_SHADER, SHADOW_FRAGMENT_SHADER);

        Function<LitParams, Material> lit = p -> renderer.createMaterial(forwardShader)
                .setVector3f("objectColor", p.color())
                .setVector3f("lightDirection", light.direction())
                .setFloat("ambientStrength", p.ambient())
                .setFloat("diffuseStrength", p.diffuse())
                .setFloat("shadowBias", p.shadowBias())
                .setMatrix4f("lightSpaceMatrix", light.lightSpaceMatrix())
                .setTexture("shadowMap", shadowMap, 1);

        return new Resources(
                new RenderView(width, height, Projections.perspective(width, height), new Matrix4f()),
                light.shadowView(shadowMap),
                new RenderPassDescriptor("shadow")
                        .depthAttachment(shadowMap)
                        .clearColorBuffer(false)
                        .clearDepthBuffer(true)
                        .depthTest(true)
                        .cullFace(false),
                new RenderPassDescriptor("forward")
                        .clearColor(0.1f, 0.2f, 0.3f, 1f)
                        .depthTest(true)
                        .cullFace(true),
                renderer.createMesh(ShapeData.cube()),
                renderer.createMesh(ShapeData.floor()),
                lit.apply(new LitParams(new Vector3f(1.0f, 0.5f, 0.2f), 0.2f, 0.8f, 0.004f)),
                lit.apply(new LitParams(new Vector3f(0.72f, 0.78f, 0.68f), 0.25f, 0.75f, 0.002f)),
                renderer.createMaterial(shadowShader));
    }

    // ---- Shaders ----

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
