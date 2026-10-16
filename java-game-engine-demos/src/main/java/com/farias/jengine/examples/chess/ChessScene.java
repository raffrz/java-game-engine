package com.farias.jengine.examples.chess;

import java.util.Map;

import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * Os modelos 3D do jogo, só dados: o piso ({@code tiles[x][z]} é a casa
 * (x, z)) e um modelo por peça, inclusive as capturadas. Não tem
 * comportamento: {@link ChessSceneFactory} cria, e o {@link ChessRenderer}
 * posiciona e desenha a partir do estado do jogo.
 */
record ChessScene(TexturedModel[][] tiles, Map<ChessPiece, TexturedModel> pieces) {
}
