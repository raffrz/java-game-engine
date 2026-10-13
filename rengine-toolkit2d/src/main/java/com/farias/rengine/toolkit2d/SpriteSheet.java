package com.farias.rengine.toolkit2d;

import java.util.Objects;

import com.farias.rengine.renderer.Texture;

/**
 * Uma textura dividida em tiles de tamanho fixo (tileW x tileH, em pixels da
 * textura). Valor puro: o tile desenhado e escolhido a cada chamada de
 * desenho, nao ha estado de "tile atual".
 */
public record SpriteSheet(Texture texture, int tileW, int tileH) {

    public SpriteSheet {
        Objects.requireNonNull(texture, "texture");
        if (tileW <= 0 || tileH <= 0) {
            throw new IllegalArgumentException("tile size must be positive: " + tileW + "x" + tileH);
        }
    }
}
