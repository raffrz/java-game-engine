package com.farias.jengine.examples.chess.pieces;

import java.util.List;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

public final class Queen extends ChessPiece {

    public Queen(Side side, BoardTile tile) {
        super(PieceType.QUEEN, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        return slide(board, ALL_DIRECTIONS);
    }
}
