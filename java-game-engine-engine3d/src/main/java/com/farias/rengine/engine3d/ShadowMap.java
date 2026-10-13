package com.farias.rengine.engine3d;

import org.joml.Matrix4f;

import com.farias.rengine.renderer.DirectionalLight;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Texture;

/**
 * Sombra de uma luz direcional: o mapa de profundidade e tudo que o passe da
 * luz precisa. Criado por {@link Shadows#create}.
 *
 * <p>
 * Uso: crie o passe com {@link #descriptor()} e {@link #view()} <b>antes</b>
 * do passe da cena, submeta {@link #caster} para cada objeto que projeta
 * sombra, e use {@link LitMaterial#receiving} nos materiais que recebem.
 *
 * <pre>{@code
 * RenderPassNode pass = renderer.createPass(shadow.descriptor(), shadow.view());
 * models.forEach(model -> pass.submit(shadow.caster(model)));
 * }</pre>
 */
public final class ShadowMap {

    private static final float DEFAULT_BIAS = 0.004f;

    private final Texture texture;
    private final DirectionalLight light;
    private final RenderView view;
    private final RenderPassDescriptor descriptor;
    private final Material casterMaterial;
    private float bias = DEFAULT_BIAS;

    ShadowMap(Texture texture, DirectionalLight light, Material casterMaterial) {
        this.texture = texture;
        this.light = light;
        this.casterMaterial = casterMaterial;
        this.view = light.shadowView(texture);
        this.descriptor = new RenderPassDescriptor("shadow")
                .depthAttachment(texture)
                .clearColorBuffer(false)
                .clearDepthBuffer(true)
                .depthTest(true)
                .cullFace(false);
    }

    /** A luz que projeta a sombra (e que deve também iluminar quem a recebe). */
    public DirectionalLight light() {
        return light;
    }

    public Texture texture() {
        return texture;
    }

    /** Tolerância de profundidade contra o serrilhado da sombra (acne). */
    public float bias() {
        return bias;
    }

    public ShadowMap bias(float bias) {
        this.bias = bias;
        return this;
    }

    public RenderPassDescriptor descriptor() {
        return descriptor;
    }

    public RenderView view() {
        return view;
    }

    /** O comando que desenha {@code mesh} no passe da luz. */
    public RenderCommand caster(Mesh mesh, Matrix4f modelMatrix) {
        return new RenderCommand(mesh, casterMaterial, modelMatrix);
    }

    public RenderCommand caster(TexturedModel model) {
        return caster(model.getMesh(), model.getModelMatrix());
    }
}
