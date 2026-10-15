package com.farias.jengine.examples.chess;

import java.util.List;
import java.util.Optional;

import com.farias.jengine.examples.chess.pieces.ChessPiece;

/**
 * Estado do jogo: o {@link ChessBoard} e a peça e a casa selecionadas e sob o
 * cursor. Não depende de renderização, janela nem da engine; quem desenha o
 * jogo (ver {@code ChessScene}) lê este estado e mostra como quiser.
 */
public final class ChessGame {

    private final ChessBoard board = ChessBoard.initialPosition();
    private ChessPiece selectedPiece;
    private ChessPiece hoveredPiece;
    private BoardTile selectedTile;
    private BoardTile hoveredTile;

    public List<ChessPiece> pieces() {
        return board.pieces();
    }

    /**
     * Seleciona {@code piece}. Selecionar a peça que já está selecionada, ou
     * {@code null}, desfaz a seleção.
     */
    public void selectPiece(ChessPiece piece) {
        selectedPiece = piece != null && piece.equals(selectedPiece) ? null : piece;
    }

    public Optional<ChessPiece> selectedPiece() {
        return Optional.ofNullable(selectedPiece);
    }

    /**
     * Seleciona {@code tile}. Selecionar a casa que já está selecionada, ou
     * {@code null}, desfaz a seleção.
     */
    public void selectTile(BoardTile tile) {
        selectedTile = tile != null && tile.equals(selectedTile) ? null : tile;
    }

    public Optional<BoardTile> selectedTile() {
        return Optional.ofNullable(selectedTile);
    }

    /** Marca {@code piece} como a peça sob o cursor; {@code null} desfaz o hover. */
    public void hoverPiece(ChessPiece piece) {
        hoveredPiece = piece;
    }

    public Optional<ChessPiece> hoveredPiece() {
        return Optional.ofNullable(hoveredPiece);
    }

    /**
     * Marca {@code tile} como a casa sob o cursor; {@code null} desfaz o
     * hover. Só vale com uma peça selecionada: a casa sob o cursor é o destino
     * do movimento.
     */
    public void hoverTile(BoardTile tile) {
        hoveredTile = selectedPiece != null ? tile : null;
    }

    public Optional<BoardTile> hoveredTile() {
        return Optional.ofNullable(hoveredTile);
    }

    /**
     * O caminho da peça selecionada até a casa sob o cursor (ver
     * {@link ChessPiece#getPathToTarget}). Vazio sem peça selecionada, sem casa
     * sob o cursor ou se ela não é um destino válido.
     */
    public List<BoardTile> hoveredPath() {
        if (selectedPiece == null || hoveredTile == null) {
            return List.of();
        }
        return selectedPiece.getPathToTarget(board, hoveredTile);
    }

    /** As casas para onde {@code piece} pode ir no tabuleiro atual. */
    public List<BoardTile> validTiles(ChessPiece piece) {
        return piece.getValidMoves(board);
    }
}
