package com.farias.jengine.examples.city;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.RenderView;

/**
 * Camera de perspectiva que orbita um ponto: {@code yaw} em volta do eixo Y, {@code pitch} acima do plano do
 * chao e {@code distance} ate o alvo. O alvo anda no plano do chao ({@link #pan}), relativo ao yaw atual.
 */
final class OrbitCamera {

    private static final float FOV_DEGREES = 55f;
    private static final float NEAR = 2f;
    private static final float FAR = 1500f;
    private static final float MIN_PITCH = 6f;
    private static final float MAX_PITCH = 88f;
    private static final float MIN_DISTANCE = 20f;
    private static final float MAX_DISTANCE = 800f;

    private final Vector3f target = new Vector3f();
    private float yaw;
    private float pitch;
    private float distance;

    OrbitCamera(float yaw, float pitch, float distance) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
    }

    Vector3f target() {
        return target;
    }

    float yaw() {
        return yaw;
    }

    float distance() {
        return distance;
    }

    void rotate(float deltaYaw, float deltaPitch) {
        yaw = ((yaw + deltaYaw) % 360f + 360f) % 360f;
        pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch + deltaPitch));
    }

    /** Aproxima (scroll positivo) ou afasta, proporcional a distancia atual. */
    void zoom(float notches) {
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance * (float) Math.pow(0.9, notches)));
    }

    /** Move o alvo no chao: {@code forward} para onde a camera olha, {@code right} para a direita da tela. */
    void pan(float forward, float right) {
        double radians = Math.toRadians(yaw);
        float sin = (float) Math.sin(radians);
        float cos = (float) Math.cos(radians);
        target.x += -sin * forward + cos * right;
        target.z += -cos * forward - sin * right;
    }

    Vector3f eye() {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        return new Vector3f(
                (float) (Math.cos(pitchRadians) * Math.sin(yawRadians)),
                (float) Math.sin(pitchRadians),
                (float) (Math.cos(pitchRadians) * Math.cos(yawRadians))).mul(distance).add(target);
    }

    RenderView view(Window window) {
        // janela minimizada tem altura 0
        float aspect = (float) window.getWidth() / Math.max(window.getHeight(), 1);
        Matrix4f projection = new Matrix4f().setPerspective((float) Math.toRadians(FOV_DEGREES), aspect, NEAR, FAR);
        Vector3f eye = eye();
        Matrix4f view = new Matrix4f().lookAt(eye.x, eye.y, eye.z, target.x, target.y, target.z, 0f, 1f, 0f);
        return new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(), projection, view);
    }
}
