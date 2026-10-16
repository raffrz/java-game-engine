package com.farias.jengine.examples.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.farias.jengine.examples.chess.model.BoardTile;
import com.farias.jengine.examples.chess.model.ChessGame;
import com.farias.jengine.examples.chess.model.ChessPiece;

/**
 * O que o jogador está apontando e escolhendo: a peça selecionada, o que está
 * sob o cursor e o que um clique faz. Toda a política de interação fica aqui;
 * quem faz o picking (ver {@link IsometricChess}) só informa o que o raio
 * atingiu, e quem desenha (ver {@link ChessScene}) só lê o resultado.
 */
final class ChessInteraction {

    private final ChessGame game;
    private ChessPiece selectedPiece;
    private ChessPiece hoveredPiece;
    private BoardTile hoveredTile;

    ChessInteraction(ChessGame game) {
        this.game = game;
    }

    /**
     * Informa o que está sob o cursor neste frame: a peça atingida e, se
     * nenhuma, a casa do tabuleiro (ambos podem ser null). A peça tem
     * prioridade: com uma peça sob o cursor, a casa apontada é a dela, e não a
     * que o raio atinge atrás, para o caminho até uma captura aparecer.
     */
    void hover(ChessPiece piece, BoardTile boardTile) {
        hoveredPiece = piece;
        hoveredTile = piece != null ? piece.tile() : boardTile;
    }

    /**
     * Um clique no que está sob o cursor. Com uma peça selecionada, primeiro
     * tenta movê-la para a casa apontada (andar ou capturar). Se não é um
     * movimento válido, seleciona a peça apontada: troca a seleção, ou a
     * desfaz quando é a mesma peça ou nenhuma.
     */
    void click() {
        if (selectedPiece != null && game.move(selectedPiece, hoveredTile)) {
            selectedPiece = null;
            return;
        }
        selectedPiece = hoveredPiece != null && hoveredPiece != selectedPiece ? hoveredPiece : null;
    }

    Optional<ChessPiece> selectedPiece() {
        return Optional.ofNullable(selectedPiece);
    }

    Optional<ChessPiece> hoveredPiece() {
        return Optional.ofNullable(hoveredPiece);
    }

    /**
     * As casas a destacar: a origem da peça selecionada e o caminho até a casa
     * sob o cursor, se ela é um destino válido. Vazio caso contrário.
     */
    List<BoardTile> highlightedTiles() {
        if (selectedPiece == null || hoveredTile == null) {
            return List.of();
        }
        List<BoardTile> path = game.pathTo(selectedPiece, hoveredTile);
        if (path.isEmpty()) {
            return List.of();
        }
        List<BoardTile> tiles = new ArrayList<>();
        tiles.add(selectedPiece.tile());
        tiles.addAll(path);
        return tiles;
    }
}
