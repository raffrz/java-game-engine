package com.farias.rengine.assets;

import java.nio.ByteBuffer;

import org.lwjgl.stb.STBImage;

import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Texture;
import com.farias.rengine.renderer.TextureData;

public final class TextureLoader {

    private TextureLoader() {
    }

    public static Texture load(Renderer renderer, String filename) {
        int[] width = new int[1];
        int[] height = new int[1];
        int[] channels = new int[1];

        ByteBuffer pixels = STBImage.stbi_load(filename, width, height, channels, 4);
        if (pixels == null) {
            throw new RuntimeException(
                    "Failed to load texture: " + filename + " - " + STBImage.stbi_failure_reason());
        }

        try {
            return renderer.createTexture(new TextureData(width[0], height[0], pixels));
        } finally {
            STBImage.stbi_image_free(pixels);
        }
    }
}
