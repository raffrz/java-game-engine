package com.farias.rengine.engine3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f scale = new Vector3f(1, 1, 1);
    public Vector3f rotation = new Vector3f(0, 0, 0);

    private boolean useLookAt = false;
    private final Vector3f target = new Vector3f();
    private final Vector3f up = new Vector3f(0, 1, 0);

    public Camera() {
    }

    public Camera(float x, float y, float z) {
        this.position.set(x, y, z);
    }

    public Matrix4f getViewMatrix() {
        if (useLookAt) {
            return new Matrix4f().lookAt(
                    position,
                    target,
                    up);
        }
        return new Matrix4f()
                .rotate((float) Math.toRadians(rotation.x), 1, 0, 0)
                .rotate((float) Math.toRadians(rotation.y), 0, 1, 0)
                .rotate((float) Math.toRadians(rotation.z), 0, 0, 1)
                .translate(position.x, position.y, position.z);
    }

    public void setPosition(int x, int y, int z) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }

    public void setPosition(int x, int y) {
        this.position.x = x;
        this.position.y = y;
    }

    public void rotate(float x, float y) {
        this.rotation.x = x;
        this.rotation.y = y;
    }

    public void setZoom(float zoom) {
        this.scale.x = zoom;
        this.scale.y = zoom;
    }

    public void pitch(float angle) {
        this.rotation.x = angle;
    }

    public void roll(float angle) {
        this.rotation.y = angle;
    }

    public void lookAt(float eyeX, float eyeY, float eyeZ,
            float targetX, float targetY, float targetZ,
            float upX, float upY, float upZ) {
        position.set(eyeX, eyeY, eyeZ);
        target.set(targetX, targetY, targetZ);
        up.set(upX, upY, upZ);
        useLookAt = true;
    }

    public void disableLookAt() {
        useLookAt = false;
    }
}
