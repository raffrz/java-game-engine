package com.farias.jengine.examples.chess;

import java.util.HashMap;
import java.util.Map;

import com.farias.jengine.examples.chess.model.ChessBoard;
import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.ChessState;
import com.farias.jengine.assets.Assets;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.Texture;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.LitMaterials;
import com.farias.jengine.toolkit3d.Models;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.Shapes;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * Carrega os modelos da {@link ChessScene}: as 64 casas e um modelo por peça.
 *
 * <p>
 * A casa (x, z) tem o centro em (x, 0, z) e lado 1. O quad do piso nasce
 * vertical (plano XY) e é deitado com rotação de 90° em X. Cada casa tem o
 * próprio material (as texturas são compartilhadas), para o tint do destaque
 * valer só para ela.
 */
final class ChessSceneFactory {

    private static final float TILE_ROTATION_X = 90f;
    private static final float PIECE_SCALE = 1.2f;
    private static final float SPECULAR_STRENGTH = 0.5f;
    private static final float SHININESS = 48f;

    private ChessSceneFactory() {
    }

    /**
     * Cria os modelos das casas e de cada peça de {@code state}, com os
     * serviços de assets e 3D resolvidos em {@code context}.
     */
    static ChessScene create(EngineContext context, ChessState state, ShadowMap shadow,
            IsometricCamera camera) {
        TexturedModel[][] tiles = createTiles(context.resolve(Assets.class), context.resolve(Shapes.class),
                context.resolve(LitMaterials.class), shadow);
        Map<ChessPiece, TexturedModel> pieces = createPieces(state, context.resolve(Models.class), shadow, camera);
        return new ChessScene(tiles, pieces);
    }

    private static TexturedModel[][] createTiles(Assets assets, Shapes shapes, LitMaterials materials,
            ShadowMap shadow) {
        Mesh plane = shapes.plane(1f, 1f);
        Texture white = assets.texture("textures/yellow_sprite.png");
        Texture black = assets.texture("textures/brown_sprite.png");

        TexturedModel[][] tiles = new TexturedModel[ChessBoard.SIZE][ChessBoard.SIZE];
        for (int x = 0; x < ChessBoard.SIZE; x++) {
            for (int z = 0; z < ChessBoard.SIZE; z++) {
                Texture texture = (x + z) % 2 == 0 ? white : black;
                TexturedModel tile = new TexturedModel(plane, materials.create(texture).receiving(shadow));
                tile.getPosition().set(x, 0f, z);
                tile.getRotation().set(TILE_ROTATION_X, 0f, 0f);
                tiles[x][z] = tile;
            }
        }
        return tiles;
    }

    private static Map<ChessPiece, TexturedModel> createPieces(ChessState state, Models models,
            ShadowMap shadow, IsometricCamera camera) {
        Map<ChessPiece, TexturedModel> pieces = new HashMap<>();
        for (ChessPiece piece : state.allPieces()) {
            String name = piece.type().name().toLowerCase() + "_" + piece.side().name().toLowerCase();
            // o mesh de cada tipo de peca e compartilhado; o material e por peca
            TexturedModel model = models.load("models/" + name + ".obj", "textures/" + name + ".png", true);
            model.lit().receiving(shadow).glossy(SPECULAR_STRENGTH, SHININESS, camera.directionToEye());
            model.getScale().set(PIECE_SCALE);
            pieces.put(piece, model);
        }
        return pieces;
    }
}
