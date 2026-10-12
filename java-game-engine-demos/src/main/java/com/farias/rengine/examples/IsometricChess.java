package com.farias.rengine.examples;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.assets.AssetsModule;
import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine3d.Camera;
import com.farias.rengine.engine3d.Engine3DModule;
import com.farias.rengine.engine3d.Materials;
import com.farias.rengine.engine3d.Meshes;
import com.farias.rengine.engine3d.ModelLoader;
import com.farias.rengine.engine3d.Picking;
import com.farias.rengine.engine3d.Projections;
import com.farias.rengine.engine3d.Ray;
import com.farias.rengine.engine3d.Shaders;
import com.farias.rengine.engine3d.TexturedModel;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.MouseButton;
import com.farias.rengine.io.Window;
import com.farias.rengine.platform.glfw.GlfwModule;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderModule;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.toolkit2d.Toolkit2DModule;
import com.farias.rengine.toolkit2d.ui.Ui;

/**
 * Xadrez isométrico: tabuleiro 8x8 com peças 3D e seleção por clique
 * (raycasting).
 *
 * <h2>Controles</h2>
 * Setas esquerda/direita movem a câmera na diagonal do tabuleiro, cima/baixo
 * sobem e descem, Z aproxima e Shift+Z afasta. O clique esquerdo seleciona uma
 * peça, que sobe e fica girando; clicar de novo nela, ou fora de qualquer peça,
 * desfaz a seleção. ESC fecha.
 *
 * <h2>Convenções do tabuleiro</h2>
 * A casa (x, z) tem o centro em (x, 0, z) e lado 1, então cobre
 * [x - 0,5; x + 0,5] em X (e igual em Z). O quad do piso nasce vertical (plano
 * XY) e é deitado com rotação de 90° em X.
 *
 * <h2>Picking</h2>
 * Clique → {@link Picking#toNdc} → {@link Picking#ray} → {@code ChessBoard.pick}.
 * O raio sai da mesma {@code view3D()} que desenha a cena, e cada peça é
 * testada com a matriz de modelo do próprio {@link TexturedModel}, a mesma
 * usada para desenhá-la; por isso a caixa acompanha a peça elevada e girando.
 *
 * <h2>Renderização</h2>
 * O passe 3D não usa culling: os .obj das peças têm winding e normais
 * invertidos (ver CHESS_MODELS.md), por isso o carregamento usa
 * {@code flipNormals} e, sem blend, o depth test sozinho escolhe a face
 * visível. A UI é um passe à parte, criado depois da cena porque os passes
 * executam na ordem de criação.
 *
 * <h2>Peças</h2>
 * Todas começam giradas 90° em Y, exceto o cavalo: -180° nas brancas e 0° nas
 * pretas.
 */
public class IsometricChess {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;
    private static final int TITLE_FONT_SIZE = 20;
    private static final int HINT_FONT_SIZE = 14;

    private static final int BOARD_SIZE = 8;
    private static final float TILE_ROTATION_X = 90f;
    private static final float PIECE_SCALE = 1.2f;
    private static final float PIECE_ROTATION_Y = 90f;
    private static final int KNIGHT = 3;

    private static final float CAMERA_SPEED = 5.0f;
    private static final float CAMERA_DISTANCE = 10f;

    private static final float LIFT_HEIGHT = 0.5f;
    private static final float LIFT_SPEED = 3f;
    private static final float SPIN_SPEED = 90f;

    private final Camera camera = new Camera();
    private final List<TexturedModel> tiles = new ArrayList<>();

    private final Window window;
    private final InputSystem input;
    private final Renderer renderer;
    private final Assets assets;
    private final Ui ui;
    private final Meshes meshes;
    private final Materials materials;
    private final ModelLoader modelLoader;

    private final Shader litShader;
    private final RenderPassDescriptor scene3DDescriptor;
    private final ChessBoard chessBoard;

    private ChessPiece selected;

    private float targetX = 0;
    private float targetY = 0;
    private float targetZ = 0;
    private float fov = 5f;

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
        renderer = context.require(Renderer.class);
        assets = context.require(Assets.class);
        ui = context.require(Ui.class);
        meshes = context.require(Meshes.class);
        materials = context.require(Materials.class);
        modelLoader = context.require(ModelLoader.class);

        litShader = renderer.createShader(Shaders.LIT_VERTEX, Shaders.LIT_FRAGMENT);
        scene3DDescriptor = new RenderPassDescriptor("isometric-chess-3d")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(true)
                .cullFace(false)
                .blend(false);

        createTiles();
        chessBoard = new ChessBoard();
        chessBoard.setupDefaultBoard();
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

    private void update(float deltaTime) {
        updateCamera(deltaTime);
        handleClick();
        chessBoard.update(deltaTime);
    }

    private void updateCamera(float deltaTime) {
        float move = CAMERA_SPEED * deltaTime;

        float pan = axis(Key.RIGHT, Key.LEFT) * move / 2f;
        targetX += pan;
        targetZ -= pan;
        targetY += axis(Key.UP, Key.DOWN) * move;

        if (input.isKeyDown(Key.Z)) {
            fov += (input.isKeyDown(Key.LEFT_SHIFT) ? 1f : -1f) * 2f * deltaTime * CAMERA_SPEED;
        }

        camera.lookAt(
                targetX + CAMERA_DISTANCE,
                targetY + CAMERA_DISTANCE,
                targetZ + CAMERA_DISTANCE,
                targetX,
                targetY,
                targetZ,
                0, 1, 0);
    }

    private float axis(Key positive, Key negative) {
        return (input.isKeyDown(positive) ? 1f : 0f) - (input.isKeyDown(negative) ? 1f : 0f);
    }

    private void handleClick() {
        if (!input.isMouseButtonPressed(MouseButton.LEFT)) {
            return;
        }

        Vector2f ndc = Picking.toNdc(input.getMouseX(), input.getMouseY(), window.getWidth(), window.getHeight());
        select(chessBoard.pick(Picking.ray(view3D(), ndc)));
    }

    private void select(ChessPiece piece) {
        if (selected != null) {
            selected.setSelected(false);
        }
        selected = piece == selected ? null : piece;
        if (selected != null) {
            selected.setSelected(true);
        }
    }

    private void render() {
        RenderPassNode scenePass = renderer.createPass(scene3DDescriptor, view3D());
        tiles.forEach(tile -> submit(scenePass, tile));
        chessBoard.draw(scenePass);

        ui.begin();
        ui.textCentered(ui.defaultFont(), "ISOMETRIC CHESS - LWJGL RENDER ENGINE", WINDOW_WIDTH / 2, 20,
                TITLE_FONT_SIZE);
        ui.textCentered(ui.defaultFont(), "ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT | CLICK = SELECT",
                WINDOW_WIDTH / 2, 50, HINT_FONT_SIZE);
    }

    private RenderView view3D() {
        return new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections.orthographic3D(fov, (float) WINDOW_WIDTH / WINDOW_HEIGHT), camera.getViewMatrix());
    }

    private static void submit(RenderPassNode pass, TexturedModel model) {
        pass.submit(new RenderCommand(model.getMesh(), model.getMaterial(), model.getModelMatrix()));
    }

    private void createTiles() {
        Mesh plane = meshes.plane(1f, 1f);
        Material white = materials.lit(litShader, assets.texture("textures/yellow_sprite.png"));
        Material black = materials.lit(litShader, assets.texture("textures/brown_sprite.png"));

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int z = 0; z < BOARD_SIZE; z++) {
                TexturedModel tile = new TexturedModel(plane, (x + z) % 2 == 0 ? white : black);
                tile.getPosition().set(x, 0f, z);
                tile.getRotation().set(TILE_ROTATION_X, 0f, 0f);
                tiles.add(tile);
            }
        }
    }

    /** Uma casa do tabuleiro, com x e z de 0 a 7. */
    private record Square(int x, int z) {
    }

    class ChessPiece {
        private final TexturedModel model;
        private final float baseRotationY;

        private boolean selected;
        private float lift;
        private float spin;

        ChessPiece(TexturedModel model, int x, int z, float rotationY) {
            this.model = model;
            this.baseRotationY = rotationY;

            model.getPosition().set(x, 0f, z);
            model.getScale().set(PIECE_SCALE);
            model.getRotation().set(0f, rotationY, 0f);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        void update(float deltaTime) {
            float step = LIFT_SPEED * deltaTime;
            float targetLift = selected ? LIFT_HEIGHT : 0f;
            lift += Math.max(-step, Math.min(step, targetLift - lift));
            spin = selected ? (spin + SPIN_SPEED * deltaTime) % 360f : 0f;

            model.getPosition().y = lift;
            model.getRotation().y = baseRotationY + spin;
        }

        void draw(RenderPassNode pass) {
            submit(pass, model);
        }

        /** Distância ao longo do raio até esta peça, ou NaN se não acertou. */
        float hit(Ray ray) {
            return Picking.rayBounds(ray, model.getMesh().bounds(), model.getModelMatrix());
        }
    }

    class ChessBoard {
        private final String[] PIECE_TYPES = { "", "pawn", "rook", "knight", "bishop", "queen", "king" };
        private final List<ChessPiece> pieces = new ArrayList<>();

        void setupDefaultBoard() {
            int[][] initialSetup = {
                    { 2, 3, 4, 5, 6, 4, 3, 2 }, // Peças pretas
                    { 1, 1, 1, 1, 1, 1, 1, 1 }, // Peões pretos
                    { 0, 0, 0, 0, 0, 0, 0, 0 }, // Vazio
                    { 0, 0, 0, 0, 0, 0, 0, 0 }, // Vazio
                    { 0, 0, 0, 0, 0, 0, 0, 0 }, // Vazio
                    { 0, 0, 0, 0, 0, 0, 0, 0 }, // Vazio
                    { 1, 1, 1, 1, 1, 1, 1, 1 }, // Peões brancos
                    { 2, 3, 4, 5, 6, 4, 3, 2 } // Peças brancas
            };

            for (int z = 0; z < BOARD_SIZE; z++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    if (initialSetup[z][x] != 0) {
                        place(initialSetup[z][x], x, z);
                    }
                }
            }
        }

        void update(float deltaTime) {
            pieces.forEach(piece -> piece.update(deltaTime));
        }

        void draw(RenderPassNode pass) {
            pieces.forEach(piece -> piece.draw(pass));
        }

        /** A peça mais próxima atingida pelo raio, ou null se nenhuma. */
        ChessPiece pick(Ray ray) {
            ChessPiece closest = null;
            float closestT = Float.POSITIVE_INFINITY;

            for (ChessPiece piece : pieces) {
                float t = piece.hit(ray);
                if (!Float.isNaN(t) && t < closestT) {
                    closestT = t;
                    closest = piece;
                }
            }
            return closest;
        }

        /** A casa sob o raio, ou vazio se ele não toca o tabuleiro. */
        Optional<Square> squareAt(Ray ray) {
            float t = Picking.rayPlaneY(ray, 0f);
            if (Float.isNaN(t)) {
                return Optional.empty();
            }

            Vector3f hit = ray.pointAt(t);
            int x = (int) Math.floor(hit.x + 0.5f);
            int z = (int) Math.floor(hit.z + 0.5f);

            if (x < 0 || x >= BOARD_SIZE || z < 0 || z >= BOARD_SIZE) {
                return Optional.empty();
            }
            return Optional.of(new Square(x, z));
        }

        private void place(int pieceId, int x, int z) {
            boolean white = z >= BOARD_SIZE / 2;
            String name = PIECE_TYPES[pieceId] + (white ? "_white" : "_black");
            TexturedModel model = modelLoader.loadWavefrontOBJ(name, litShader, true);

            float rotation = PIECE_ROTATION_Y;
            if (pieceId == KNIGHT) {
                rotation = white ? -180f : 0f;
            }

            pieces.add(new ChessPiece(model, x, z, rotation));
        }
    }
}
