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
        
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
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

	public void pollEvents() {
		glfwPollEvents();
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(id);
	}

	public void swapBuffers(boolean limitFPS) {
		// ajuda a controlar a taxa de frames por segundo
		// 0 n�o limita o fps
		// 1 mesmo fps que a taxa de atualiza��o do monitor
		// 2 metade do fps da taxa de atualiza��o do monitor ex: 60hz -> 30fps
		// deixei 0 para saber o impacto na performance durante o desenvolvimento
		glfwSwapInterval(limitFPS ? 1 : 0);
		glfwSwapBuffers(id);
	}
	public void swapBuffers() {
		swapBuffers(false);
	}
	
	public long getHandle() {
		return id;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * Destroi a janela nativa. Nao encerra o GLFW (isso e feito por quem
	 * criou a janela, ao fim do ciclo de vida).
	 */
	public void destroy() {
		if (id != 0) {
			glfwDestroyWindow(id);
			id = 0;
		}
	}

	public void setTitle(String title) {
		glfwSetWindowTitle(id, title);
	}

	/**
	 * Tamanho real do framebuffer em pixels. Em telas HiDPI (ex: Retina no
	 * macOS) isso difere do width/height logico passado ao construtor, e e o
	 * que deve ser usado para configurar o viewport do OpenGL.
	 */
	public int getFramebufferWidth() {
		int[] w = new int[1];
		int[] h = new int[1];
		glfwGetFramebufferSize(id, w, h);
		return w[0];
	}

	public int getFramebufferHeight() {
		int[] w = new int[1];
		int[] h = new int[1];
		glfwGetFramebufferSize(id, w, h);
		return h[0];
	}
}
