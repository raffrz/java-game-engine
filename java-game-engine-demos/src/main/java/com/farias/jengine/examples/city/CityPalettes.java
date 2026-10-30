package com.farias.jengine.examples.city;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.Texture;
import com.farias.jengine.renderer.TextureData;

/**
 * Cores por material, como texturas de uma linha (paletas). A coordenada U de cada
 * vertice (o atributo {@code tone} da geometria) escolhe a cor, entao um material pode
 * variar de quarteirao para quarteirao sem um material novo por predio.
 */
final class CityPalettes {

    /** Cores de cada material (RGB 0xRRGGBB); mais de uma cor = paleta sorteada por quarteirao. */
    private static final Map<String, int[]> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("wall", new int[] {0xD8CBB0, 0xC9CED3, 0xA9644B, 0xE6DFD0, 0x8E9AA6, 0xC7A57A, 0xB8B1A6, 0x9C8F82});
        COLORS.put("roof", new int[] {0x5A5F66, 0x70584B, 0x464B52, 0x7A6A5A});
        COLORS.put("metal", new int[] {0x8A9097});
        COLORS.put("window_lit", new int[] {0xFFD98A});
        COLORS.put("window_dark", new int[] {0x2B3A4A});
        COLORS.put("road", new int[] {0x34363A});
        COLORS.put("sidewalk", new int[] {0x9A9A94});
        COLORS.put("ground", new int[] {0x5D7A4E});
        COLORS.put("grass", new int[] {0x6FA356});
        COLORS.put("trunk", new int[] {0x6B4A2F});
        COLORS.put("leaves", new int[] {0x2F6B35, 0x3C7D3A, 0x2A5E36});
        COLORS.put("trim", new int[] {0xE9E4DA});
        COLORS.put("door", new int[] {0x5A3A24, 0x2F4F6B, 0x7A2E2E});
        COLORS.put("brick", new int[] {0x8C4B3A});
        COLORS.put("shopfront", new int[] {0xF2D9A0});
        COLORS.put("fence", new int[] {0xF1EEE6});
        COLORS.put("lamp", new int[] {0xFFF1C2});
        COLORS.put("marking", new int[] {0xE8E6D0});
        COLORS.put("car_body", new int[] {0xC0392B, 0x2E6DA4, 0xE5B80B, 0xECECEC, 0x2C2C2C, 0x3A8D5A, 0xD9731A, 0x7D4E9E});
        COLORS.put("car_glass", new int[] {0x1E2A33});
        COLORS.put("tire", new int[] {0x151515});
        COLORS.put("concrete", new int[] {0xA7A59E, 0x8F8D88, 0xB5B2AA, 0x9A9892, 0x7F7E7A});
        COLORS.put("plaza", new int[] {0xBDB9AF});
        COLORS.put("awning", new int[] {0xB23A3A, 0x2F6B8F, 0x2F7D4F, 0xD9A21B});
        COLORS.put("foliage", new int[] {0x5E9E3F, 0x7BB04A, 0x4C8A3A, 0x9CB84A, 0x6FA356, 0xC9A23A});
        COLORS.put("water", new int[] {0x4A86B8});
    }

    private CityPalettes() {
    }

    static Map<String, Integer> sizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        COLORS.forEach((name, colors) -> sizes.put(name, colors.length));
        return sizes;
    }

    static Texture texture(Renderer renderer, String material) {
        int[] colors = COLORS.getOrDefault(material, new int[] {0xFF00FF});
        ByteBuffer pixels = ByteBuffer.allocateDirect(colors.length * 4);
        for (int rgb : colors) {
            pixels.put((byte) (rgb >> 16)).put((byte) (rgb >> 8)).put((byte) rgb).put((byte) 255);
        }
        pixels.flip();
        return renderer.createTexture(new TextureData(colors.length, 1, pixels));
    }
}
