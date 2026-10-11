package com.farias.rengine.toolkit2d.legacy;

import org.joml.Vector2f;

import com.farias.rengine.renderer.Texture;

/**
 * Uma textura mais a sub-regiao de UV atualmente selecionada (para recorte de
 * spritesheet). O offset/scale e mutavel para permitir trocar de tile entre
 * desenhos sem precisar de uma nova textura ou mesh.
 */
public final class Sprite {
    private final Texture texture;
    private final Vector2f uvOffset;
    private final Vector2f uvScale;

    public Sprite(Texture texture, Vector2f uvOffset, Vector2f uvScale) {
        this.texture = texture;
        this.uvOffset = uvOffset;
        this.uvScale = uvScale;
    }

    public Texture getTexture() {
        return texture;
    }

    public Vector2f getUvOffset() {
        return uvOffset;
    }

    public Vector2f getUvScale() {
        return uvScale;
    }

    public void setUv(Vector2f offset, Vector2f scale) {
        this.uvOffset.set(offset);
        this.uvScale.set(scale);
    }
}
