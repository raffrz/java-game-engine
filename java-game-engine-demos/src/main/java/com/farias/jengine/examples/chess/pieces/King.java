package com.farias.jengine.examples.chess.pieces;

import java.util.List;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

public final class King extends ChessPiece {

    public King(Side side, BoardTile tile) {
        super(PieceType.KING, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        return step(board, ALL_DIRECTIONS);
    }
}
