package com.farias.jengine.examples.chess.pieces;

import java.util.ArrayList;
import java.util.List;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

public final class Pawn extends ChessPiece {

    public Pawn(Side side, BoardTile tile) {
        super(PieceType.PAWN, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        List<BoardTile> positions = new ArrayList<>();
        var tileX = tile().x();
        var tileZ = tile().z();
        var signal = side() == Side.WHITE ? -1 : 1;
        int forwardZ = tileZ + signal;

        // passo simples e duplo: só em casa vazia
        if (ChessBoard.contains(tileX, forwardZ) && board.pieceAt(tileX, forwardZ).isEmpty()) {
            positions.add(new BoardTile(tileX, forwardZ));

            int startZ = side() == Side.WHITE ? 6 : 1;
            int doubleZ = tileZ + 2 * signal;
            if (tileZ == startZ && board.pieceAt(tileX, doubleZ).isEmpty()) {
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
