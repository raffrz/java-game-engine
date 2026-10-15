package com.farias.jengine.examples.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.joml.Vector3f;

import com.farias.jengine.assets.Assets;
import com.farias.jengine.examples.chess.pieces.ChessPiece;
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
 */
final class ChessScene {

    private static final float TILE_ROTATION_X = 90f;
    private static final float SPECULAR_STRENGTH = 0.5f;
    private static final float SHININESS = 48f;
    private static final Vector3f PATH_TINT_COLOR = new Vector3f(0.2f, 1f, 0.2f);
    private static final float PATH_TINT_STRENGTH = 0.35f;

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

    void update(ChessGame game, float deltaTime) {
        Optional<ChessPiece> selectedPiece = game.selectedPiece();
        Optional<ChessPiece> hoveredPiece = game.hoveredPiece();
        hoveredPieceModels.clear();
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
        highlightPath(game.hoveredPath(), selectedPiece);
    }

    /**
     * Destaca as casas de {@code path} e, se ele não está vazio, a casa de
     * origem da peça selecionada; as outras ficam sem tint.
     */
    private void highlightPath(List<BoardTile> path, Optional<ChessPiece> selectedPiece) {
        for (TexturedModel[] column : tileModels) {
            for (TexturedModel tile : column) {
                tile.lit().tint(PATH_TINT_COLOR, 0f);
            }
        }
        if (path.isEmpty()) {
            return;
        }
        selectedPiece.map(ChessPiece::tile).ifPresent(this::tintTile);
        path.forEach(this::tintTile);
    }

    private void tintTile(BoardTile tile) {
        tileModels[tile.x()][tile.z()].lit().tint(PATH_TINT_COLOR, PATH_TINT_STRENGTH);
    }

    /** Os modelos das peças sob o cursor, conforme o último {@link #update}. */
    List<TexturedModel> hoveredPieceModels() {
        return hoveredPieceModels;
    }

    /** A peça mais próxima atingida pelo raio, ou null se nenhuma. */
    ChessPiece pieceAt(Ray ray) {
        ChessPiece closest = null;
        float closestT = Float.POSITIVE_INFINITY;

        for (PieceView view : pieceViews) {
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
