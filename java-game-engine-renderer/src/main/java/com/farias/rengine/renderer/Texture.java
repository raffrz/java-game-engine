package com.farias.rengine.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Texture implements Disposable {

    private int id;

    private int width;

    private int height;

    private final boolean depthTexture;

    public Texture(TextureData data) {
        id = glGenTextures();
        width = data.width();
        height = data.height();
        depthTexture = false;

        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
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
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private Texture(int width, int height, boolean depthTexture) {
        this.id = glGenTextures();
        this.width = width;
        this.height = height;
        this.depthTexture = depthTexture;

        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[] { 1f, 1f, 1f, 1f });
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_DEPTH_COMPONENT,
            width,
            height,
            0,
            GL_DEPTH_COMPONENT,
            GL_FLOAT,
            (java.nio.ByteBuffer) null
        );
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static Texture createDepthTexture(int width, int height) {
        return new Texture(width, height, true);
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

    public void unbind(int sampler) {
        if (sampler < 0 || sampler > 31) {
            throw new IllegalArgumentException("Invalid sampler index");
        }
        glActiveTexture(GL_TEXTURE0 + sampler);
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

    int id() {
        return id;
    }

    boolean isDepthTexture() {
        return depthTexture;
    }
}
