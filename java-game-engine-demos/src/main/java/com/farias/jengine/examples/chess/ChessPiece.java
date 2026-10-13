package com.farias.jengine.examples.chess;

/** Uma peça e a casa onde ela está. */
public record ChessPiece(PieceType type, Side side, int x, int z) {

    public Square square() {
        return new Square(x, z);
    }
}
