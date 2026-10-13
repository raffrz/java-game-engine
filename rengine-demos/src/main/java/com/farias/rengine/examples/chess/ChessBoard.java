package com.farias.rengine.examples.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * O tabuleiro 8x8 e as peças sobre ele, com x e z de 0 a 7. As pretas começam
 * em z = 0 e z = 1, as brancas em z = 6 e z = 7.
 */
public final class ChessBoard {

    public static final int SIZE = 8;

    private static final PieceType[] BACK_RANK = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
    };

    private final List<ChessPiece> pieces = new ArrayList<>();

    /** Um tabuleiro na posição inicial. */
    public static ChessBoard initialPosition() {
        ChessBoard board = new ChessBoard();
        for (int x = 0; x < SIZE; x++) {
            board.pieces.add(new ChessPiece(BACK_RANK[x], Side.BLACK, x, 0));
            board.pieces.add(new ChessPiece(PieceType.PAWN, Side.BLACK, x, 1));
            board.pieces.add(new ChessPiece(PieceType.PAWN, Side.WHITE, x, SIZE - 2));
            board.pieces.add(new ChessPiece(BACK_RANK[x], Side.WHITE, x, SIZE - 1));
        }
        return board;
    }

    public static boolean contains(int x, int z) {
        return x >= 0 && x < SIZE && z >= 0 && z < SIZE;
    }

    public List<ChessPiece> pieces() {
        return Collections.unmodifiableList(pieces);
    }
}
