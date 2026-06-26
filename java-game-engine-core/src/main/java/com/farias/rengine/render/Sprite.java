package com.farias.rengine.render;

import org.joml.Matrix4f;

public class Sprite {
    private Model model;
    private Texture texture;
    private Shader shader;

    public static final float[] VERTICES = new float[] { 
        -1f, 1f, 0, // TOP LEFT 0
        1f, 1f, 0, // TOP RIGHT 1
        1f, -1f, 0, // BOTTOM RIGHT 2
        -1f, -1f, 0, // BOTTOM LEFT 3
    };

    // In Opengl the indices must be drawn in conter clock wise order
    public static final int[] INDICES = new int[] { 
        0, 3, 2,
        2, 1, 0 
    };

    public Sprite(Texture texture, Shader shader, Model model) {
        this.model = model;
        this.shader = shader;
        this.texture = texture;
    }

    public static Sprite createSprite(String file, int tile, float width, float height) {
        Texture texture = new Texture(file);
        Shader shader = new Shader("sprite_shader");
        float[] texCoords = createTexCoords(texture, tile, width, height);
        Model model = new Model(VERTICES, texCoords, INDICES);

        return new Sprite(texture, shader, model);
    }

    public void draw(Matrix4f projection) {
        this.shader.bind();
        this.texture.bind(0);
        this.shader.setUniform("sampler", 0);
        this.shader.setUniform("projection", projection);
        this.model.draw();
    }

    public void setTexCoords(float[] texCoords) {
        this.model.setTexCoords(texCoords);
    }

    public Texture getTexture() {
        return this.texture;
    }

    public static float[] createTexCoords(Texture t, int tile, float width, float height) {
        float hTiles = t.getWidth() / width;
        float vTiles = t.getHeight() / height;
        float sx = 1 / hTiles;
        float sy = 1 / vTiles;
        int tx = (int) (tile % hTiles);
        int ty = (int) (tile / hTiles);
        float[] texCoords = new float[] { 
            sx * tx, sy * ty,
            sx * tx + sx, sy * ty,
            sx * tx + sx, sy * ty + sy,
            sx * tx, sy * ty + sy 
        };
        return texCoords;
    }
}
