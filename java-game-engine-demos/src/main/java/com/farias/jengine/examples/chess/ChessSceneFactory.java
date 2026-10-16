package com.farias.jengine.examples.chess;

import java.util.HashMap;
import java.util.Map;

import org.joml.Vector3f;

import com.farias.jengine.examples.chess.model.ChessBoard;
import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.ChessState;
import com.farias.jengine.assets.Assets;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.Texture;
import com.farias.jengine.toolkit3d.DirectionalLight;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.LitMaterials;
import com.farias.jengine.toolkit3d.Models;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.Shadows;
import com.farias.jengine.toolkit3d.Shapes;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * Monta a {@link ChessScene} (o mapa de sombras, as 64 casas e um modelo por
 * peça) e configura a câmera do jogo.
 *
 * <p>
 * A câmera olha (e gira em volta) do centro do tabuleiro, e a luz que gera o
 * mapa de sombras cobre o tabuleiro inteiro; é a mesma do sombreamento
 * difuso.
 *
 * <p>
 * A casa (x, z) tem o centro em (x, 0, z) e lado 1. O quad do piso nasce
 * vertical (plano XY) e é deitado com rotação de 90° em X. Cada casa tem o
 * próprio material (as texturas são compartilhadas), para o tint do destaque
 * valer só para ela.
 */
final class ChessSceneFactory {

    private static final float CAMERA_OFFSET = 10f;
    private static final float CAMERA_SIZE = 5f;

    private static final int SHADOW_MAP_SIZE = 2048;
    private static final Vector3f LIGHT_DIRECTION = new Vector3f(0.25f, 1f, 0.75f);
    private static final float LIGHT_RADIUS = 7f;

    private static final float TILE_ROTATION_X = 90f;
    private static final float PIECE_SCALE = 1.2f;
    private static final float SPECULAR_STRENGTH = 0.5f;
    private static final float SHININESS = 48f;

    private ChessSceneFactory() {
    }

    /** A câmera do jogo, olhando para o centro do tabuleiro. */
    static IsometricCamera camera() {
        IsometricCamera camera = new IsometricCamera(CAMERA_OFFSET, CAMERA_SIZE);
        camera.target().set(boardCenter());
        return camera;
    }

    /**
     * Cria o mapa de sombras e os modelos das casas e de cada peça de
     * {@code state}, com os serviços de assets e 3D resolvidos em
     * {@code context}. O brilho das peças é calculado para {@code camera}.
     */
    static ChessScene create(EngineContext context, ChessState state, IsometricCamera camera) {
        ShadowMap shadow = context.resolve(Shadows.class).create(boardLight(), SHADOW_MAP_SIZE);

        TexturedModel[][] tiles = createTiles(context.resolve(Assets.class), context.resolve(Shapes.class),
                context.resolve(LitMaterials.class), shadow);
        Map<ChessPiece, TexturedModel> pieces = createPieces(state, context.resolve(Models.class), shadow, camera);
        return new ChessScene(shadow, tiles, pieces);
    }

    private static DirectionalLight boardLight() {
        return DirectionalLight.covering(LIGHT_DIRECTION, boardCenter(), LIGHT_RADIUS);
    }

    /** Centro do tabuleiro: a casa (x, z) tem centro em (x, 0, z), de 0 a SIZE - 1. */
    private static Vector3f boardCenter() {
        float center = (ChessBoard.SIZE - 1) / 2f;
        return new Vector3f(center, 0f, center);
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
