package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.farias.rengine.Game;
import com.farias.rengine.System;

public class RenderSystem implements System {

	RenderSystemOptions renderSystemOptions;

	public RenderSystem(RenderSystemOptions options) {
		this.renderSystemOptions = options;
	}
	
	public RenderSystem() {
		this(new RenderSystemOptions());
	}

	public static RenderSystem renderSystemDefault() {
		return new RenderSystem();
	}

	public static RenderSystem renderSystem2D() {
		RenderSystemOptions options = new RenderSystemOptions();
		options.setEnableDepthTest(false);
		return new RenderSystem(options);
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
	public void update(Game game, float deltaTime) {
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
