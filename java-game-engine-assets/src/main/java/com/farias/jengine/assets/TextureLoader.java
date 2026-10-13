package com.farias.jengine.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.Texture;
import com.farias.jengine.renderer.TextureData;

/**
 * Decodifica uma imagem (STB) e a sobe como Texture. Detalhe interno de
 * {@link Assets}: ninguem mais carrega textura direto do disco.
 */
final class TextureLoader {

    private TextureLoader() {
    }

    static Texture load(Renderer renderer, InputStream in, String name) throws IOException {
        byte[] bytes = in.readAllBytes();
        // STB so le de buffers diretos.
        ByteBuffer encoded = BufferUtils.createByteBuffer(bytes.length);
        encoded.put(bytes).flip();

        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];

        ByteBuffer pixels = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
        if (pixels == null) {
            throw new IOException(
                    "Failed to load texture: " + name + " - " + STBImage.stbi_failure_reason());
        }

        try {
            return renderer.createTexture(new TextureData(width[0], height[0], pixels));
        } finally {
            STBImage.stbi_image_free(pixels);
        }
    }
}
