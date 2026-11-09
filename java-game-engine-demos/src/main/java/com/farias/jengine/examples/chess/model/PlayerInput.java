package com.farias.jengine.examples.chess.model;

/**
 * O que o jogador fez neste frame, já traduzido para o tabuleiro: a casa que o
 * cursor aponta (a de uma peça, se o cursor está sobre ela; null fora do
 * tabuleiro) e se clicou. Quem traduz o mouse em casa é a apresentação; a
 * lógica só vê casas.
 */
public record PlayerInput(BoardTile pointedTile, boolean clicked) {
}
