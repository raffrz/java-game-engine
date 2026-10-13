package com.farias.jengine.examples.chess;

import com.farias.jengine.toolkit3d.Picking;
import com.farias.jengine.toolkit3d.Ray;
import com.farias.jengine.toolkit3d.TexturedModel;

/** Como uma peça aparece: o modelo 3D e a animação de seleção. */
final class PieceView {

    private static final float PIECE_SCALE = 1.2f;
    private static final float PIECE_ROTATION_Y = 90f;

    private static final float LIFT_HEIGHT = 0.5f;
    private static final float LIFT_SPEED = 3f;
    private static final float SPIN_SPEED = 90f;

    private final ChessPiece piece;
    private final TexturedModel model;
    private final float baseRotationY;

    private float lift;
    private float spin;

    PieceView(ChessPiece piece, TexturedModel model) {
        this.piece = piece;
        this.model = model;
        this.baseRotationY = initialRotationY(piece);

        model.getPosition().set(piece.x(), 0f, piece.z());
        model.getScale().set(PIECE_SCALE);
        model.getRotation().set(0f, baseRotationY, 0f);
    }

    ChessPiece piece() {
        return piece;
    }

    TexturedModel model() {
        return model;
    }

    void update(boolean selected, float deltaTime) {
        float step = LIFT_SPEED * deltaTime;
        float targetLift = selected ? LIFT_HEIGHT : 0f;
        lift += Math.max(-step, Math.min(step, targetLift - lift));
        spin = selected ? (spin + SPIN_SPEED * deltaTime) % 360f : 0f;

        model.getPosition().y = lift;
        model.getRotation().y = baseRotationY + spin;
    }

    /** Distância ao longo do raio até esta peça, ou NaN se não acertou. */
    float hit(Ray ray) {
        return Picking.rayBounds(ray, model.getMesh().bounds(), model.getModelMatrix());
    }

    private static float initialRotationY(ChessPiece piece) {
        if (piece.type() != PieceType.KNIGHT) {
            return PIECE_ROTATION_Y;
        }
        return piece.side() == Side.WHITE ? -180f : 0f;
    }
}
