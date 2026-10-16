package com.farias.jengine.examples.chess.model.pieces;

import java.util.List;

import com.farias.jengine.examples.chess.model.BoardTile;
import com.farias.jengine.examples.chess.model.ChessBoard;
import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.PieceType;
import com.farias.jengine.examples.chess.model.Side;

public final class Rook extends ChessPiece {

    public Rook(Side side, BoardTile tile) {
        super(PieceType.ROOK, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        return slide(board, ORTHOGONAL);
    }
}
