package com.farias.jengine.examples.chess.pieces;

import com.farias.jengine.examples.chess.ChessBoard;

/**
 * Um lado do jogo e a orientação dele no tabuleiro: as pretas começam em
 * z = 0 e avançam para z maior, as brancas começam em z = 7 e avançam para z
 * menor. Quem precisa saber "para onde é a frente" ou "onde fica a última
 * fileira" pergunta aqui, em vez de repetir os números.
 */
public enum Side {
    BLACK(+1, 0),
    WHITE(-1, ChessBoard.SIZE - 1);

    private final int forward;
    private final int homeRow;

    Side(int forward, int homeRow) {
        this.forward = forward;
        this.homeRow = homeRow;
    }

    /** Quanto o z muda num passo para a frente deste lado: +1 ou -1. */
    public int forward() {
        return forward;
    }

    /** O z da fileira onde começam as peças maiores deste lado. */
    public int homeRow() {
        return homeRow;
    }

    /** O z da fileira onde começam os peões deste lado. */
    public int pawnRow() {
        return homeRow + forward;
    }

    public Side opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
