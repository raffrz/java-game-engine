package com.farias.jengine.examples.chess;

import java.util.Collection;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.jengine.platform.Window;
import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.RenderCommand;
import com.farias.jengine.renderer.RenderPass;
import com.farias.jengine.renderer.RenderPassDescriptor;
import com.farias.jengine.renderer.RenderView;
import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.Texture;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.ShapeData;
import com.farias.jengine.toolkit3d.Shaders;
import com.farias.jengine.toolkit3d.TexturedModel;

/**
 * Contorno branco em volta de um ou mais modelos, em dois passes:
 * <ol>
 * <li><b>máscara</b>: só os modelos, desenhados numa textura de profundidade do
 * tamanho do framebuffer, com a mesma câmera da cena;</li>
 * <li><b>contorno</b>: um quad de tela cheia por cima da cena, que pinta os
 * pixels vazios da máscara que têm o modelo a até {@code THICKNESS} pixels
 * (ver {@link OutlineShaders}).</li>
 * </ol>
 *
 * <p>
 * A espessura é constante na tela e não depende das normais nem do winding do
 * modelo. O contorno não é ocultado por objetos na frente do modelo: a
 * profundidade da cena não é amostrável, e para destacar a peça sob
 * o cursor isso é desejável.
 */
final class ModelOutline {

    /** Espessura do contorno, em pixels lógicos da janela. */
    private static final float THICKNESS = 3f;
    private static final Vector3f COLOR = new Vector3f(1f, 1f, 1f);

    private final Renderer renderer;
    private final Window window;
    private final IsometricCamera camera;

    private final Material maskMaterial;
    private final Material outlineMaterial;
    private final Mesh screenQuad;
    private final Matrix4f identity = new Matrix4f();

    private final RenderPassDescriptor maskDescriptor = new RenderPassDescriptor("outline-mask")
            .clearColorBuffer(false)
            .clearDepthBuffer(true)
            .depthTest(true)
            .cullFace(false);
    private final RenderPassDescriptor outlineDescriptor = new RenderPassDescriptor("outline")
            .clearColorBuffer(false)
            .clearDepthBuffer(false)
            .depthTest(false)
            .cullFace(false)
            .blend(true);

    private Texture mask;

    ModelOutline(Renderer renderer, Window window, IsometricCamera camera) {
        this.renderer = renderer;
        this.window = window;
        this.camera = camera;
        this.maskMaterial = renderer.createMaterial(
                renderer.createShader(Shaders.SHADOW_DEPTH_VERTEX, Shaders.SHADOW_DEPTH_FRAGMENT));
        this.outlineMaterial = renderer.createMaterial(
                renderer.createShader(OutlineShaders.VERTEX, OutlineShaders.FRAGMENT))
                .setVector3f("color", COLOR);
        // 2x2 centrado na origem: com view e projeção identidade, cobre a tela.
        this.screenQuad = renderer.createMesh(ShapeData.plane(2f, 2f));
    }

    /**
     * Cria os passes do contorno de {@code models} neste frame; sem modelos,
     * não cria nada. Chame depois do passe da cena (o contorno é desenhado por
     * cima) e antes da UI. Modelos que se encostam na tela ganham um contorno
     * só, em volta dos dois.
     */
    void draw(Collection<TexturedModel> models) {
        if (models.isEmpty()) {
            return;
        }
        int width = window.getFramebufferWidth();
        int height = window.getFramebufferHeight();
        if (width <= 0 || height <= 0) {
            return; // janela minimizada
        }
        resizeMask(width, height);

        RenderPass maskPass = renderer.createPass(maskDescriptor, camera.view(window));
        for (TexturedModel model : models) {
            maskPass.submit(new RenderCommand(model.getMesh(), maskMaterial, model.getModelMatrix()));
        }

        RenderPass outlinePass = renderer.createPass(outlineDescriptor,
                new RenderView(width, height, identity, identity));
        outlinePass.submit(new RenderCommand(screenQuad, outlineMaterial, identity));
    }

    /** A máscara acompanha o framebuffer: é recriada quando a janela muda de tamanho. */
    private void resizeMask(int width, int height) {
        if (mask != null && mask.getWidth() == width && mask.getHeight() == height) {
            return;
        }
        if (mask != null) {
            renderer.destroy(mask);
        }
        mask = renderer.createDepthTexture(width, height);
        maskDescriptor.depthAttachment(mask);

        float pixelScale = (float) width / Math.max(window.getWidth(), 1);
        outlineMaterial
                .setTexture("mask", mask, 0)
                .setVector2f("texelSize", new Vector2f(1f / width, 1f / height))
                .setFloat("thickness", THICKNESS * pixelScale);
    }
}
