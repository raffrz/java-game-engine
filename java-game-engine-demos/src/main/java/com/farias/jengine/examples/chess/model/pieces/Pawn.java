package com.farias.jengine.examples.chess.model.pieces;

import java.util.ArrayList;
import java.util.List;

import com.farias.jengine.examples.chess.model.BoardTile;
import com.farias.jengine.examples.chess.model.ChessBoard;
import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.PieceType;
import com.farias.jengine.examples.chess.model.Side;

public final class Pawn extends ChessPiece {

    public Pawn(Side side, BoardTile tile) {
        super(PieceType.PAWN, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        List<BoardTile> positions = new ArrayList<>();
        var tileX = tile().x();
        var tileZ = tile().z();
        var signal = side().forward();
        int forwardZ = tileZ + signal;

        // passo simples e duplo: só em casa vazia
        if (ChessBoard.contains(tileX, forwardZ) && board.pieceAt(tileX, forwardZ).isEmpty()) {
            positions.add(new BoardTile(tileX, forwardZ));

            int doubleZ = tileZ + 2 * signal;
            if (tileZ == side().pawnRow() && board.pieceAt(tileX, doubleZ).isEmpty()) {
                positions.add(new BoardTile(tileX, doubleZ));
            }
        }

        // capturas: diagonal à frente, só com peça adversária
        for (int dx : new int[] { -1, 1 }) {
            int x = tileX + dx;
            if (ChessBoard.contains(x, forwardZ)
                    && board.pieceAt(x, forwardZ).filter(p -> p.side() != side()).isPresent()) {
                positions.add(new BoardTile(x, forwardZ));
            }
        }

        return positions;
    }
}
