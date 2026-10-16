package com.farias.jengine.examples.chess;

import java.util.ArrayList;
import java.util.List;

import com.farias.jengine.examples.chess.pieces.ChessPiece;
import com.farias.jengine.examples.chess.pieces.Side;

/**
 * As regras do jogo sobre o {@link ChessBoard}: quais movimentos valem e o que
 * acontece quando uma peça se move. Não sabe de seleção, hover nem cliques
 * (ver {@link ChessInteraction}), nem de renderização, janela ou engine.
 */
public final class ChessGame {

    private final ChessBoard board = ChessBoard.initialPosition();
    private final List<ChessPiece> recentCaptures = new ArrayList<>();

    /** As peças em jogo. */
    public List<ChessPiece> pieces() {
        return board.pieces();
    }

    /** As peças de {@code side} capturadas até agora, na ordem da captura. */
    public List<ChessPiece> captured(Side side) {
        return board.captured(side);
    }

    /** As casas para onde {@code piece} pode ir no tabuleiro atual. */
    public List<BoardTile> validMoves(ChessPiece piece) {
        return piece.getValidMoves(board);
    }

    /**
     * O caminho de {@code piece} até {@code target} (ver
     * {@link ChessPiece#getPathToTarget}); vazio se não é um movimento válido.
     */
    public List<BoardTile> pathTo(ChessPiece piece, BoardTile target) {
        return piece.getPathToTarget(board, target);
    }

    /**
     * Move {@code piece} para {@code target}, capturando a peça que estiver
     * lá. Não faz nada se não é um movimento válido.
     *
     * @return se a peça foi movida
     */
    public boolean move(ChessPiece piece, BoardTile target) {
        if (!validMoves(piece).contains(target)) {
            return false;
        }
        board.move(piece, target).ifPresent(recentCaptures::add);
        return true;
    }

    /**
     * As peças capturadas desde a última chamada, na ordem em que foram
     * capturadas. Esvazia a lista: é para um consumidor só (a cena, uma vez por
     * frame).
     */
    public List<ChessPiece> takeRecentCaptures() {
        List<ChessPiece> captures = List.copyOf(recentCaptures);
        recentCaptures.clear();
        return captures;
    }
}
