package com.farias.rengine.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Material {
    private final Shader shader;

    private final Map<String, Object> uniforms = new HashMap<>();
    private final Map<String, TextureBinding> textures = new HashMap<>();

    public Material(Shader shader) {
        this.shader = Objects.requireNonNull(shader, "shader");
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

    public void setVector2f(String name, Vector2f value) {
        uniforms.put(name, new Vector2f(value));
    }

    public void setMatrix4f(String name, Matrix4f value) {
        uniforms.put(name, new Matrix4f(value));
    }

    public void setMatrix3f(String name, Matrix3f value) {
        uniforms.put(name, new Matrix3f(value));
    }

    public void setTexture(String uniformName, Texture texture, int sampler) {
        textures.put(
                Objects.requireNonNull(uniformName, "uniformName"),
                new TextureBinding(Objects.requireNonNull(texture, "texture"), sampler));
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
            } else if (value instanceof Vector2f v) {
                shader.setVector2f(name, v);
            } else if (value instanceof Matrix4f m) {
                shader.setMatrix4f(name, m);
            } else if (value instanceof Matrix3f m) {
                shader.setMatrix3f(name, m);
            } else {
                throw new IllegalArgumentException("Unsupported uniform type: " + value.getClass());
            }
        }

        for (Map.Entry<String, TextureBinding> entry : textures.entrySet()) {
            shader.setInt(entry.getKey(), entry.getValue().sampler());
        }
    }

    public void bind() {
        shader.bind();
        for (TextureBinding binding : textures.values()) {
            binding.texture().bind(binding.sampler());
        }
    }

    public void unbind() {
        for (TextureBinding binding : textures.values()) {
            binding.texture().unbind(binding.sampler());
        }
        shader.unbind();
    }

    public Shader shader() {
        return shader;
    }

    private record TextureBinding(Texture texture, int sampler) {
    }
}
