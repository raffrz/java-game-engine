package com.farias.jengine.examples.chess;

import org.joml.Vector3f;

import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.PieceType;
import com.farias.jengine.examples.chess.model.Side;
import com.farias.jengine.toolkit3d.Picking;
import com.farias.jengine.toolkit3d.Ray;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * Como uma peça aparece: o modelo 3D, a animação de seleção e a de captura
 * (ver {@link #capture}).
 */
final class PieceView {

    private static final float PIECE_SCALE = 1.2f;
    private static final float PIECE_ROTATION_Y = 90f;

    private static final float LIFT_HEIGHT = 0.5f;
    private static final float LIFT_SPEED = 3f;
    private static final float SPIN_SPEED = 90f;

    /** Altura em que a peça capturada atravessa o tabuleiro. */
    private static final float CAPTURE_HEIGHT = 1.5f;
    /** Velocidade da animação de captura, em unidades por segundo. */
    private static final float CAPTURE_SPEED = 12f;

    /** As fases da animação de captura, nesta ordem. */
    private enum CapturePhase {
        RISING, TRAVELING, LANDING, RESTING
    }

    private final ChessPiece piece;
    private final TexturedModel model;
    private final float baseRotationY;

    private float lift;
    private float spin;

    /** Fase da animação de captura; null enquanto a peça está em jogo. */
    private CapturePhase capturePhase;
    private final Vector3f captureTarget = new Vector3f();

    PieceView(ChessPiece piece, TexturedModel model) {
        this.piece = piece;
        this.model = model;
        this.baseRotationY = initialRotationY(piece);

        model.getPosition().set(piece.tile().x(), 0f, piece.tile().z());
        model.getScale().set(PIECE_SCALE);
        model.getRotation().set(0f, baseRotationY, 0f);
    }

    ChessPiece piece() {
        return piece;
    }

    TexturedModel model() {
        return model;
    }

    boolean isCaptured() {
        return capturePhase != null;
    }

    /**
     * Começa a animação de captura até {@code restingPlace}, fora do
     * tabuleiro: a peça sobe até {@code CAPTURE_HEIGHT}, atravessa na
     * horizontal e desce. Depois fica parada lá, e {@link #update} não a
     * devolve mais à casa.
     */
    void capture(Vector3f restingPlace) {
        captureTarget.set(restingPlace);
        capturePhase = CapturePhase.RISING;
        spin = 0f;
    }

    void update(boolean selected, float deltaTime) {
        if (isCaptured()) {
            animateCapture(deltaTime);
            return;
        }
        float targetLift = selected ? LIFT_HEIGHT : 0f;
        lift = moveToward(lift, targetLift, LIFT_SPEED * deltaTime);
        spin = selected ? (spin + SPIN_SPEED * deltaTime) % 360f : 0f;

        // a peça pode ter mudado de casa (ChessPiece.moveTo)
        model.getPosition().set(piece.tile().x(), lift, piece.tile().z());
        model.getRotation().y = baseRotationY + spin;
    }

    /**
     * Atualiza o brilho especular para a câmera atual. Sem isso o reflexo
     * continua calculado para a direção de quando a peça foi criada.
     */
    void viewFrom(Vector3f directionToEye) {
        model.lit().viewDirection(directionToEye);
    }

    /** Distância ao longo do raio até esta peça, ou NaN se não acertou. */
    float hit(Ray ray) {
        return Picking.rayBounds(ray, model.getMesh().bounds(), model.getModelMatrix());
    }

    private void animateCapture(float deltaTime) {
        Vector3f position = model.getPosition();
        float step = CAPTURE_SPEED * deltaTime;
        switch (capturePhase) {
            case RISING -> {
                position.y = moveToward(position.y, CAPTURE_HEIGHT, step);
                if (position.y == CAPTURE_HEIGHT) {
                    capturePhase = CapturePhase.TRAVELING;
                }
            }
            case TRAVELING -> {
                float dx = captureTarget.x - position.x;
                float dz = captureTarget.z - position.z;
                float distance = (float) Math.sqrt(dx * dx + dz * dz);
                if (distance <= step) {
                    position.x = captureTarget.x;
                    position.z = captureTarget.z;
                    capturePhase = CapturePhase.LANDING;
                } else {
                    position.x += dx / distance * step;
                    position.z += dz / distance * step;
                }
            }
            case LANDING -> {
                position.y = moveToward(position.y, captureTarget.y, step);
                if (position.y == captureTarget.y) {
                    capturePhase = CapturePhase.RESTING;
                }
            }
            case RESTING -> {
            }
        }
        model.getRotation().y = baseRotationY;
    }

    /** {@code current} andando até {@code target}, no máximo {@code maxStep}. */
    private static float moveToward(float current, float target, float maxStep) {
        return current + Math.max(-maxStep, Math.min(maxStep, target - current));
    }

    private static float initialRotationY(ChessPiece piece) {
        if (piece.type() != PieceType.KNIGHT) {
            return PIECE_ROTATION_Y;
        }
        return piece.side() == Side.WHITE ? -180f : 0f;
    }
}
