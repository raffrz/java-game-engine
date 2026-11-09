package com.farias.jengine.examples.chess.model;

/**
 * Como a partida está para quem tem a vez ({@link ChessState#turn}): jogando
 * normalmente, em xeque, ou acabada por xeque-mate ou afogamento (sem lance
 * válido e sem estar em xeque, empate).
 */
public enum GameStatus {
    PLAYING, CHECK, CHECKMATE, STALEMATE;

    /** Se a partida acabou: não há mais lances. */
    public boolean isOver() {
        return this == CHECKMATE || this == STALEMATE;
    }
}
