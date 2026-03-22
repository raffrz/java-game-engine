package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.stb.STBImage.stbi_load;

import java.nio.ByteBuffer;

public class Texture {
	
	private int id;
	
	private int width;
	
	private int height;
	
    public Texture(String filename) {
        int[] width = new int[1];
        int[] height = new int[1];
        int[] nrChannels = new int[1];

        //TODO create a ResourceManager class
        ByteBuffer pixels = stbi_load(filename, width, height, nrChannels, 0);
        this.width = width[0];
        this.height = height[0];

        pixels.flip();

        id = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, id);

        //GL_NEAREST OR GL_LINEAR
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.width, this.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    }
	
	public void bind(int sampler) {
		// opengl has about 32 different samples we can use
		if (sampler >= 0 && sampler <= 31) {
			glActiveTexture(GL_TEXTURE0 + sampler);
			glBindTexture(GL_TEXTURE_2D, id);
		}
	}
	
	public int getWidth() {
		return width;
	}
	
    public int getHeight() {
        return height;
    }
    
    public float[] createTexCoords(int tile, float width, float height) {
		float hTiles = this.getWidth() / width;
		float vTiles = this.getHeight() / height;
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
