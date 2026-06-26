package com.farias.rengine.renderer;

import static org.lwjgl.opengl.GL11.*;

public class ForwardRenderPass /*extends RenderPass*/ {
    // private final float[] clearColor;

    // public ForwardRenderPass(float r, float g, float b, float a) {
    //     this.clearColor = new float[]{r, g, b, a};
    // }

    // @Override
    // public void begin() {
    //     // Define as configurações específicas deste passe
    //     glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
    //     glEnable(GL_DEPTH_TEST);
    //     glEnable(GL_CULL_FACE);
    //     glCullFace(GL_BACK);
        
    //     // Limpa apenas o que este passe precisa
    //     glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    // }

    // @Override
    // public void end() {
    //     // Se tivéssemos usado um Framebuffer customizado, faríamos o unbind aqui
    // }
}