package com.farias.jengine.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Material {
    private final Shader shader;

    private final Map<String, UniformValue> uniforms = new HashMap<>();
    private final Map<String, TextureBinding> textures = new HashMap<>();

    public Material(Shader shader) {
        this.shader = Objects.requireNonNull(shader, "shader");
    }

    public Material setInt(String name, int value) {
        uniforms.put(name, (s, n) -> s.setInt(n, value));
        return this;
    }

    public Material setFloat(String name, float value) {
        uniforms.put(name, (s, n) -> s.setFloat(n, value));
        return this;
    }

    public Material setVector3f(String name, Vector3f value) {
        Vector3f copy = new Vector3f(value);
        uniforms.put(name, (s, n) -> s.setVector3f(n, copy));
        return this;
    }

    public Material setVector2f(String name, Vector2f value) {
        Vector2f copy = new Vector2f(value);
        uniforms.put(name, (s, n) -> s.setVector2f(n, copy));
        return this;
    }

    public Material setMatrix4f(String name, Matrix4f value) {
        Matrix4f copy = new Matrix4f(value);
        uniforms.put(name, (s, n) -> s.setMatrix4f(n, copy));
        return this;
    }

    public Material setMatrix3f(String name, Matrix3f value) {
        Matrix3f copy = new Matrix3f(value);
        uniforms.put(name, (s, n) -> s.setMatrix3f(n, copy));
        return this;
    }

    public Material setTexture(String uniformName, Texture texture, int sampler) {
        textures.put(
                Objects.requireNonNull(uniformName, "uniformName"),
                new TextureBinding(Objects.requireNonNull(texture, "texture"), sampler));
        return this;
    }

    public void applyUniforms() {
        for (Map.Entry<String, UniformValue> entry : uniforms.entrySet()) {
            entry.getValue().apply(shader, entry.getKey());
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

    /** Um valor de uniform ja tipado: aplicar e uma chamada virtual, sem instanceof. */
    @FunctionalInterface
    private interface UniformValue {
        void apply(Shader shader, String name);
    }

    private record TextureBinding(Texture texture, int sampler) {
    }
}
