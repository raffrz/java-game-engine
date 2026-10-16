package com.farias.jengine.examples.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.joml.Vector3f;

import com.farias.jengine.assets.Assets;
import com.farias.jengine.examples.chess.pieces.ChessPiece;
import com.farias.jengine.examples.chess.pieces.Side;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.LitMaterials;
import com.farias.jengine.toolkit3d.Models;
import com.farias.jengine.toolkit3d.Picking;
import com.farias.jengine.toolkit3d.Ray;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.Shapes;
import com.farias.jengine.toolkit3d.TexturedModel;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.Texture;

/**
 * Os modelos 3D do jogo: o piso do tabuleiro e uma {@link PieceView} por peça.
 * Sabe animar as peças e responder qual delas está sob um raio, mas não conhece
 * entrada nem como os passes de render são montados (ver {@link ChessRenderer}).
 *
 * <p>
 * A casa (x, z) tem o centro em (x, 0, z) e lado 1. O quad do piso nasce
 * vertical (plano XY) e é deitado com rotação de 90° em X. Cada casa tem o
 * próprio material (as texturas são compartilhadas), para o tint do caminho
 * valer só para ela.
 *
 * <p>
 * Peças capturadas continuam na cena: são levadas para fora do tabuleiro,
 * atrás da última fileira de quem capturou, e saem do picking.
 */
final class ChessScene {

    private static final float TILE_ROTATION_X = 90f;
    private static final float SPECULAR_STRENGTH = 0.5f;
    private static final float SHININESS = 48f;
    private static final Vector3f PATH_TINT_COLOR = new Vector3f(0.2f, 1f, 0.2f);
    private static final float PATH_TINT_STRENGTH = 0.35f;
    /** Distância da primeira fileira de capturadas à última fileira do tabuleiro. */
    private static final float CAPTURED_ROW_DISTANCE = 1.5f;
    private static final float CAPTURED_ROW_SPACING = 1f;

    /** O modelo da casa (x, z) fica em {@code tileModels[x][z]}. */
    private final TexturedModel[][] tileModels = new TexturedModel[ChessBoard.SIZE][ChessBoard.SIZE];
    private final List<PieceView> pieceViews = new ArrayList<>();
    private final List<TexturedModel> hoveredPieceModels = new ArrayList<>();
    private final IsometricCamera camera;

    ChessScene(ChessGame game, Assets assets, Shapes shapes, LitMaterials materials, Models models,
            ShadowMap shadow, IsometricCamera camera) {
        this.camera = camera;
        createTiles(assets, shapes, materials, shadow);
        for (ChessPiece piece : game.pieces()) {
            pieceViews.add(createPieceView(piece, models, shadow, camera));
        }
    }

    List<TexturedModel> tileModels() {
        return Arrays.stream(tileModels).flatMap(Arrays::stream).toList();
    }

    List<TexturedModel> pieceModels() {
        return pieceViews.stream().map(PieceView::model).toList();
    }

    void update(ChessGame game, ChessInteraction interaction, float deltaTime) {
        Optional<ChessPiece> selectedPiece = interaction.selectedPiece();
        Optional<ChessPiece> hoveredPiece = interaction.hoveredPiece();
        hoveredPieceModels.clear();
        for (Move move : game.takeMoves()) {
            move.captured().ifPresent(captured -> {
                int slot = game.captured(captured.side()).indexOf(captured);
                viewOf(captured).capture(restingPlace(captured.side(), slot));
            });
        }
        // a câmera pode ter girado desde o último frame; o brilho acompanha
        Vector3f directionToEye = camera.directionToEye();
        for (PieceView view : pieceViews) {
            view.viewFrom(directionToEye);
            boolean isSelected = selectedPiece.filter(view.piece()::equals).isPresent();
            boolean isHovered = hoveredPiece.filter(view.piece()::equals).isPresent();
            view.update(isSelected, deltaTime);
            if (isHovered) {
                hoveredPieceModels.add(view.model());
            }
        }
        highlight(interaction.highlightedTiles());
    }

    /** Destaca as casas de {@code tiles}; as outras ficam sem tint. */
    private void highlight(List<BoardTile> tiles) {
        for (TexturedModel[] column : tileModels) {
            for (TexturedModel tile : column) {
                tile.lit().tint(PATH_TINT_COLOR, 0f);
            }
        }
        tiles.forEach(this::tintTile);
    }

    private void tintTile(BoardTile tile) {
        tileModels[tile.x()][tile.z()].lit().tint(PATH_TINT_COLOR, PATH_TINT_STRENGTH);
    }

    private PieceView viewOf(ChessPiece piece) {
        return pieceViews.stream().filter(view -> view.piece() == piece).findFirst().orElseThrow();
    }

    /**
     * Onde fica a {@code slot}-ésima peça capturada de {@code capturedSide}
     * (ver {@link ChessGame#captured}): fora do tabuleiro, atrás da última
     * fileira de quem capturou (brancas em z = 7, pretas em z = 0), em
     * fileiras de {@code SIZE} peças ao longo de x.
     */
    private static Vector3f restingPlace(Side capturedSide, int slot) {
        boolean byWhite = capturedSide == Side.BLACK;
        float distance = CAPTURED_ROW_DISTANCE + (slot / ChessBoard.SIZE) * CAPTURED_ROW_SPACING;
        float z = byWhite ? ChessBoard.SIZE - 1 + distance : -distance;
        return new Vector3f(slot % ChessBoard.SIZE, 0f, z);
    }

    /** Os modelos das peças sob o cursor, conforme o último {@link #update}. */
    List<TexturedModel> hoveredPieceModels() {
        return hoveredPieceModels;
    }

    /** A peça em jogo mais próxima atingida pelo raio, ou null se nenhuma. */
    ChessPiece pieceAt(Ray ray) {
        ChessPiece closest = null;
        float closestT = Float.POSITIVE_INFINITY;

        for (PieceView view : pieceViews) {
            if (view.isCaptured()) {
                continue;
            }
            float t = view.hit(ray);
            if (!Float.isNaN(t) && t < closestT) {
                closestT = t;
                closest = view.piece();
            }
        }
        return closest;
    }

    /** A casa sob o raio, ou null se ele não toca o tabuleiro. */
    BoardTile tileAt(Ray ray) {
        float t = Picking.rayPlaneY(ray, 0f);
        if (Float.isNaN(t)) {
            return null;
        }

        Vector3f hit = ray.pointAt(t);
        int x = (int) Math.floor(hit.x + 0.5f);
        int z = (int) Math.floor(hit.z + 0.5f);

        return ChessBoard.contains(x, z) ? new BoardTile(x, z) : null;
    }

    private void createTiles(Assets assets, Shapes shapes, LitMaterials materials, ShadowMap shadow) {
        Mesh plane = shapes.plane(1f, 1f);
        Texture white = assets.texture("textures/yellow_sprite.png");
        Texture black = assets.texture("textures/brown_sprite.png");

        for (int x = 0; x < ChessBoard.SIZE; x++) {
            for (int z = 0; z < ChessBoard.SIZE; z++) {
                Texture texture = (x + z) % 2 == 0 ? white : black;
                TexturedModel tile = new TexturedModel(plane, materials.create(texture).receiving(shadow));
                tile.getPosition().set(x, 0f, z);
                tile.getRotation().set(TILE_ROTATION_X, 0f, 0f);
                tileModels[x][z] = tile;
            }
        }
    }

    private PieceView createPieceView(ChessPiece piece, Models models, ShadowMap shadow,
            IsometricCamera camera) {
        String name = piece.type().name().toLowerCase() + "_" + piece.side().name().toLowerCase();
        // o mesh de cada tipo de peca e compartilhado; o material e por peca
        TexturedModel model = models.load("models/" + name + ".obj", "textures/" + name + ".png", true);
        model.lit().receiving(shadow).glossy(SPECULAR_STRENGTH, SHININESS, camera.directionToEye());
        return new PieceView(piece, model);
    }
}
