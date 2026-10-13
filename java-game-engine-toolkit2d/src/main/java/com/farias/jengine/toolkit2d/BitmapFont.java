package com.farias.jengine.toolkit2d;

import java.util.Objects;

import com.farias.jengine.renderer.Texture;

/**
 * Fonte bitmap: um atlas em que o codigo do caractere e o indice do tile
 * (glyphW x glyphH). Valor puro, sem I/O; o atlas vem de Assets.
 */
public record BitmapFont(Texture atlas, int glyphW, int glyphH) {

    public BitmapFont {
        Objects.requireNonNull(atlas, "atlas");
        if (glyphW <= 0 || glyphH <= 0) {
            throw new IllegalArgumentException("glyph size must be positive: " + glyphW + "x" + glyphH);
        }
    }

    public SpriteSheet sheet() {
        return new SpriteSheet(atlas, glyphW, glyphH);
    }
}
