package com.farias.jengine.examples.chess.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * As regras da partida: recebe o que o jogador fez ({@link PlayerInput}) e o
 * tempo passado, e atualiza o {@link ChessState}. O padrão de movimento de cada
 * peça fica nela mesma ({@link ChessPiece#getValidMoves}); aqui fica o que
 * depende do jogo inteiro: de quem é a vez, o que um clique faz, validar e
 * aplicar o lance (que não pode deixar o próprio rei em xeque), xeque,
 * xeque-mate e afogamento, o que destacar e as animações das peças. Não sabe
 * de renderização, janela nem engine.
 *
 * <p>
 * As animações não bloqueiam o jogo: um lance é aplicado na hora, e a pose da
 * peça (ver {@link PiecePose}) só acompanha depois.
 */
public final class ChessLogic {

    /** Quanto a peça selecionada sobe, e a que velocidade. */
    private static final float LIFT_HEIGHT = 0.5f;
    private static final float LIFT_SPEED = 3f;
    /** Giro da peça selecionada, em graus por segundo. */
    private static final float SPIN_SPEED = 90f;
    /** Velocidade da peça indo para a casa nova depois de um lance, em unidades por segundo. */
    private static final float MOVE_SPEED = 5f;

    /** Altura em que a peça capturada atravessa o tabuleiro. */
    private static final float CAPTURE_HEIGHT = 1.5f;
    /** Velocidade da animação de captura, em unidades por segundo. */
    private static final float CAPTURE_SPEED = 4f;
    /** Distância da primeira fileira de capturadas à última fileira do tabuleiro. */
    private static final float CAPTURED_ROW_DISTANCE = 1.5f;
    private static final float CAPTURED_ROW_SPACING = 1f;

    /**
     * Um passo do jogo: registra a casa apontada, trata o clique, recalcula o
     * destaque e avança as animações em {@code deltaTime} segundos.
     */
    public void update(ChessState state, PlayerInput input, float deltaTime) {
        state.pointedTile = input.pointedTile();
        if (input.clicked() && !state.status.isOver()) {
            click(state);
        }
        state.highlightedTiles = highlightedTiles(state);
        animate(state, deltaTime);
    }

    /**
     * Com uma peça selecionada, primeiro tenta movê-la para a casa apontada
     * (andar ou capturar); se moveu, passa a vez e vê se o adversário ficou em
     * xeque, xeque-mate ou afogado. Se não é um movimento
     * válido, seleciona a peça apontada quando ela é de quem tem a vez: troca
     * a seleção, ou a desfaz quando é a mesma peça, do adversário ou nenhuma.
     */
    private void click(ChessState state) {
        if (state.selectedPiece != null && move(state, state.selectedPiece, state.pointedTile)) {
            state.selectedPiece = null;
            state.turn = state.turn.opponent();
            state.status = status(state.board, state.turn);
            return;
        }
        ChessPiece pointed = state.pointedPiece().orElse(null);
        boolean selectable = pointed != null && pointed.side() == state.turn && pointed != state.selectedPiece;
        state.selectedPiece = selectable ? pointed : null;
    }

    /**
     * Move {@code piece} para {@code target}, capturando a peça que estiver
     * lá, e registra o lance. Não faz nada se não é um lance legal (ver
     * {@link #legalPath}).
     *
     * @return se a peça foi movida
     */
    private boolean move(ChessState state, ChessPiece piece, BoardTile target) {
        if (target == null) {
            return false;
        }
        List<BoardTile> path = legalPath(state.board, piece, target);
        if (path.isEmpty()) {
            return false;
        }
        BoardTile from = piece.tile();
        Optional<ChessPiece> captured = state.board.move(piece, target);
        captured.ifPresent(capturedPiece -> startCapture(state, capturedPiece));
        state.history.add(new Move(piece, from, target, path, captured));
        return true;
    }

    private List<BoardTile> highlightedTiles(ChessState state) {
        if (state.selectedPiece == null || state.pointedTile == null) {
            return List.of();
        }
        List<BoardTile> path = legalPath(state.board, state.selectedPiece, state.pointedTile);
        if (path.isEmpty()) {
            return List.of();
        }
        List<BoardTile> tiles = new ArrayList<>();
        tiles.add(state.selectedPiece.tile());
        tiles.addAll(path);
        return tiles;
    }

    // ---- xeque ----

    /**
     * O caminho de {@code piece} até {@code target} (ver
     * {@link ChessPiece#getPathToTarget}), ou vazio se o movimento não vale:
     * fora do padrão da peça ou deixando o próprio rei em xeque.
     */
    private static List<BoardTile> legalPath(ChessBoard board, ChessPiece piece, BoardTile target) {
        List<BoardTile> path = piece.getPathToTarget(board, target);
        if (path.isEmpty() || leavesKingInCheck(board, piece, target)) {
            return List.of();
        }
        return path;
    }

    private static boolean leavesKingInCheck(ChessBoard board, ChessPiece piece, BoardTile target) {
        return board.testMove(piece, target, after -> isInCheck(after, piece.side()));
    }

    /** Como a partida fica para {@code side}, que tem a vez. */
    private static GameStatus status(ChessBoard board, Side side) {
        boolean check = isInCheck(board, side);
        if (hasLegalMove(board, side)) {
            return check ? GameStatus.CHECK : GameStatus.PLAYING;
        }
        return check ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
    }

    /**
     * Se o rei de {@code side} está atacado: alguma peça adversária tem a casa
     * dele entre os movimentos (o peão só conta a diagonal, que é onde ele
     * captura).
     */
    private static boolean isInCheck(ChessBoard board, Side side) {
        Optional<BoardTile> king = board.pieces().stream()
                .filter(piece -> piece.side() == side && piece.type() == PieceType.KING)
                .map(ChessPiece::tile)
                .findFirst();
        return king.isPresent() && board.pieces().stream()
                .filter(piece -> piece.side() != side)
                .anyMatch(piece -> piece.getValidMoves(board).contains(king.get()));
    }

    private static boolean hasLegalMove(ChessBoard board, Side side) {
        // cópia: testar um lance tira e devolve peças da lista do tabuleiro
        for (ChessPiece piece : List.copyOf(board.pieces())) {
            if (piece.side() != side) {
                continue;
            }
            for (BoardTile target : piece.getValidMoves(board)) {
                if (!leavesKingInCheck(board, piece, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- animações ----

    /**
     * Começa a levar {@code captured} para fora do tabuleiro, atrás da última
     * fileira de quem capturou, em fileiras de {@code SIZE} peças ao longo de
     * x: sobe, atravessa e desce (ver {@link #animateCapture}).
     */
    private void startCapture(ChessState state, ChessPiece captured) {
        Side capturer = captured.side().opponent();
        int slot = state.captured(captured.side()).indexOf(captured);
        float distance = CAPTURED_ROW_DISTANCE + (slot / ChessBoard.SIZE) * CAPTURED_ROW_SPACING;

        PiecePose pose = state.pose(captured);
        pose.restX = slot % ChessBoard.SIZE;
        // "atrás" da última fileira é o sentido contrário ao da frente
        pose.restZ = capturer.homeRow() - capturer.forward() * distance;
        pose.spin = 0f;
        pose.capturePhase = PiecePose.CapturePhase.RISING;
    }

    private void animate(ChessState state, float deltaTime) {
        for (Map.Entry<ChessPiece, PiecePose> entry : state.poses.entrySet()) {
            ChessPiece piece = entry.getKey();
            PiecePose pose = entry.getValue();
            if (pose.captured()) {
                animateCapture(pose, deltaTime);
            } else {
                animateInPlay(pose, piece, piece == state.selectedPiece, deltaTime);
            }
        }
    }

    /**
     * Peça em jogo: desliza em linha reta até a sua casa (depois de um lance,
     * a casa muda na hora e a pose vai atrás) e, selecionada, sobe e gira; sem
     * seleção, desce e volta ao giro zero.
     */
    private static void animateInPlay(PiecePose pose, ChessPiece piece, boolean selected, float deltaTime) {
        moveTowardXZ(pose, piece.tile().x(), piece.tile().z(), MOVE_SPEED * deltaTime);
        pose.y = moveToward(pose.y, selected ? LIFT_HEIGHT : 0f, LIFT_SPEED * deltaTime);
        pose.spin = selected ? (pose.spin + SPIN_SPEED * deltaTime) % 360f : 0f;
    }

    /**
     * Peça capturada: sobe até {@code CAPTURE_HEIGHT}, atravessa na
     * horizontal até o lugar dela fora do tabuleiro e desce. Cada fase começa
     * quando a anterior termina; depois fica parada.
     */
    private static void animateCapture(PiecePose pose, float deltaTime) {
        float step = CAPTURE_SPEED * deltaTime;
        switch (pose.capturePhase) {
            case RISING -> {
                pose.y = moveToward(pose.y, CAPTURE_HEIGHT, step);
                if (pose.y == CAPTURE_HEIGHT) {
                    pose.capturePhase = PiecePose.CapturePhase.TRAVELING;
                }
            }
            case TRAVELING -> {
                if (moveTowardXZ(pose, pose.restX, pose.restZ, step)) {
                    pose.capturePhase = PiecePose.CapturePhase.LANDING;
                }
            }
            case LANDING -> {
                pose.y = moveToward(pose.y, 0f, step);
                if (pose.y == 0f) {
                    pose.capturePhase = PiecePose.CapturePhase.RESTING;
                }
            }
            case RESTING -> {
            }
        }
    }

    /**
     * Anda a pose no plano XZ, em linha reta, até ({@code x}, {@code z}),
     * no máximo {@code maxStep}.
     *
     * @return se chegou
     */
    private static boolean moveTowardXZ(PiecePose pose, float x, float z, float maxStep) {
        float dx = x - pose.x;
        float dz = z - pose.z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance <= maxStep) {
            pose.x = x;
            pose.z = z;
            return true;
        }
        pose.x += dx / distance * maxStep;
        pose.z += dz / distance * maxStep;
        return false;
    }

    /** {@code current} andando até {@code target}, no máximo {@code maxStep}. */
    private static float moveToward(float current, float target, float maxStep) {
        return current + Math.max(-maxStep, Math.min(maxStep, target - current));
    }
}
