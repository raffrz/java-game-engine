package com.farias.jengine.renderer;

/**
 * Como o teste de profundidade de um passe compara o fragmento novo com a profundidade já gravada: o fragmento passa
 * quando a comparação dá verdadeiro. {@link #LESS} é o normal (o mais perto da câmera ganha); {@link #GREATER} desenha só
 * o que está atrás do que já foi desenhado (uma silhueta vista através de uma parede, por exemplo).
 */
public enum DepthCompare {
    /** Passa o que está mais perto que o gravado (o padrão). */
    LESS,
    /** Passa o que está mais perto ou na mesma profundidade. */
    LESS_OR_EQUAL,
    /** Passa só o que está mais longe que o gravado: o que algo já desenhado esconde. */
    GREATER,
    /** Passa o que está mais longe ou na mesma profundidade. */
    GREATER_OR_EQUAL,
    /** Passa sempre (mas, ao contrário de desligar o teste, ainda grava a profundidade se a escrita estiver ligada). */
    ALWAYS
}
