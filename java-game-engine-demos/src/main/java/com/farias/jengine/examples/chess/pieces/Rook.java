package com.farias.jengine.examples.chess.pieces;

import java.util.List;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

public final class Rook extends ChessPiece {

    public Rook(Side side, BoardTile tile) {
        super(PieceType.ROOK, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        return slide(board, ORTHOGONAL);
    }
}
