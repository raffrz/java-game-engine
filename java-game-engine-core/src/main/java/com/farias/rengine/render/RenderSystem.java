package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.farias.rengine.Game;
import com.farias.rengine.System;

public class RenderSystem extends System {

	RenderSystemOptions renderSystemOptions;

	public RenderSystem(Game g, RenderSystemOptions options) {
		super(g);
		this.renderSystemOptions = options;
	}
	
	public RenderSystem(Game g) {
		this(g, new RenderSystemOptions());
	}

	public static RenderSystem renderSystemDefault(Game g) {
		return new RenderSystem(g);
	}

	public static RenderSystem renderSystem2D(Game g) {
		RenderSystemOptions ro = new RenderSystemOptions();
		ro.setEnableDepthTest(false);
		return new RenderSystem(g, ro);
	}

	public void init() {
		GL.createCapabilities();
		// Ativactes culling
		glEnable(GL_CULL_FACE);
		// Enables backface culling to hide back faces of transparent models
		glCullFace(GL_BACK);
		glFrontFace(GL_CCW);
		//Activates the depth test to draw elements far away behind near elements
		if (this.renderSystemOptions.isEnableDepthTest()) {
			glEnable(GL11.GL_DEPTH_TEST);
		}
		// Enable transparency
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_2D);
	}

	@Override
	public void update(float deltaTime) {
		//clears all the pixel colors to black you can use glClearColor to change the
		// clear color
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	}

	public static class RenderSystemOptions {
		private boolean enableDepthTest = true;

		public boolean isEnableDepthTest() {
			return enableDepthTest;
		}

		public void setEnableDepthTest(boolean enableDepthTest) {
			this.enableDepthTest = enableDepthTest;
		}
	}

}
