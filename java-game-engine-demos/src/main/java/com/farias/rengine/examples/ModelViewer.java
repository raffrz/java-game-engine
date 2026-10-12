package com.farias.rengine.examples;

import java.nio.file.Path;

import com.farias.rengine.assets.AssetsModule;
import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine.modules.InputModule;
import com.farias.rengine.engine.modules.RenderModule;
import com.farias.rengine.engine.modules.WindowModule;
import com.farias.rengine.engine3d.Camera;
import com.farias.rengine.engine3d.Engine3DModule;
import com.farias.rengine.engine3d.ModelLoader;
import com.farias.rengine.engine3d.Projections;
import com.farias.rengine.engine3d.Shaders;
import com.farias.rengine.engine3d.TexturedModel;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.toolkit2d.Toolkit2DModule;
import com.farias.rengine.toolkit2d.ui.Ui;

public class ModelViewer {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;
    private static final int FONT_SIZE = 20;
    private static final float CAMERA_SPEED = 5.0f;
    private static final float ROTATION_SPEED = 16f;

    private final Camera camera = new Camera();
    private final TexturedModel earth;
    private final TexturedModel clouds;

    private final Window window;
    private final InputSystem input;
    private final Renderer renderer;
    private final Ui ui;
    private final RenderPassDescriptor scene3DDescriptor;

    // ponto observado pela câmera
    private float targetX = 0;
    private float targetY = 0;
    private float targetZ = 0;

    // distância da câmera (terra tem raio ~1, entao perto o bastante pra preencher a tela)
    private float cameraDistance = 3f;

    public ModelViewer(EngineContext context) {
        window = context.require(Window.class);
        input = context.require(InputSystem.class);
        renderer = context.require(Renderer.class);
        ui = context.require(Ui.class);

        Shader litShader = renderer.createShader(Shaders.LIT_VERTEX, Shaders.LIT_FRAGMENT);
        ModelLoader models = context.require(ModelLoader.class);
        earth = models.loadWavefrontOBJ("earth", litShader);
        clouds = models.loadWavefrontOBJ("clouds", litShader);

        scene3DDescriptor = new RenderPassDescriptor("model-viewer-3d")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(true)
                .cullFace(true)
                .blend(true);

        updateIsometricCamera();
    }

    private void run() {
        Timer timer = new Timer();
        while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
            timer.update();
            input.update();
            update(timer.deltaTime());

            renderer.beginFrame();
            render();
            renderer.endFrame();
        }
    }

    private void updateIsometricCamera() {
        camera.lookAt(
                targetX + cameraDistance,
                targetY + cameraDistance,
                targetZ + cameraDistance,

                targetX,
                targetY,
                targetZ,

                0, 1, 0);
    }

    private void update(float deltaTime) {

        // rotação da terra
        earth.getRotation().y -= 0.7f * deltaTime * ROTATION_SPEED;

        if (earth.getRotation().y < -360f) {
            earth.getRotation().y += 360f;
        }

        // rotação das nuvens
        clouds.getRotation().y -= 1.0f * deltaTime * ROTATION_SPEED;

        if (clouds.getRotation().y < -360f) {
            clouds.getRotation().y += 360f;
        }

        // tamanho da atmosfera
        if (input.isKeyDown(Key.A)) {

            if (input.isKeyDown(Key.LEFT_SHIFT)) {
                clouds.getScale().sub(0.1f, 0.1f, 0.1f);
            } else {
                clouds.getScale().add(0.1f, 0.1f, 0.1f);
            }
        }

        float move = CAMERA_SPEED * deltaTime;

        /*
         * Movimento isométrico
         *
         * ↑ = nordeste
         * ↓ = sudoeste
         * ← = noroeste
         * → = sudeste
         */

        if (input.isKeyDown(Key.UP)) {
            targetX += move;
            targetZ += move;
        }

        if (input.isKeyDown(Key.DOWN)) {
            targetX -= move;
            targetZ -= move;
        }

        if (input.isKeyDown(Key.LEFT)) {
            targetX -= move;
            targetZ += move;
        }

        if (input.isKeyDown(Key.RIGHT)) {
            targetX += move;
            targetZ -= move;
        }

        // sobe/desce a câmera
        if (input.isKeyDown(Key.UP)) {
            targetY += move;
        }

        if (input.isKeyDown(Key.DOWN)) {
            targetY -= move;
        }

        // zoom (aproxima/afasta a câmera do alvo)
        if (input.isKeyDown(Key.Z)) {

            if (input.isKeyDown(Key.LEFT_SHIFT)) {
                cameraDistance += 2f * deltaTime * CAMERA_SPEED;
            } else {
                cameraDistance -= 2f * deltaTime * CAMERA_SPEED;
            }

            if (cameraDistance < 2f) {
                cameraDistance = 2f;
            }
        }

        updateIsometricCamera();
    }

    private void render() {
        // Renderização 3D em perspectiva (era ortográfica/isométrica)
        RenderView view3D = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections.perspective(WINDOW_WIDTH, WINDOW_HEIGHT), camera.getViewMatrix());
        RenderPassNode scenePass = renderer.createPass(scene3DDescriptor, view3D);

        scenePass.submit(new RenderCommand(earth.getMesh(), earth.getMaterial(), earth.getModelMatrix()));
        scenePass.submit(new RenderCommand(clouds.getMesh(), clouds.getMaterial(), clouds.getModelMatrix()));

        // UI (depois da cena: os passes executam na ordem de criacao)
        ui.begin();
        ui.textCentered(ui.defaultFont(), "ISOMETRIC VIEW", WINDOW_WIDTH / 2, 20, FONT_SIZE);
        ui.textCentered(ui.defaultFont(), "ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT", WINDOW_WIDTH / 2, 50, 14);
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new WindowModule(WINDOW_WIDTH, WINDOW_HEIGHT, "Model Viewer - Isometric"))
                .registerModule(new InputModule())
                .registerModule(new RenderModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Engine3DModule())
                .registerModule(new Toolkit2DModule(WINDOW_WIDTH, WINDOW_HEIGHT));

        EngineContext context = engine.start();
        try {
            new ModelViewer(context).run();
        } finally {
            engine.terminate();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
