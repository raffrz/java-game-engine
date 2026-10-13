package com.farias.rengine.examples.chess;

import java.util.List;
import java.util.Optional;

/**
 * Estado do jogo: o {@link ChessBoard} e a peça selecionada. Não depende de
 * renderização, janela nem da engine; quem desenha o jogo (ver
 * {@code ChessScene}) lê este estado e mostra como quiser.
 */
public final class ChessGame {

    private final ChessBoard board = ChessBoard.initialPosition();
    private ChessPiece selected;

    public List<ChessPiece> pieces() {
        return board.pieces();
    }

    public Optional<ChessPiece> selected() {
        return Optional.ofNullable(selected);
    }

    /**
     * Seleciona {@code piece}. Selecionar a peça que já está selecionada, ou
     * {@code null}, desfaz a seleção.
     */
    public void select(ChessPiece piece) {
        selected = piece != null && piece.equals(selected) ? null : piece;
    }
}
