package com.farias.rengine.renderer;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Material {
    private Shader shader;
    private Texture texture;

    private final Map<String, Object> uniforms = new HashMap<>();

    public Material(Shader shader, Texture texture) {
        this.shader = shader;
        this.texture = texture;
    }

    public void setInt(String name, int value) {
        uniforms.put(name, value);
    }

    public void setFloat(String name, float value) {
        uniforms.put(name, value);
    }

    public void setVector3f(String name, Vector3f value) {
        uniforms.put(name, new Vector3f(value));
    }

    public void setMatrix4f(String name, Matrix4f value) {
        uniforms.put(name, new Matrix4f(value));
    }

    public void setMatrix3f(String name, Matrix3f value) {
        uniforms.put(name, new Matrix3f(value));
    }

    public void applyUniforms() {
        for (Map.Entry<String, Object> entry : uniforms.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Integer i) {
                shader.setInt(name, i);
            } else if (value instanceof Float f) {
                shader.setFloat(name, f);
            } else if (value instanceof Vector3f v) {
                shader.setVector3f(name, v);
            } else if (value instanceof Matrix4f m) {
                shader.setMatrix4f(name, m);
            } else if (value instanceof Matrix3f m) {
                shader.setMatrix3f(name, m);
            } else {
                throw new IllegalArgumentException("Unsupported uniform type: " + value.getClass());
            }
        }
    }

    public void bind() {
        shader.bind();
        if (texture != null) {
            texture.bind(0);
        }
    }

    public void unbind() {
        shader.unbind();
        if (texture != null) {
            texture.unbind();
        }
    }

    public Shader shader() {
        return shader;
    }

    public Texture texture() {
        return texture;
    }
}
