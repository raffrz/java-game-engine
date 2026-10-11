package com.farias.rengine.toolkit2d.legacy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.joml.Matrix4f;

import com.farias.rengine.renderer.RenderPassNode;

/**
 * Texto bitmap sobre o atlas default16x16.png (um char = um tile 16x16),
 * desenhado com o mesmo quad/uniforms de um {@link SpriteBatch2D}.
 */
public final class BitmapFont {
    private static final String FONTS_FILE_NAME = "default16x16.png";
    private static final int GLYPH_SIZE = 16;

    private final SpriteBatch2D batch;
    private final Sprite atlas;

    public BitmapFont(SpriteBatch2D batch) {
        this.batch = batch;
        try {
            extractFontFile();
        } catch (IOException e) {
            throw new RuntimeException("falha ao gravar o arquivo de fontes", e);
        }
        this.atlas = batch.createSprite(FONTS_FILE_NAME, 0, GLYPH_SIZE, GLYPH_SIZE);
    }

    private static void extractFontFile() throws IOException {
        try (InputStream in = BitmapFont.class.getClassLoader().getResourceAsStream("fonts/" + FONTS_FILE_NAME)) {
            if (in == null) {
                throw new IOException("fonts/" + FONTS_FILE_NAME + " nao encontrado no classpath");
            }
            Files.copy(in, new File(FONTS_FILE_NAME).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
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
