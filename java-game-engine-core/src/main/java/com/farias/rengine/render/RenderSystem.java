package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import com.farias.rengine.Game;
import com.farias.rengine.System;

public class RenderSystem extends System {
	
	public RenderSystem(Game g) {
		super(g);
	}

	public void init() {
		GL.createCapabilities();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_TEXTURE_2D);
	}

	public void update(float deltaTime) {
		//clears all the pixel colors to black you can use glClearColor to change the
		// clear color
		glEnable(GL11.GL_DEPTH_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

	}

}
