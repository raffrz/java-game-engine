package com.farias.jengine.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Estado de pass e framebuffers de depth em OpenGL. Detalhe interno do
 * {@link Renderer}; hoje e o unico backend, entao nao ha interface.
 */
final class OpenGLBackend {

    private final Map<Texture, Integer> framebuffers = new HashMap<>();

    void beginPass(RenderPassDescriptor descriptor, RenderView view) {
        bindRenderTarget(descriptor);
        glViewport(0, 0, view.getWidth(), view.getHeight());

        if (descriptor.depthTest()) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }

        if (descriptor.cullFace()) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }

        if (descriptor.blend()) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        } else {
            glDisable(GL_BLEND);
        }

        ClearColor color = descriptor.clearColor();
        if (color != null) {
            glClearColor(
                    color.r(),
                    color.g(),
                    color.b(),
                    color.a());
        }

        int clearMask = 0;

        if (descriptor.clearColorBuffer()) {
            clearMask |= GL_COLOR_BUFFER_BIT;
        }

        if (descriptor.clearDepthBuffer()) {
            clearMask |= GL_DEPTH_BUFFER_BIT;
        }

        glClear(clearMask);
    }

    void endPass() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDrawBuffer(GL_BACK);
        glReadBuffer(GL_BACK);
    }

    /** Apaga o framebuffer criado para a textura, se houver. */
    void release(Texture depthTexture) {
        Integer framebuffer = framebuffers.remove(depthTexture);
        if (framebuffer != null) {
            glDeleteFramebuffers(framebuffer);
        }
    }

    void dispose() {
        for (int framebuffer : framebuffers.values()) {
            glDeleteFramebuffers(framebuffer);
        }
        framebuffers.clear();
    }

    private void bindRenderTarget(RenderPassDescriptor descriptor) {
        Texture depthAttachment = descriptor.depthAttachment();

        if (depthAttachment == null) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDrawBuffer(GL_BACK);
            glReadBuffer(GL_BACK);
            return;
        }

        int framebuffer = framebuffers.computeIfAbsent(depthAttachment, this::createDepthFramebuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
    }

    private int createDepthFramebuffer(Texture depthTexture) {
        int framebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_TEXTURE_2D,
                depthTexture.id(),
                0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete framebuffer for depth texture: " + status);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return framebuffer;
    }
}
