package com.farias.jengine.platform.glfw;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.glfw.GLFWVidMode;

import com.farias.jengine.platform.Window;

/**
 * Janela GLFW. Ainda pede um contexto OpenGL 3.3 core: parametrizar isso so
 * faz sentido quando houver outro backend de renderizacao.
 */
final class GlfwWindow implements Window {
    private final int width;
    private final int height;
    private long id;

    GlfwWindow(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void create() {
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        id = glfwCreateWindow(width, height, "", 0, 0);
        if (id == 0) {
            throw new IllegalStateException("Failed to create window");
        }

        // centraliza a janela no monitor primario
        GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(id,
                (videoMode.width() - width) / 2,
                (videoMode.height() - height) / 2);

        glfwShowWindow(id);
    }

    /** Torna o contexto da janela corrente na thread atual. */
    void bind() {
        glfwMakeContextCurrent(id);
        setVSync(true);
    }

    /** Destroi a janela nativa. Nao encerra o GLFW (quem a criou faz isso). */
    void destroy() {
        if (id != 0) {
            glfwDestroyWindow(id);
            id = 0;
        }
    }

    long handle() {
        return id;
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(id);
    }

    @Override
    public void swapBuffers() {
        glfwSwapBuffers(id);
    }

    @Override
    public void setVSync(boolean enabled) {
        glfwSwapInterval(enabled ? 1 : 0);
    }

    // Consultados a cada chamada: a janela pode ser redimensionada.
    @Override
    public int getWidth() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetWindowSize(id, w, h);
        return w[0];
    }

    @Override
    public int getHeight() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetWindowSize(id, w, h);
        return h[0];
    }

    @Override
    public void setTitle(String title) {
        glfwSetWindowTitle(id, title);
    }

    @Override
    public int getFramebufferWidth() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(id, w, h);
        return w[0];
    }

    @Override
    public int getFramebufferHeight() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(id, w, h);
        return h[0];
    }
}
