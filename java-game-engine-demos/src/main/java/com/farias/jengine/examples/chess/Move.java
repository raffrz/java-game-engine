package com.farias.jengine.examples.chess;

import java.util.List;
import java.util.Optional;

import com.farias.jengine.examples.chess.pieces.ChessPiece;

/**
 * Um lance já feito: {@code piece} saiu de {@code from} e foi para {@code to}
 * percorrendo {@code path} (ver {@link ChessPiece#getPathToTarget}, calculado
 * antes do movimento), capturando {@code captured} se havia peça lá.
 */
public record Move(ChessPiece piece, BoardTile from, BoardTile to, List<BoardTile> path,
        Optional<ChessPiece> captured) {
}
