package com.farias.rengine.io;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.glfw.GLFWVidMode;

public class Window {
	private long id;
	private int width;
	private int height;
	private boolean fullscreen;
	
	public Window(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void bind() {
		glfwMakeContextCurrent(id);		
	}

	public long create() {
		// init GLFW
		if (!glfwInit()) {
			throw new IllegalStateException("Failed to initialize GLFW");
		}
		
		// creating window
		this.id = glfwCreateWindow(width, height, "",
				fullscreen ? glfwGetPrimaryMonitor() : 0, 0);
		if (this.id == 0) {
			throw new IllegalStateException("Failed to create window");
		}
		
		if (!fullscreen) {
			//moves window to the center
			GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			glfwSetWindowPos(id,
					(videoMode.width() - width) / 2,
					(videoMode.height() - height) / 2);
			
			glfwShowWindow(id);
		}
		
		return this.id;
	}
	
	public void setFullscreen(boolean fullscreen) {
		this.fullscreen = fullscreen;
	}
	
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(id);
	}

	public void swapBuffers() {
		// ajuda a controlar a taxa de frames por segundo
		// 0 não limita o fps
		// 1 mesmo fps que a taxa de atualização do monitor
		// 2 metade do fps da taxa de atualização do monitor ex: 60hz -> 30fps
		// deixei 0 para saber o impacto na performance durante o desenvolvimento
		glfwSwapInterval(0);
		glfwSwapBuffers(id);
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setTitle(String title) {
		glfwSetWindowTitle(id, title);
	}
}
