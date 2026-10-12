package com.farias.rengine.examples;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.assets.AssetsModule;
import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine.modules.InputModule;
import com.farias.rengine.engine.modules.RenderModule;
import com.farias.rengine.engine.modules.WindowModule;
import com.farias.rengine.engine3d.Camera;
import com.farias.rengine.engine3d.Engine3DModule;
import com.farias.rengine.engine3d.Materials;
import com.farias.rengine.engine3d.Meshes;
import com.farias.rengine.engine3d.ModelLoader;
import com.farias.rengine.engine3d.Projections;
import com.farias.rengine.engine3d.Shaders;
import com.farias.rengine.engine3d.TexturedModel;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.MouseButton;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.toolkit2d.Toolkit2DModule;
import com.farias.rengine.toolkit2d.ui.Ui;

public class IsometricChess {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;
    private static final int FONT_SIZE = 20;
    private static final float CAMERA_SPEED = 5.0f;

    private final Camera camera = new Camera();

    private final Window window;
    private final InputSystem input;
    private final Renderer renderer;
    private final Assets assets;
    private final Ui ui;

    private final Shader litShader;
    private final Meshes meshes;
    private final Materials materials;
    private final ModelLoader modelLoader;
    private final TexturedModel whiteTile;
    private final TexturedModel blackTile;
    private final ChessBoard chessBoard;

    private final RenderPassDescriptor scene3DDescriptor;

    // ponto observado pela câmera
    private float targetX = 0;
    private float targetY = 0;
    private float targetZ = 0;
    private float fov = 5f;

    // distância da câmera para visão isométrica
    private float cameraDistance = 10f;

    public IsometricChess(EngineContext context) {
        window = context.require(Window.class);
        input = context.require(InputSystem.class);
        renderer = context.require(Renderer.class);
        assets = context.require(Assets.class);
        ui = context.require(Ui.class);

        litShader = renderer.createShader(Shaders.LIT_VERTEX, Shaders.LIT_FRAGMENT);
        meshes = context.require(Meshes.class);
        materials = context.require(Materials.class);
        modelLoader = context.require(ModelLoader.class);

        // Inicializa os pisos do tabuleiro
        whiteTile = createPlane("yellow_sprite", 1, 1);
        blackTile = createPlane("brown_sprite", 1, 1);

        // Inicializa o tabuleiro e popula as peças internamente
        chessBoard = new ChessBoard();
        chessBoard.setupDefaultBoard();

        scene3DDescriptor = new RenderPassDescriptor("isometric-chess-3d")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(true)
                // os modelos das pecas de xadrez tem winding oposto ao do
                // earth/clouds do ModelViewer (exportados de outra fonte), e
                // culling descarta a face de fora em vez da de dentro. Sem
                // blend, o depth test sozinho ja resolve corretamente qual
                // lado mostrar (o mais perto da camera), entao culling aqui
                // so economizaria fill-rate - nao e necessario pra corretude.
                .cullFace(false)
                .blend(false);

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

    private TexturedModel createPlane(String textureName, float width, float height) {
        Mesh mesh = meshes.plane(width, height);
        Material material = materials.lit(litShader, assets.texture("textures/" + textureName + ".png"));
        return new TexturedModel(mesh, material);
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
        // BOARD CONTROLS
        if (input.isMouseButtonDown(MouseButton.LEFT)) {
            double mouseX = input.getMouseX();
            double mouseY = input.getMouseY();

            int boardX = (int) (mouseX / (WINDOW_WIDTH / 8));
            int boardZ = (int) (mouseY / (WINDOW_HEIGHT / 8));

            if (boardX >= 0 && boardX < 8 && boardZ >= 0 && boardZ < 8) {
                System.out.println("Clicked on board position: " + boardX + ", " + boardZ);
            }
        }

        // CAMERA CONTROLS
        float move = CAMERA_SPEED * deltaTime;
        if (input.isKeyDown(Key.LEFT)) {
            targetX -= move / 2f;
            targetZ += move / 2f;
        }
        if (input.isKeyDown(Key.RIGHT)) {
            targetX += move / 2f;
            targetZ -= move / 2f;
        }
        if (input.isKeyDown(Key.UP)) {
            targetY += move;
        }
        if (input.isKeyDown(Key.DOWN)) {
            targetY -= move;
        }
        // zoom
        if (input.isKeyDown(Key.Z)) {
            if (input.isKeyDown(Key.LEFT_SHIFT)) {
                fov += 2f * deltaTime * CAMERA_SPEED;
            } else {
                fov -= 2f * deltaTime * CAMERA_SPEED;
            }
        }
        updateIsometricCamera();
    }

    private void render() {
        RenderView view3D = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections.orthographic3D(fov, (float) WINDOW_WIDTH / WINDOW_HEIGHT), camera.getViewMatrix());
        RenderPassNode scenePass = renderer.createPass(scene3DDescriptor, view3D);

        drawBoard(scenePass);
        chessBoard.drawPieces(scenePass);

        // UI (depois da cena: os passes executam na ordem de criacao)
        ui.begin();
        ui.textCentered(ui.defaultFont(), "ISOMETRIC CHESS - LWJGL RENDER ENGINE", WINDOW_WIDTH / 2, 20, FONT_SIZE);
        ui.textCentered(ui.defaultFont(), "ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT", WINDOW_WIDTH / 2, 50, 14);
    }

    private void drawBoard(RenderPassNode pass) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                boolean isWhite = (i + j) % 2 == 0;
                TexturedModel tileModel = isWhite ? whiteTile : blackTile;
                submitModel(pass, tileModel, i, 0f, j, 1f, 90f, 0f, 0f);
            }
        }
    }

    private void submitModel(RenderPassNode pass, TexturedModel model, float x, float y, float z, float scale,
            float rotX, float rotY, float rotZ) {
        Matrix4f translationMatrix = new Matrix4f().translation(new Vector3f(x, y, z));
        Matrix4f rotationMatrix = new Matrix4f().rotationXYZ(
                (float) Math.toRadians(rotX),
                (float) Math.toRadians(rotY),
                (float) Math.toRadians(rotZ));
        Matrix4f scalingMatrix = new Matrix4f().scale(scale, scale, scale);
        Matrix4f modelMatrix = translationMatrix.mul(scalingMatrix.mul(rotationMatrix));

        pass.submit(new RenderCommand(model.getMesh(), model.getMaterial(), modelMatrix));
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new WindowModule(WINDOW_WIDTH, WINDOW_HEIGHT, "Isometric Chess"))
                .registerModule(new InputModule())
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

    public static void main(String[] args) {
        launch();
    }

    class ChessPiece {
        private TexturedModel model;
        private int x;
        private int z;
        private float rotationY;

        public ChessPiece(TexturedModel model, int x, int z, float rotationY) {
            this.model = model;
            this.x = x;
            this.z = z;
            this.rotationY = rotationY;
        }

        public void draw(RenderPassNode pass) {
            // Reaproveita as constantes de renderização originais das peças
            submitModel(pass, model, x, 0f, z, 1.2f, 0f, rotationY, 0f);
        }

        public int getX() { return x; }
        public int getZ() { return z; }
    }

    class ChessBoard {
        private static final float DEFAULT_PIECE_ROTATION = 90f;
        private final String[] PIECE_TYPES = { "", "pawn", "rook", "knight", "bishop", "queen", "king" };
        private List<ChessPiece> pieces = new ArrayList<>();

        public void placePiece(TexturedModel model, int x, int z, float rotationY) {
            pieces.add(new ChessPiece(model, x, z, rotationY));
        }

        public void drawPieces(RenderPassNode pass) {
            pieces.forEach(piece -> piece.draw(pass));
        }

        public void setupDefaultBoard() {
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

            IntStream.range(0, 8).forEach(z -> IntStream.range(0, 8).forEach(x -> {
                int pieceId = initialSetup[z][x];
                if (pieceId != 0) {
                    createAndPlace(pieceId, x, z);
                }
            }));
        }

        private void createAndPlace(int pieceId, int x, int z) {
            // Se z <= 1, as peças são pretas. Se z >= 6, as peças são brancas.
            String color = (z <= 1) ? "black" : "white";
            String pieceName = PIECE_TYPES[pieceId] + "_" + color;
            // as normais desses .obj apontam pra dentro (confirmado medindo
            // contra o centroide da malha), senao a iluminacao fica invertida
            TexturedModel model = modelLoader.loadWavefrontOBJ(pieceName, litShader, true);

            // Regra de rotação original: Cavalos brancos rotacionam em -180f, pretos em 0f. O resto usa 90f.
            float rotation = DEFAULT_PIECE_ROTATION;
            if (pieceId == 3) { // Se for Cavalo
                rotation = color.equals("white") ? -180f : 0f;
            }

            placePiece(model, x, z, rotation);
        }
    }
}
