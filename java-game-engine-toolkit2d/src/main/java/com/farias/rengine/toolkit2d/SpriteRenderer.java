package com.farias.rengine.toolkit2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;

/**
 * Primitiva 2D de baixo nivel: transforma "tile de uma folha em (x, y, w, h)"
 * em um RenderCommand. Compartilhada por todos os Canvas e Ui.
 *
 * <p>Convencao exposta: origem no canto superior esquerdo, X para a direita,
 * Y para baixo (valores positivos), rotacao em graus no sentido horario. A
 * projecao ({@link #orthographic}) continua a "Y negativo para baixo" do
 * motor; a inversao e feita aqui, ao montar a matriz, e nao vaza para quem
 * desenha.
 *
 * <p>Alocacao: um Material por (textura, tile) e um pool de Matrix4f
 * reaproveitado a cada frame. O RenderCommand guarda a matriz por referencia
 * ate o endFrame, por isso o pool so reinicia quando o frame do Renderer muda
 * (e nao a cada begin de Canvas/Ui, que compartilham este objeto).
 */
public final class SpriteRenderer {
    private final Renderer renderer;
    private final Shader shader;
    private final Mesh quad;

    private final Map<MaterialKey, Material> materials = new HashMap<>();
    private final List<Matrix4f> matrixPool = new ArrayList<>();
    private int matricesUsed;
    private long matrixPoolFrame = -1;

    private final Vector2f uvOffset = new Vector2f();
    private final Vector2f uvScale = new Vector2f();

    SpriteRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.createShader(SpriteShaders.SPRITE_VERTEX, SpriteShaders.SPRITE_FRAGMENT);
        this.quad = renderer.createMesh(quadData());
    }

    /**
     * Projecao ortografica 2D de "tela" para uma area virtual width x height.
     * Funcao pura: cada chamada devolve uma matriz nova.
     */
    public static Matrix4f orthographic(float width, float height) {
        return new Matrix4f().setOrtho(0, width, -height, 0, 1000, -1000);
    }

    /**
     * Desenha o tile da folha no retangulo (x, y, width, height), onde x/y e o
     * canto superior esquerdo. rotationDegrees gira em torno do centro, no
     * sentido horario.
     */
    public void sprite(RenderPassNode pass, SpriteSheet sheet, int tile, float x, float y, float width,
            float height, float rotationDegrees) {
        Matrix4f model = nextMatrix()
                .translation(x + width / 2f, -y - height / 2f, 0)
                .rotateZ((float) Math.toRadians(-rotationDegrees))
                .scale(width / 2f, height / 2f, 1f);
        pass.submit(new RenderCommand(quad, material(sheet, tile), model));
    }

    /**
     * Texto alinhado a esquerda a partir de x. size e a altura do glifo; a
     * largura segue a proporcao do glifo da fonte.
     */
    public void text(RenderPassNode pass, BitmapFont font, String text, float x, float y, float size) {
        float glyphWidth = glyphWidth(font, size);
        SpriteSheet sheet = font.sheet();
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            sprite(pass, sheet, text.charAt(i), cursor, y, glyphWidth, size, 0f);
            cursor += glyphWidth;
        }
    }

    /** Texto centrado em x (mesma regra do BitmapFont legado). */
    public void textCentered(RenderPassNode pass, BitmapFont font, String text, float x, float y, float size) {
        float glyphWidth = glyphWidth(font, size);
        text(pass, font, text, x - text.length() * glyphWidth / 2f, y, size);
    }

    /** Esvazia o cache de materiais (nao libera GPU: pertence ao Renderer). */
    void clear() {
        materials.clear();
    }

    private static float glyphWidth(BitmapFont font, float size) {
        return size * font.glyphW() / font.glyphH();
    }

    private Matrix4f nextMatrix() {
        long frame = renderer.frameIndex();
        if (frame != matrixPoolFrame) {
            matrixPoolFrame = frame;
            matricesUsed = 0;
        }
        if (matricesUsed == matrixPool.size()) {
            matrixPool.add(new Matrix4f());
        }
        return matrixPool.get(matricesUsed++);
    }

    private Material material(SpriteSheet sheet, int tile) {
        MaterialKey key = new MaterialKey(sheet.texture(), sheet.tileW(), sheet.tileH(), tile);
        Material cached = materials.get(key);
        if (cached != null) {
            return cached;
        }
        computeTileUV(sheet.texture(), tile, sheet.tileW(), sheet.tileH(), uvOffset, uvScale);
        Material material = renderer.createMaterial(shader, sheet.texture());
        material.setVector2f("uvOffset", uvOffset);
        material.setVector2f("uvScale", uvScale);
        materials.put(key, material);
        return material;
    }

    private static void computeTileUV(Texture texture, int tile, float tileWidth, float tileHeight,
            Vector2f outOffset, Vector2f outScale) {
        float hTiles = texture.getWidth() / tileWidth;
        float vTiles = texture.getHeight() / tileHeight;
        float sx = 1f / hTiles;
        float sy = 1f / vTiles;
        int tx = (int) (tile % hTiles);
        int ty = (int) (tile / hTiles);
        outScale.set(sx, sy);
        outOffset.set(sx * tx, sy * ty);
    }

    private static MeshData quadData() {
        float[] vertices = {
                -1f, 1f, 0f,
                1f, 1f, 0f,
                1f, -1f, 0f,
                -1f, -1f, 0f,
        };
        float[] texCoords = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f,
        };
        float[] normals = {
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
        };
        int[] indices = { 0, 3, 2, 2, 1, 0 };
        return new MeshData(vertices, texCoords, normals, indices);
    }

    private record MaterialKey(Texture texture, int tileW, int tileH, int tile) {
    }
}
