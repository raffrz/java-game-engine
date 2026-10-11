package com.farias.rengine.toolkit2d.legacy;

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
import com.farias.rengine.assets.Assets;
import com.farias.rengine.toolkit2d.draw.SpriteShaders;

/**
 * Helper de instancia (nao singleton) para desenhar sprites 2D/texto/tiles.
 * Todos compartilham o mesmo quad unitario; o recorte de uma sub-regiao da
 * textura (tile de spritesheet) e feito via uniforms uvOffset/uvScale do
 * Material, em vez de reescrever o buffer de UV do mesh a cada troca de tile.
 */
public final class SpriteBatch2D {
    private final Renderer renderer;
    private final Assets assets;
    private final Shader shader;
    private final Mesh quad;

    public SpriteBatch2D(Renderer renderer, Assets assets) {
        this.renderer = renderer;
        this.assets = assets;
        this.shader = renderer.createShader(SpriteShaders.SPRITE_VERTEX, SpriteShaders.SPRITE_FRAGMENT);
        this.quad = renderer.createMesh(quadData());
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

    public Sprite createSprite(String file, int tile, float tileWidth, float tileHeight) {
        Texture texture = assets.texture(file);
        Vector2f offset = new Vector2f();
        Vector2f scale = new Vector2f();
        computeTileUV(texture, tile, tileWidth, tileHeight, offset, scale);
        return new Sprite(texture, offset, scale);
    }

    public static void computeTileUV(Texture texture, int tile, float tileWidth, float tileHeight,
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

    public void draw(RenderPassNode pass, Sprite sprite, Matrix4f modelMatrix) {
        Material material = renderer.createMaterial(shader, sprite.getTexture());
        material.setVector2f("uvOffset", sprite.getUvOffset());
        material.setVector2f("uvScale", sprite.getUvScale());
        pass.submit(new RenderCommand(quad, material, modelMatrix));
    }

    public void draw(RenderPassNode pass, Sprite sprite, int tile, float tileWidth, float tileHeight,
            Matrix4f modelMatrix) {
        Vector2f offset = new Vector2f();
        Vector2f scale = new Vector2f();
        computeTileUV(sprite.getTexture(), tile, tileWidth, tileHeight, offset, scale);
        sprite.setUv(offset, scale);
        draw(pass, sprite, modelMatrix);
    }

    /**
     * Matriz de modelo para um retangulo de tela em (x, y) com o tamanho
     * dado, onde x/y e o canto superior esquerdo (convencao usada pelos
     * exemplos: y cresce para baixo). rotationDegrees roda em torno do
     * centro do retangulo.
     */
    public static Matrix4f rectTransform(float x, float y, float width, float height, float rotationDegrees) {
        Matrix4f translation = new Matrix4f().translation(x + width / 2f, y - height / 2f, 0);
        Matrix4f rotation = new Matrix4f().rotationZ((float) Math.toRadians(rotationDegrees));
        Matrix4f scale = new Matrix4f().scale(width / 2f, height / 2f, 1f);
        return translation.mul(scale.mul(rotation));
    }

    public static Matrix4f rectTransform(float x, float y, float width, float height) {
        return rectTransform(x, y, width, height, 0f);
    }
}
