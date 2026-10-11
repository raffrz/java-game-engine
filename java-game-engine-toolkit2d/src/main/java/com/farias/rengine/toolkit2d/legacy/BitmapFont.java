package com.farias.rengine.toolkit2d.legacy;

import org.joml.Matrix4f;

import com.farias.rengine.renderer.RenderPassNode;

/**
 * Texto bitmap sobre o atlas fonts/default16x16.png (resolvido por Assets, via classpath) (um char = um tile 16x16),
 * desenhado com o mesmo quad/uniforms de um {@link SpriteBatch2D}.
 */
public final class BitmapFont {
    private static final String FONTS_FILE_NAME = "fonts/default16x16.png";
    private static final int GLYPH_SIZE = 16;

    private final SpriteBatch2D batch;
    private final Sprite atlas;

    public BitmapFont(SpriteBatch2D batch) {
        this.batch = batch;
        this.atlas = batch.createSprite(FONTS_FILE_NAME, 0, GLYPH_SIZE, GLYPH_SIZE);
    }

    public void drawText(RenderPassNode pass, String text, int x, int y, int width, int height) {
        int offset = -text.length() * width / 2;
        for (char c : text.toCharArray()) {
            drawChar(pass, c, x + offset, y, width, height);
            offset += width;
        }
    }

    public void drawChar(RenderPassNode pass, int c, int x, int y, int width, int height) {
        Matrix4f modelMatrix = SpriteBatch2D.rectTransform(x, y, width, height);
        batch.draw(pass, atlas, c, GLYPH_SIZE, GLYPH_SIZE, modelMatrix);
    }
}
