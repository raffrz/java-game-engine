package com.farias.rengine.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Texture implements Disposable {

    private int id;

    private int width;

    private int height;

    public Texture(TextureData data) {
        id = glGenTextures();
        width = data.width();
        height = data.height();

        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        data.pixels().flip();
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            data.width(),
            data.height(),
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            data.pixels()
        );
    }
    
    public void bind(int sampler) {
        if (sampler < 0 || sampler > 31) {
            throw new IllegalArgumentException("Invalid sampler index");
        }
        glActiveTexture(GL_TEXTURE0 + sampler);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void dispose() {
        if (id != 0) {
            glDeleteTextures(id);
            id = 0;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
