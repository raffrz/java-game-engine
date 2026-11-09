package com.farias.jengine.examples.chess;

import java.util.Arrays;
import java.util.Map;

import org.joml.Vector3f;

import com.farias.jengine.examples.chess.model.BoardTile;
import com.farias.jengine.examples.chess.model.ChessBoard;
import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.ChessState;
import com.farias.jengine.examples.chess.model.PiecePose;
import com.farias.jengine.examples.chess.model.PieceType;
import com.farias.jengine.examples.chess.model.Side;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.RenderPass;
import com.farias.jengine.renderer.RenderPassDescriptor;
import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.toolkit2d.Toolkit2D;
import com.farias.jengine.toolkit2d.Ui;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.Picking;
import com.farias.jengine.toolkit3d.Ray;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * Apresenta o {@link ChessState}. A cada frame põe cada modelo da
 * {@link ChessScene} na pose que o estado manda, pinta as casas destacadas e
 * monta os passes: sombra, cena 3D, contorno da peça sob o cursor (ver
 * {@link ModelOutline}) e UI. Não guarda nada entre frames: tudo o que muda
 * vem do estado. Também responde o picking ({@link #tileUnder}).
 *
 * <p>
 * O passe 3D não usa culling: os .obj das peças têm winding e normais
 * invertidos (ver CHESS_MODELS.md), por isso o carregamento usa
 * {@code flipNormals} e, sem blend, o depth test sozinho escolhe a face
 * visível. Só as peças projetam sombra.
 */
final class ChessRenderer {

    private static final int TURN_FONT_SIZE = 14;
    private static final int HINT_FONT_SIZE = 10;

    /** Orientação dos modelos para ficarem de frente para o adversário. */
    private static final float PIECE_ROTATION_Y = 90f;
    private static final Vector3f HIGHLIGHT_TINT_COLOR = new Vector3f(0.2f, 1f, 0.2f);
    private static final float HIGHLIGHT_TINT_STRENGTH = 0.35f;

    private final Renderer renderer;
    private final Ui ui;
    private final Window window;
    private final int uiWidth;
    private final IsometricCamera camera;
    private final ShadowMap shadow;
    private final ModelOutline outline;
    private final ChessScene scene;
    private final RenderPassDescriptor scene3DDescriptor = new RenderPassDescriptor("isometric-chess-3d")
            .clearColor(0f, 0f, 0f, 1f)
            .depthTest(true)
            .cullFace(false)
            .blend(false);

    /**
     * Resolve em {@code context} o renderer, a janela e o toolkit 2D; a UI usa
     * o tamanho lógico da janela.
     */
    ChessRenderer(EngineContext context, ChessScene scene, IsometricCamera camera) {
        this.renderer = context.resolve(Renderer.class);
        this.window = context.resolve(Window.class);
        this.uiWidth = window.getWidth();
        this.ui = context.resolve(Toolkit2D.class).ui(window.getWidth(), window.getHeight());
        this.camera = camera;
        this.shadow = scene.shadow();
        this.outline = new ModelOutline(renderer, window, camera);
        this.scene = scene;
    }

    void render(ChessState state) {
        placePieces(state);
        tintTiles(state);

        renderer.beginFrame();
        drawShadows();
        drawScene();
        outline.draw(state.pointedPiece().map(scene.pieces()::get).stream().toList());
        drawUi(state);
        renderer.endFrame();
    }

    /**
     * A casa sob {@code ray}, ou null fora do tabuleiro, no que foi desenhado.
     * A peça tem prioridade: se o raio acerta uma peça em jogo, a casa é a
     * dela, e não a que o raio atinge atrás.
     */
    BoardTile tileUnder(ChessState state, Ray ray) {
        ChessPiece closest = null;
        float closestT = Float.POSITIVE_INFINITY;
        for (ChessPiece piece : state.pieces()) {
            TexturedModel model = scene.pieces().get(piece);
            float t = Picking.rayBounds(ray, model.getMesh().bounds(), model.getModelMatrix());
            if (!Float.isNaN(t) && t < closestT) {
                closestT = t;
                closest = piece;
            }
        }
        return closest != null ? closest.tile() : floorTileUnder(ray);
    }

    /** A casa do piso sob o raio, ou null se ele não toca o tabuleiro. */
    private static BoardTile floorTileUnder(Ray ray) {
        float t = Picking.rayPlaneY(ray, 0f);
        if (Float.isNaN(t)) {
            return null;
        }
        Vector3f hit = ray.pointAt(t);
        int x = (int) Math.floor(hit.x + 0.5f);
        int z = (int) Math.floor(hit.z + 0.5f);
        return ChessBoard.contains(x, z) ? new BoardTile(x, z) : null;
    }

    /** Copia a pose de cada peça (ver {@link PiecePose}) para o modelo dela. */
    private void placePieces(ChessState state) {
        // a câmera pode ter girado desde o último frame; o brilho acompanha
        Vector3f directionToEye = camera.directionToEye();
        for (Map.Entry<ChessPiece, TexturedModel> entry : scene.pieces().entrySet()) {
            PiecePose pose = state.pose(entry.getKey());
            TexturedModel model = entry.getValue();
            model.getPosition().set(pose.x(), pose.y(), pose.z());
            model.getRotation().set(0f, modelRotationY(entry.getKey()) + pose.spin(), 0f);
            model.lit().viewDirection(directionToEye);
        }
    }

    private void tintTiles(ChessState state) {
        for (TexturedModel[] column : scene.tiles()) {
            for (TexturedModel tile : column) {
                tile.lit().tint(HIGHLIGHT_TINT_COLOR, 0f);
            }
        }
        for (BoardTile tile : state.highlightedTiles()) {
            scene.tiles()[tile.x()][tile.z()].lit().tint(HIGHLIGHT_TINT_COLOR, HIGHLIGHT_TINT_STRENGTH);
        }
    }

    /**
     * A rotação que cada modelo precisa para olhar para o adversário. Os .obj
     * do cavalo foram modelados virados para outro lado, por isso o ângulo
     * dele é diferente e depende da cor.
     */
    private static float modelRotationY(ChessPiece piece) {
        if (piece.type() != PieceType.KNIGHT) {
            return PIECE_ROTATION_Y;
        }
        return piece.side() == Side.WHITE ? -180f : 0f;
    }

    private void drawShadows() {
        RenderPass pass = renderer.createPass(shadow.descriptor(), shadow.view());
        scene.pieces().values().forEach(model -> pass.submit(shadow.caster(model)));
    }

    private void drawScene() {
        RenderPass pass = renderer.createPass(scene3DDescriptor, camera.view(window));
        Arrays.stream(scene.tiles()).flatMap(Arrays::stream).forEach(tile -> pass.submit(tile.command()));
        scene.pieces().values().forEach(model -> pass.submit(model.command()));
    }

    /** De quem é a vez (com xeque ou fim de partida) e, abaixo, os controles. */
    private void drawUi(ChessState state) {
        int centerX = uiWidth / 2;
        ui.begin();
        ui.textCentered(ui.defaultFont(), statusText(state), centerX, 20, TURN_FONT_SIZE);
        ui.textCentered(ui.defaultFont(), "SETAS = MOVER | RODA = ZOOM | BOTAO DO MEIO = GIRAR | CLIQUE = JOGAR",
                centerX, 44, HINT_FONT_SIZE);
    }

    private static String statusText(ChessState state) {
        String side = state.turn() == Side.WHITE ? "BRANCAS" : "PRETAS";
        String winner = state.turn() == Side.WHITE ? "PRETAS" : "BRANCAS";
        return switch (state.status()) {
            case PLAYING -> "VEZ DAS " + side;
            case CHECK -> "XEQUE - VEZ DAS " + side;
            case CHECKMATE -> "XEQUE-MATE - " + winner + " VENCEM";
            case STALEMATE -> "AFOGAMENTO - EMPATE";
        };
    }
}
