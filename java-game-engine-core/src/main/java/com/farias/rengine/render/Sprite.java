package com.farias.rengine.render;

import org.joml.Vector2f;

import com.farias.rengine.renderer.Texture;

/**
 * A textured quad. The UV sub-rect (offset/scale) is mutable so the same
 * sprite can be re-sliced into a different tile of a spritesheet between
 * draw calls (see GameEngine#drawSprite tile overloads).
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
