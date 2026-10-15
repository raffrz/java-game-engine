package com.farias.jengine.examples.chess.pieces;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.farias.jengine.examples.chess.BoardTile;
import com.farias.jengine.examples.chess.ChessBoard;

/**
 * Uma peça e a casa onde ela está. Cada tipo de peça é uma subclasse que sabe
 * como se move ({@link #getValidMoves} e {@link #getPathToTarget}).
 *
 * <p>
 * A igualdade é por identidade: duas peças do mesmo tipo e lado são peças
 * diferentes, e a peça continua a mesma quando muda de casa.
 */
public abstract sealed class ChessPiece permits Pawn, Rook, Knight, Bishop, Queen, King {

    /** Direções (dx, dz) na horizontal e na vertical. */
    protected static final int[][] ORTHOGONAL = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    /** Direções (dx, dz) nas diagonais. */
    protected static final int[][] DIAGONAL = { { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };
    /** As oito direções: {@link #ORTHOGONAL} mais {@link #DIAGONAL}. */
    protected static final int[][] ALL_DIRECTIONS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
            { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

    private final PieceType type;
    private final Side side;
    private final BoardTile tile;

    protected ChessPiece(PieceType type, Side side, BoardTile tile) {
        this.type = type;
        this.side = side;
        this.tile = tile;
    }

    /** Cria a peça de {@code type}. */
    public static ChessPiece of(PieceType type, Side side, BoardTile tile) {
        return switch (type) {
            case PAWN -> new Pawn(side, tile);
            case ROOK -> new Rook(side, tile);
            case KNIGHT -> new Knight(side, tile);
            case BISHOP -> new Bishop(side, tile);
            case QUEEN -> new Queen(side, tile);
            case KING -> new King(side, tile);
        };
    }

    public PieceType type() {
        return type;
    }

    public Side side() {
        return side;
    }

    public BoardTile tile() {
        return tile;
    }

    /**
     * As casas para onde esta peça pode ir em {@code board}, considerando as
     * outras peças (bloqueio e captura).
     */
    public abstract List<BoardTile> getValidMoves(ChessBoard board);

    /**
     * As casas que a peça percorre até {@code target}, em ordem, da primeira
     * depois da casa atual até {@code target} inclusive. Vazio se
     * {@code target} não é um movimento válido em {@code board}.
     */
    public final List<BoardTile> getPathToTarget(ChessBoard board, BoardTile target) {
        if (!getValidMoves(board).contains(target)) {
            return List.of();
        }
        return pathTo(target);
    }

    /**
     * Movimentos de quem desliza (torre, bispo, dama): em cada direção, anda
     * casa a casa até sair do tabuleiro ou bater numa peça. A casa de uma peça
     * adversária entra (captura); a de uma peça do mesmo lado, não.
     */
    protected final List<BoardTile> slide(ChessBoard board, int[][] directions) {
        List<BoardTile> moves = new ArrayList<>();
        for (int[] direction : directions) {
            int x = tile.x() + direction[0];
            int z = tile.z() + direction[1];
            while (ChessBoard.contains(x, z)) {
                Optional<ChessPiece> occupant = board.pieceAt(x, z);
                if (occupant.isPresent()) {
                    if (occupant.get().side != side) {
                        moves.add(new BoardTile(x, z));
                    }
                    break;
                }
                moves.add(new BoardTile(x, z));
                x += direction[0];
                z += direction[1];
            }
        }
        return moves;
    }

    /**
     * Movimentos de um passo só (rei, cavalo): cada casa a {@code offsets} de
     * distância que está no tabuleiro e não tem peça do mesmo lado.
     */
    protected final List<BoardTile> step(ChessBoard board, int[][] offsets) {
        List<BoardTile> moves = new ArrayList<>();
        for (int[] offset : offsets) {
            int x = tile.x() + offset[0];
            int z = tile.z() + offset[1];
            if (ChessBoard.contains(x, z) && board.pieceAt(x, z).filter(p -> p.side == side).isEmpty()) {
                moves.add(new BoardTile(x, z));
            }
        }
        return moves;
    }

    /**
     * O caminho até {@code target}, que já é um movimento válido. Por padrão,
     * uma linha reta (horizontal, vertical ou diagonal), que serve para todas
     * as peças menos o cavalo.
     */
    protected List<BoardTile> pathTo(BoardTile target) {
        int dx = Integer.signum(target.x() - tile.x());
        int dz = Integer.signum(target.z() - tile.z());

        List<BoardTile> path = new ArrayList<>();
        BoardTile current = tile;
        while (!current.equals(target)) {
            current = new BoardTile(current.x() + dx, current.z() + dz);
            path.add(current);
        }
        return path;
    }
}
