package com.farias.jengine.examples.chess.pieces;

import java.util.List;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

public final class Knight extends ChessPiece {

    /** Os oito saltos em L, como (dx, dz). */
    private static final int[][] JUMPS = {
            { 1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 },
            { -1, -2 }, { -2, -1 }, { -2, 1 }, { -1, 2 } };

    public Knight(Side side, BoardTile tile) {
        super(PieceType.KNIGHT, side, tile);
    }

    @Override
    public List<BoardTile> getValidMoves(ChessBoard board) {
        return step(board, JUMPS);
    }

    /**
     * O desenho do L: duas casas no eixo longo e depois uma no eixo curto. É só
     * visual; o cavalo salta, então as casas do meio podem estar ocupadas.
     */
    @Override
    protected List<BoardTile> pathTo(BoardTile target) {
        int dx = target.x() - tile().x();
        int dz = target.z() - tile().z();
        int stepX = Integer.signum(dx);
        int stepZ = Integer.signum(dz);
        BoardTile origin = tile();

        if (Math.abs(dx) == 2) {
            return List.of(
                    new BoardTile(origin.x() + stepX, origin.z()),
                    new BoardTile(origin.x() + 2 * stepX, origin.z()),
                    target);
        }
        return List.of(
                new BoardTile(origin.x(), origin.z() + stepZ),
                new BoardTile(origin.x(), origin.z() + 2 * stepZ),
                target);
    }
}
