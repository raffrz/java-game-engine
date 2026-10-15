package com.farias.jengine.examples.chess.pieces;

import java.util.List;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

public final class Bishop extends ChessPiece {

    public Bishop(Side side, BoardTile tile) {
        super(PieceType.BISHOP, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        return slide(board, DIAGONAL);
    }
}
