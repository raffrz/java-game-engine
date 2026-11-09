package com.farias.jengine.examples.chess.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * O tabuleiro 8x8, as peças sobre ele e as já capturadas, com x e z de 0 a 7.
 * Onde cada lado começa e para onde avança está em {@link Side}. Todo
 * movimento passa por {@link #move}.
 */
public final class ChessBoard {

    public static final int SIZE = 8;

    private static final PieceType[] HOME_ROW = {
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
            PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
    };

    private final List<ChessPiece> pieces = new ArrayList<>();
    private final List<ChessPiece> capturedWhites = new ArrayList<>();
    private final List<ChessPiece> capturedBlacks = new ArrayList<>();

    /** Um tabuleiro na posição inicial. */
    public static ChessBoard initialPosition() {
        ChessBoard board = new ChessBoard();
        for (Side side : Side.values()) {
            for (int x = 0; x < SIZE; x++) {
                board.pieces.add(ChessPiece.of(HOME_ROW[x], side, new BoardTile(x, side.homeRow())));
                board.pieces.add(ChessPiece.of(PieceType.PAWN, side, new BoardTile(x, side.pawnRow())));
            }
        }
        return board;
    }

    /**
     * Põe {@code piece} em {@code target}, capturando a peça que estiver lá.
     * Não confere se o movimento é válido (ver {@link ChessPiece#getValidMoves});
     * só garante que duas peças nunca dividem uma casa. Package-private: de
     * fora do pacote, o movimento passa pelo {@link ChessLogic}, que valida.
     *
     * @return a peça capturada, ou vazio se a casa estava livre
     */
    Optional<ChessPiece> move(ChessPiece piece, BoardTile target) {
        Optional<ChessPiece> captured = pieceAt(target);
        captured.ifPresent(this::capturePiece);
        piece.moveTo(target);
        return captured;
    }

    /**
     * Responde {@code question} sobre o tabuleiro com {@code piece} em
     * {@code target} (tirando a peça que estiver lá) e desfaz o movimento:
     * para as regras testarem um lance sem jogá-lo. Nada é registrado como
     * capturado.
     */
    boolean testMove(ChessPiece piece, BoardTile target, Predicate<ChessBoard> question) {
        BoardTile from = piece.tile();
        Optional<ChessPiece> captured = pieceAt(target);
        int capturedIndex = captured.map(pieces::indexOf).orElse(-1);
        captured.ifPresent(pieces::remove);
        piece.moveTo(target);
        try {
            return question.test(this);
        } finally {
            piece.moveTo(from);
            captured.ifPresent(capturedPiece -> pieces.add(capturedIndex, capturedPiece));
        }
    }

    /** As peças de {@code side} capturadas até agora, na ordem da captura. */
    public List<ChessPiece> captured(Side side) {
        return Collections.unmodifiableList(side == Side.BLACK ? capturedBlacks : capturedWhites);
    }

    private void capturePiece(ChessPiece piece) {
        pieces.remove(piece);
        if (piece.side() == Side.BLACK) {
            capturedBlacks.add(piece);
        } else {
            capturedWhites.add(piece);
        }
    }

    public static boolean contains(int x, int z) {
        return x >= 0 && x < SIZE && z >= 0 && z < SIZE;
    }

    public static boolean contains(BoardTile tile) {
        return contains(tile.x(), tile.z());
    }

    /** A peça em {@code tile}, ou vazio se a casa está livre. */
    public Optional<ChessPiece> pieceAt(BoardTile tile) {
        return pieces.stream().filter(piece -> piece.tile().equals(tile)).findFirst();
    }

    /** A peça na casa (x, z), ou vazio se a casa está livre. */
    public Optional<ChessPiece> pieceAt(int x, int z) {
        return pieceAt(new BoardTile(x, z));
    }

    public List<ChessPiece> pieces() {
        return Collections.unmodifiableList(pieces);
    }
}
