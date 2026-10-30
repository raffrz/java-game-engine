package com.farias.jengine.examples.city;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.jengine.assets.AssetsModule;
import com.farias.jengine.core.Engine;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Timer;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Key;
import com.farias.jengine.platform.MouseButton;
import com.farias.jengine.platform.Window;
import com.farias.jengine.platform.glfw.GlfwPlatformModule;
import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.MeshData;
import com.farias.jengine.renderer.RenderCommand;
import com.farias.jengine.renderer.RenderPass;
import com.farias.jengine.renderer.RenderPassDescriptor;
import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.RendererModule;
import com.farias.jengine.renderer.Texture;
import com.farias.jengine.toolkit2d.Toolkit2D;
import com.farias.jengine.toolkit2d.Toolkit2DModule;
import com.farias.jengine.toolkit2d.Ui;
import com.farias.jengine.toolkit3d.DirectionalLight;
import com.farias.jengine.toolkit3d.LitMaterial;
import com.farias.jengine.toolkit3d.LitMaterials;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.Shadows;
import com.farias.jengine.toolkit3d.Toolkit3DModule;
import com.farias.procgeo.core.geometry.GeometrySet;
import com.farias.procgeo.graph.GenerationOptions;
import com.farias.procgeo.graph.GraphExecutor;
import com.farias.procgeo.graph.GraphInputs;
import com.farias.procgeo.graph.GraphLibrary;
import com.farias.procgeo.nodes.StandardNodes;

/**
 * Cidade procedural: ruas e predios variados (torres, galpoes, predios cilindricos,
 * casas com telhado de telhas, parques com arvores), todos com muito detalhe (janelas com requadro, sacadas, meio-fio, sinalizacao) gerados em tempo de execucao por grafos da
 * biblioteca procedural-geometry (ver {@link CityGraphs}). O demo so executa o grafo
 * "city" com uma seed, converte a malha resultante em meshes do renderer (um por
 * material, ver {@link CityMeshes}) e desenha com sombras.
 *
 * <p>
 * Camera de perspectiva em orbita. Controles: setas/WASD movem no chao, roda = zoom, arrastar (botao esquerdo ou
 * do meio) orbita, Q/E giram, R gera outra cidade (nova seed), Esc sai.
 */
public final class ProceduralCityDemo {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 800;
    private static final int BLOCKS = 5;
    private static final long FIRST_SEED = 2026L;

    private static final float PAN_SPEED = 0.8f;
    private static final float ROTATE_SENSITIVITY = 0.3f;
    private static final float KEY_ROTATE_SPEED = 60f;

    private static final Vector3f LIGHT_DIRECTION = new Vector3f(0.45f, 1f, 0.6f);
    private static final float LIGHT_RADIUS = 170f;
    private static final int SHADOW_MAP_SIZE = 4096;

    private final Window window;
    private final Input input;
    private final Renderer renderer;
    private final Ui ui;
    private final LitMaterials litMaterials;
    private final ShadowMap shadow;
    private final OrbitCamera camera = new OrbitCamera(35f, 38f, 330f);
    private final Matrix4f identity = new Matrix4f();
    private final RenderPassDescriptor scenePass = new RenderPassDescriptor("procedural-city")
            .clearColor(0.62f, 0.76f, 0.92f, 1f)
            .depthTest(true)
            .cullFace(false)
            .blend(false);

    private final GraphLibrary graphs = CityGraphs.library();
    private final GraphExecutor executor = new GraphExecutor(StandardNodes.newRegistry(), graphs);
    private final Map<String, Texture> textures = new LinkedHashMap<>();
    private final Map<String, Material> materials = new LinkedHashMap<>();
    private final List<RenderCommand> commands = new ArrayList<>();
    private final List<Mesh> meshes = new ArrayList<>();

    private long seed = FIRST_SEED;
    private String stats = "";
    private double lastMouseX;
    private double lastMouseY;

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new GlfwPlatformModule(WINDOW_WIDTH, WINDOW_HEIGHT, "Procedural City"))
                .registerModule(new RendererModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Toolkit3DModule())
                .registerModule(new Toolkit2DModule());

        EngineContext context = engine.start();
        try {
            new ProceduralCityDemo(context).run();
        } finally {
            engine.terminate();
        }
    }

    private ProceduralCityDemo(EngineContext context) {
        window = context.require(Window.class);
        input = context.require(Input.class);
        renderer = context.require(Renderer.class);
        ui = context.require(Toolkit2D.class).ui(window.getWidth(), window.getHeight());
        litMaterials = context.require(LitMaterials.class);

        DirectionalLight light = DirectionalLight.covering(LIGHT_DIRECTION, new Vector3f(0f, 0f, 0f), LIGHT_RADIUS);
        shadow = context.require(Shadows.class).create(light, SHADOW_MAP_SIZE);
        camera.target().set(0f, 8f, 0f);
        generate();
    }

    private void run() {
        Timer timer = new Timer();
        while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
            timer.update();
            window.pollEvents();
            input.update();

            if (input.isKeyPressed(Key.R)) {
                seed++;
                generate();
            }
            moveCamera(timer.deltaTime());
            render();
        }
    }

    // ---- geracao: o grafo roda aqui, o resto do demo so desenha o resultado ----

    private void generate() {
        long start = System.nanoTime();
        GeometrySet city = executor.execute(
                graphs.load("city"),
                GraphInputs.builder().set("blocks", BLOCKS).build(),
                GenerationOptions.withSeed(seed)).geometry("geometry");
        long generated = System.nanoTime();

        Map<String, MeshData> byMaterial = CityMeshes.byMaterial(city, CityPalettes.sizes());
        meshes.forEach(renderer::destroy);
        meshes.clear();
        commands.clear();

        int triangles = 0;
        for (Map.Entry<String, MeshData> entry : byMaterial.entrySet()) {
            Mesh mesh = renderer.createMesh(entry.getValue());
            meshes.add(mesh);
            commands.add(new RenderCommand(mesh, material(entry.getKey()), identity));
            triangles += entry.getValue().indices().length / 3;
        }
        long done = System.nanoTime();

        stats = "seed " + seed + " | " + triangles + " triangulos | grafo " + (generated - start) / 1_000_000 + " ms";
        System.out.println("[city] " + stats + " | meshes " + (done - generated) / 1_000_000 + " ms");
    }

    private Material material(String name) {
        return materials.computeIfAbsent(name, n -> {
            Texture texture = textures.computeIfAbsent(n, k -> CityPalettes.texture(renderer, k));
            LitMaterial material = litMaterials.create(texture)
                    .light(LIGHT_DIRECTION, 0.38f, 0.72f)
                    .receiving(shadow);
            switch (n) {
                case "window_lit" -> material.emission(texture, 0.55f);
                case "shopfront" -> material.emission(texture, 0.4f);
                case "lamp" -> material.emission(texture, 0.95f);
                case "headlight", "light_red", "light_amber", "light_green" -> material.emission(texture, 0.9f);
                case "taillight" -> material.emission(texture, 0.7f);
                default -> {
                }
            }
            return material;
        });
    }

    // ---- camera ----

    private void moveCamera(float deltaTime) {
        float forward = axis(Key.UP, Key.W, Key.DOWN, Key.S);
        float right = axis(Key.RIGHT, Key.D, Key.LEFT, Key.A);
        float speed = PAN_SPEED * camera.distance() * deltaTime;
        camera.pan(forward * speed, right * speed);

        camera.zoom((float) input.getScrollY());
        camera.rotate(axis(Key.E, Key.E, Key.Q, Key.Q) * KEY_ROTATE_SPEED * deltaTime, 0f);

        // arrastar com o botao esquerdo ou do meio orbita (horizontal = yaw, vertical = pitch)
        double mouseX = input.getMouseX();
        double mouseY = input.getMouseY();
        boolean pressed = input.isMouseButtonPressed(MouseButton.LEFT) || input.isMouseButtonPressed(MouseButton.MIDDLE);
        boolean dragging = input.isMouseButtonDown(MouseButton.LEFT) || input.isMouseButtonDown(MouseButton.MIDDLE);
        if (dragging && !pressed) {
            camera.rotate((float) (mouseX - lastMouseX) * -ROTATE_SENSITIVITY, (float) (mouseY - lastMouseY) * ROTATE_SENSITIVITY);
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private float axis(Key positive, Key positiveAlt, Key negative, Key negativeAlt) {
        float p = input.isKeyDown(positive) || input.isKeyDown(positiveAlt) ? 1f : 0f;
        float n = input.isKeyDown(negative) || input.isKeyDown(negativeAlt) ? 1f : 0f;
        return p - n;
    }

    // ---- desenho ----

    private void render() {
        renderer.beginFrame();

        RenderPass shadowPass = renderer.createPass(shadow.descriptor(), shadow.view());
        for (RenderCommand command : commands) {
            shadowPass.submit(shadow.caster(command.mesh(), identity));
        }

        RenderPass pass = renderer.createPass(scenePass, camera.view(window));
        commands.forEach(pass::submit);

        int centerX = window.getWidth() / 2;
        ui.begin();
        ui.textCentered(ui.defaultFont(), "CIDADE PROCEDURAL - " + stats.toUpperCase(), centerX, 18, 12);
        ui.textCentered(ui.defaultFont(), "SETAS/WASD = MOVER | RODA = ZOOM | ARRASTAR = ORBITAR | Q/E = GIRAR | R = NOVA CIDADE",
                centerX, 38, 10);
        renderer.endFrame();
    }
}
