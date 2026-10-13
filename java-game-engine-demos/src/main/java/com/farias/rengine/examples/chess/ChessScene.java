package com.farias.rengine.examples.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector3f;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.engine3d.IsometricCamera;
import com.farias.rengine.engine3d.Materials;
import com.farias.rengine.engine3d.Meshes;
import com.farias.rengine.engine3d.ModelLoader;
import com.farias.rengine.engine3d.Picking;
import com.farias.rengine.engine3d.Ray;
import com.farias.rengine.engine3d.ShadowMap;
import com.farias.rengine.engine3d.TexturedModel;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;

/**
 * Os modelos 3D do jogo: o piso do tabuleiro e uma {@link PieceView} por peça.
 * Sabe animar as peças e responder qual delas está sob um raio, mas não conhece
 * entrada nem como os passes de render são montados (ver {@link ChessRenderer}).
 *
 * <p>
 * A casa (x, z) tem o centro em (x, 0, z) e lado 1. O quad do piso nasce
 * vertical (plano XY) e é deitado com rotação de 90° em X.
 */
final class ChessScene {

    private static final float TILE_ROTATION_X = 90f;
    private static final float SPECULAR_STRENGTH = 0.5f;
    private static final float SHININESS = 48f;

    private final List<TexturedModel> tiles = new ArrayList<>();
    private final List<PieceView> pieceViews = new ArrayList<>();

    ChessScene(ChessGame game, Assets assets, Meshes meshes, Materials materials, ModelLoader modelLoader,
            ShadowMap shadow, IsometricCamera camera) {
        createTiles(assets, meshes, materials, shadow);
        for (ChessPiece piece : game.pieces()) {
            pieceViews.add(createPieceView(piece, modelLoader, shadow, camera));
        }
    }

    List<TexturedModel> tiles() {
        return tiles;
    }

    List<TexturedModel> pieceModels() {
        return pieceViews.stream().map(PieceView::model).toList();
    }

    void update(ChessGame game, float deltaTime) {
        Optional<ChessPiece> selected = game.selected();
        for (PieceView view : pieceViews) {
            view.update(selected.filter(view.piece()::equals).isPresent(), deltaTime);
        }
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

    /** A casa sob o raio, ou vazio se ele não toca o tabuleiro. */
    Optional<Square> squareAt(Ray ray) {
        float t = Picking.rayPlaneY(ray, 0f);
        if (Float.isNaN(t)) {
            return Optional.empty();
        }

        Vector3f hit = ray.pointAt(t);
        int x = (int) Math.floor(hit.x + 0.5f);
        int z = (int) Math.floor(hit.z + 0.5f);

        return ChessBoard.contains(x, z) ? Optional.of(new Square(x, z)) : Optional.empty();
    }

    private void createTiles(Assets assets, Meshes meshes, Materials materials, ShadowMap shadow) {
        Mesh plane = meshes.plane(1f, 1f);
        Material white = materials.lit(assets.texture("textures/yellow_sprite.png")).receiving(shadow);
        Material black = materials.lit(assets.texture("textures/brown_sprite.png")).receiving(shadow);

        for (int x = 0; x < ChessBoard.SIZE; x++) {
            for (int z = 0; z < ChessBoard.SIZE; z++) {
                TexturedModel tile = new TexturedModel(plane, (x + z) % 2 == 0 ? white : black);
                tile.getPosition().set(x, 0f, z);
                tile.getRotation().set(TILE_ROTATION_X, 0f, 0f);
                tiles.add(tile);
            }
        }
    }

    private PieceView createPieceView(ChessPiece piece, ModelLoader modelLoader, ShadowMap shadow,
            IsometricCamera camera) {
        String name = piece.type().name().toLowerCase() + "_" + piece.side().name().toLowerCase();
        TexturedModel model = modelLoader.loadWavefrontOBJ(name, true);
        model.lit().receiving(shadow).glossy(SPECULAR_STRENGTH, SHININESS, camera.directionToEye());
        return new PieceView(piece, model);
    }
}
