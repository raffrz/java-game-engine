package com.farias.jengine.examples.chess.model;

/**
 * Onde uma peça está desenhada agora, em unidades do mundo: posição (a casa
 * (x, z) tem centro em (x, 0, z)), altura e giro em graus. É o resultado das
 * animações, que o {@link ChessLogic} avança a cada update; quem desenha só
 * copia esses valores para o modelo.
 *
 * <p>
 * Os campos são package-private: só a lógica escreve.
 */
public final class PiecePose {

    /** As fases da animação de captura, nesta ordem. */
    enum CapturePhase {
        RISING, TRAVELING, LANDING, RESTING
    }

    float x;
    float y;
    float z;
    float spin;

    /** Fase da animação de captura; null enquanto a peça está em jogo. */
    CapturePhase capturePhase;
    /** Para onde a peça capturada vai, fora do tabuleiro. */
    float restX;
    float restZ;

    PiecePose(BoardTile tile) {
        x = tile.x();
        z = tile.z();
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    /** Giro em volta do eixo Y, em graus, somado à orientação do modelo. */
    public float spin() {
        return spin;
    }

    public boolean captured() {
        return capturePhase != null;
    }
}
