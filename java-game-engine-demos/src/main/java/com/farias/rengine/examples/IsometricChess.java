package com.farias.rengine.examples;

import static com.farias.rengine.GameEngine.drawText;
import static com.farias.rengine.GameEngine.drawTexturedModel;
import static com.farias.rengine.GameEngine.getInput;
import static com.farias.rengine.GameEngine.orthographicMode;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.RenderSystem.RenderSystemOptions;
import com.farias.rengine.render.TexturedModel;

public class IsometricChess extends Game {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;
    private static final int FONT_SIZE = 20;
    private static final float CAMERA_SPEED = 5.0f;

    float rotationAngle = 0f;

    private TexturedModel whiteTile;
    private TexturedModel blackTile;
    private ChessBoard chessBoard;

    // ponto observado pela câmera
    private float targetX = 0;
    private float targetY = 0;
    private float targetZ = 0;
    private float fov = 5f;

    // distância da câmera para visão isométrica
    private float cameraDistance = 10f;

    public IsometricChess(Window window) {
        super("Isometric Chess", window);
    }

    @Override
    public void onUserCreate() {
        // Projeção ortográfica = base da visão isométrica
        orthographicMode(WINDOW_WIDTH, WINDOW_HEIGHT);

        // Inicializa os pisos do tabuleiro
        whiteTile = GameEngine.createPlane("yellow_sprite", 1, 1);
        blackTile = GameEngine.createPlane("brown_sprite", 1, 1);

        // Inicializa o tabuleiro e popula as peças internamente
        chessBoard = new ChessBoard();
        chessBoard.setupDefaultBoard();

        updateIsometricCamera();
    }

    private void updateIsometricCamera() {
        GameEngine.setCameraLookAt(
                targetX + cameraDistance,
                targetY + cameraDistance,
                targetZ + cameraDistance,
                targetX,
                targetY,
                targetZ,
                0, 1, 0);
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        // BOARD CONTROLS
        if (getInput().isMouseButtonDown(0)) {
            double mouseX = getInput().getMouseX();
            double mouseY = getInput().getMouseY();

            int boardX = (int) (mouseX / (WINDOW_WIDTH / 8));
            int boardZ = (int) (mouseY / (WINDOW_HEIGHT / 8));

            if (boardX >= 0 && boardX < 8 && boardZ >= 0 && boardZ < 8) {
                System.out.println("Clicked on board position: " + boardX + ", " + boardZ);
            }
        }

        // CAMERA CONTROLS
        float move = CAMERA_SPEED * deltaTime;
        if (getInput().isKeyDown(GLFW_KEY_LEFT)) {
            targetX -= move / 2f;
            targetZ += move / 2f;
        }
        if (getInput().isKeyDown(GLFW_KEY_RIGHT)) {
            targetX += move / 2f;
            targetZ -= move / 2f;
        }
        if (getInput().isKeyDown(GLFW_KEY_UP)) {
            targetY += move;
        }
        if (getInput().isKeyDown(GLFW_KEY_DOWN)) {
            targetY -= move;
        }
        // zoom
        if (getInput().isKeyDown(GLFW_KEY_Z)) {
            if (getInput().isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                fov += 2f * deltaTime * CAMERA_SPEED;
            } else {
                fov -= 2f * deltaTime * CAMERA_SPEED;
            }
            if (cameraDistance < 2f) {
                cameraDistance = 2f;
            }
        }
        updateIsometricCamera();
    }

    @Override
    public void onGfxUpdate(float deltaTime) {
        GameEngine.orthographic3DMode(fov, (float) WINDOW_WIDTH / WINDOW_HEIGHT);
        drawBoard();
        chessBoard.drawPieces();
    }

    private void drawBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                boolean isWhite = (i + j) % 2 == 0;
                TexturedModel tileModel = isWhite ? whiteTile : blackTile;
                drawTexturedModel(tileModel, i, 0f, j, 1f, 90f, 0f, 0f);
            }
        }
    }

    @Override
    public void onUIUpdate(float deltaTime) {
        drawText("ISOMETRIC CHESS - LWJGL RENDER ENGINE", WINDOW_WIDTH / 2, -20, FONT_SIZE, FONT_SIZE);
        drawText("ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT", WINDOW_WIDTH / 2, -50, 14, 14);
    }

    public static void main(String[] args) {
        Window window = new Window(WINDOW_WIDTH, WINDOW_HEIGHT);
        long windowId = window.create();

        IsometricChess game = new IsometricChess(window);

        game.addSystem(new InputSystem(windowId));
        RenderSystemOptions renderOptions = new RenderSystemOptions();
        renderOptions.setEnableTransparency(false);
        renderOptions.setEnableDepthTest(true);
        renderOptions.setEnableCulling(false);
        game.addSystem(new RenderSystem(renderOptions));

        GameEngine.initGame(game);
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

        public void draw() {
            // Reaproveita as constantes de renderização originais das peças
            drawTexturedModel(model, x, 0f, z, 1.2f, 0f, rotationY, 0f);
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

        public void drawPieces() {
            pieces.forEach(ChessPiece::draw);
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
            TexturedModel model = GameEngine.loadTexturedModel(pieceName);

            // Regra de rotação original: Cavalos brancos rotacionam em -180f, pretos em 0f. O resto usa 90f.
            float rotation = DEFAULT_PIECE_ROTATION;
            if (pieceId == 3) { // Se for Cavalo
                rotation = color.equals("white") ? -180f : 0f;
            }

            placePiece(model, x, z, rotation);
        }
    }
}