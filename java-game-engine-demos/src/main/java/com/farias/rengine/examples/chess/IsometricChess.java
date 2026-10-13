package com.farias.rengine.examples.chess;

import java.nio.file.Path;

import org.joml.Vector3f;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.assets.AssetsModule;
import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine3d.Engine3DModule;
import com.farias.rengine.engine3d.IsometricCamera;
import com.farias.rengine.engine3d.Materials;
import com.farias.rengine.engine3d.Meshes;
import com.farias.rengine.engine3d.ModelLoader;
import com.farias.rengine.engine3d.ShadowMap;
import com.farias.rengine.engine3d.Shadows;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.MouseButton;
import com.farias.rengine.io.Window;
import com.farias.rengine.platform.glfw.GlfwModule;
import com.farias.rengine.renderer.DirectionalLight;
import com.farias.rengine.renderer.RenderModule;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.Toolkit2DModule;
import com.farias.rengine.toolkit2d.ui.Ui;

/**
 * Xadrez isométrico: liga o {@link ChessGame} (regras e estado, sem engine) à
 * apresentação 3D e roda o loop principal.
 *
 * <h2>Quem faz o quê</h2>
 * <ul>
 * <li>{@link ChessGame}, {@link ChessBoard}, {@link ChessPiece}: estado do
 * jogo.</li>
 * <li>{@link ChessCameraControls}: teclado → câmera.</li>
 * <li>{@link ChessScene} e {@link PieceView}: modelos, animação e picking.</li>
 * <li>{@link ChessRenderer}: passes de sombra, cena e UI.</li>
 * </ul>
 *
 * <h2>Controles</h2>
 * Setas esquerda/direita movem a câmera na diagonal do tabuleiro, cima/baixo
 * sobem e descem, Z aproxima e Shift+Z afasta. O clique esquerdo seleciona uma
 * peça, que sobe e fica girando; clicar de novo nela, ou fora de qualquer peça,
 * desfaz a seleção. ESC fecha.
 *
 * <h2>Picking</h2>
 * {@link IsometricCamera#rayAt} gera o raio da mesma câmera que desenha a
 * cena, e cada peça é testada com a matriz de modelo do próprio modelo, a mesma
 * usada para desenhá-la; por isso a caixa acompanha a peça elevada e girando.
 *
 * <h2>Sombras</h2>
 * A luz que gera o mapa de sombras é a mesma do sombreamento difuso; casas e
 * peças recebem sombra, só as peças projetam.
 */
public class IsometricChess {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;

    private static final float CAMERA_OFFSET = 10f;
    private static final float CAMERA_SIZE = 5f;

    private static final int SHADOW_MAP_SIZE = 2048;
    private static final Vector3f LIGHT_DIRECTION = new Vector3f(0.25f, 1f, 0.75f);
    private static final float LIGHT_RADIUS = 7f;

    private final Window window;
    private final InputSystem input;

    private final ChessGame game = new ChessGame();
    private final IsometricCamera camera = new IsometricCamera(CAMERA_OFFSET, CAMERA_SIZE);
    private final ChessCameraControls cameraControls;
    private final ChessScene scene;
    private final ChessRenderer renderer;

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new GlfwModule(WINDOW_WIDTH, WINDOW_HEIGHT, "Isometric Chess"))
                .registerModule(new RenderModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Engine3DModule())
                .registerModule(new Toolkit2DModule(WINDOW_WIDTH, WINDOW_HEIGHT));

        EngineContext context = engine.start();
        try {
            new IsometricChess(context).run();
        } finally {
            engine.terminate();
        }
    }

    public IsometricChess(EngineContext context) {
        window = context.require(Window.class);
        input = context.require(InputSystem.class);

        ShadowMap shadow = context.require(Shadows.class).create(boardLight(), SHADOW_MAP_SIZE);

        cameraControls = new ChessCameraControls(input, camera);
        scene = new ChessScene(game,
                context.require(Assets.class),
                context.require(Meshes.class),
                context.require(Materials.class),
                context.require(ModelLoader.class),
                shadow, camera);
        renderer = new ChessRenderer(context.require(Renderer.class), context.require(Ui.class),
                window, WINDOW_WIDTH, camera, shadow);
    }

    private void run() {
        Timer timer = new Timer();
        while (!shouldQuit()) {
            timer.update();
            window.pollEvents();
            input.update();
            float deltaTime = timer.deltaTime();

            cameraControls.update(deltaTime);
            handleSelection();
            scene.update(game, deltaTime);
            renderer.render(scene);
        }
    }

    private boolean shouldQuit() {
        return window.shouldClose() || input.isKeyPressed(Key.ESCAPE);
    }

    private void handleSelection() {
        if (input.isMouseButtonPressed(MouseButton.LEFT)) {
            game.select(scene.pieceAt(camera.rayAt(input.getMouseX(), input.getMouseY(), window)));
        }
    }

    private static DirectionalLight boardLight() {
        float boardCenter = (ChessBoard.SIZE - 1) / 2f;
        return DirectionalLight.covering(LIGHT_DIRECTION, new Vector3f(boardCenter, 0f, boardCenter),
                LIGHT_RADIUS);
    }
}
