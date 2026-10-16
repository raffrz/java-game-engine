package com.farias.jengine.examples.chess.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tudo o que o jogo é num instante: o tabuleiro, a peça selecionada, a casa
 * apontada, as casas a destacar, a pose de cada peça (o resultado das
 * animações) e os lances já feitos. Só o {@link ChessLogic} altera (os campos
 * são package-private); quem desenha lê pelos métodos públicos.
 */
public final class ChessState {

    final ChessBoard board = ChessBoard.initialPosition();
    final Map<ChessPiece, PiecePose> poses = new HashMap<>();
    final List<Move> history = new ArrayList<>();
    ChessPiece selectedPiece;
    BoardTile pointedTile;
    List<BoardTile> highlightedTiles = List.of();

    public ChessState() {
        for (ChessPiece piece : board.pieces()) {
            poses.put(piece, new PiecePose(piece.tile()));
        }
    }

    /** As peças em jogo, sem as capturadas. */
    public List<ChessPiece> pieces() {
        return board.pieces();
    }

    /** Todas as peças da partida, inclusive as capturadas. */
    public List<ChessPiece> allPieces() {
        return List.copyOf(poses.keySet());
    }

    /** As peças de {@code side} capturadas até agora, na ordem da captura. */
    public List<ChessPiece> captured(Side side) {
        return board.captured(side);
    }

    /** Onde {@code piece} está desenhada agora (ver {@link PiecePose}). */
    public PiecePose pose(ChessPiece piece) {
        return poses.get(piece);
    }

    public Optional<ChessPiece> selectedPiece() {
        return Optional.ofNullable(selectedPiece);
    }

    /** A casa sob o cursor, ou vazio fora do tabuleiro. */
    public Optional<BoardTile> pointedTile() {
        return Optional.ofNullable(pointedTile);
    }

    /** A peça em jogo na casa sob o cursor, se houver. */
    public Optional<ChessPiece> pointedPiece() {
        return pointedTile().flatMap(board::pieceAt);
    }

    /**
     * As casas a destacar: a origem da peça selecionada e o caminho até a casa
     * apontada, quando ela é um destino válido. Vazio caso contrário.
     */
    public List<BoardTile> highlightedTiles() {
        return highlightedTiles;
    }

    /** Os lances feitos na partida, em ordem. */
    public List<Move> history() {
        return Collections.unmodifiableList(history);
    }
}
