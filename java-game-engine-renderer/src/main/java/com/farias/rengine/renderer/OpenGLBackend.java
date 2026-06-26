package com.farias.rengine.renderer;

import static org.lwjgl.opengl.GL11.*;

public final class OpenGLBackend implements RenderBackend {

    @Override
    public void beginPass(RenderPassDescriptor descriptor) {

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

        ClearColor color = descriptor.clearColor();
        glClearColor(
                color.r(),
                color.g(),
                color.b(),
                color.a());

        int clearMask = 0;

        if (descriptor.clearColorBuffer()) {
            clearMask |= GL_COLOR_BUFFER_BIT;
        }

        if (descriptor.clearDepthBuffer()) {
            clearMask |= GL_DEPTH_BUFFER_BIT;
        }

        glClear(clearMask);
    }

    @Override
    public void endPass() {
    }
}
