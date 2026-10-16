package com.farias.jengine.examples.chess;

import java.util.Map;

import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * A cena 3D do jogo, só dados: o mapa de sombras da luz do tabuleiro, o piso
 * ({@code tiles[x][z]} é a casa (x, z)) e um modelo por peça, inclusive as
 * capturadas. Não tem comportamento: {@link ChessSceneFactory} cria, e o
 * {@link ChessRenderer} posiciona e desenha a partir do estado do jogo.
 *
 * <p>
 * A câmera fica de fora: ela muda a cada frame e é compartilhada entre
 * controles e render, então quem a liga às partes é o {@link IsometricChess}.
 */
record ChessScene(ShadowMap shadow, TexturedModel[][] tiles, Map<ChessPiece, TexturedModel> pieces) {
}
