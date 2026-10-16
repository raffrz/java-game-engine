package com.farias.jengine.examples.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.farias.jengine.examples.chess.pieces.ChessPiece;
import com.farias.jengine.examples.chess.pieces.Side;

/**
 * As regras do jogo sobre o {@link ChessBoard}: quais movimentos valem e o que
 * acontece quando uma peça se move. Não sabe de seleção, hover nem cliques
 * (ver {@link ChessInteraction}), nem de renderização, janela ou engine.
 */
public final class ChessGame {

    private final ChessBoard board = ChessBoard.initialPosition();
    private final List<Move> recentMoves = new ArrayList<>();

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
     * lá, e registra o lance (ver {@link #takeMoves}). Não faz nada se não é
     * um movimento válido.
     *
     * @return se a peça foi movida
     */
    public boolean move(ChessPiece piece, BoardTile target) {
        List<BoardTile> path = pathTo(piece, target);
        if (path.isEmpty()) {
            return false;
        }
        BoardTile from = piece.tile();
        Optional<ChessPiece> captured = board.move(piece, target);
        recentMoves.add(new Move(piece, from, target, path, captured));
        return true;
    }

    /**
     * Os lances feitos desde a última chamada, em ordem. Esvazia a lista: é
     * para um consumidor só (a cena, uma vez por frame).
     */
    public List<Move> takeMoves() {
        List<Move> moves = List.copyOf(recentMoves);
        recentMoves.clear();
        return moves;
    }
}
