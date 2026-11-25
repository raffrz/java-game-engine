package com.farias.jengine.toolkit3d;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.RenderCommand;
import com.farias.jengine.renderer.RenderPassDescriptor;
import com.farias.jengine.renderer.RenderView;
import com.farias.jengine.renderer.Texture;

/**
 * Sombra de uma luz direcional: o mapa de profundidade e tudo que o passe da
 * luz precisa. Criado por {@link Shadows#create}.
 *
 * <p>
 * Uso: crie o passe com {@link #descriptor()} e {@link #view()} <b>antes</b>
 * do passe da cena, submeta {@link #caster} para cada objeto que projeta
 * sombra ({@link #casters(SkinnedModel)} para modelos animados), e use
 * {@link LitMaterial#receiving} nos materiais que recebem.
 *
 * <pre>{@code
 * RenderPass pass = renderer.createPass(shadow.descriptor(), shadow.view());
 * models.forEach(model -> pass.submit(shadow.caster(model)));
 * }</pre>
 */
public final class ShadowMap {

    private final Texture texture;
    private final DirectionalLight light;
    private final RenderView view;
    private final RenderPassDescriptor descriptor;
    private final Material casterMaterial;
    private final Material skinnedCasterMaterial;
    private final float bias;

    ShadowMap(Texture texture, DirectionalLight light, Material casterMaterial, Material skinnedCasterMaterial,
            float bias) {
        this.texture = texture;
        this.light = light;
        this.casterMaterial = casterMaterial;
        this.skinnedCasterMaterial = skinnedCasterMaterial;
        this.bias = bias;
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

    /**
     * Os comandos que desenham {@code model} no passe da luz, um por parte,
     * na pose do último {@link SkinnedModel#update}: a sombra acompanha a
     * animação.
     */
    public List<RenderCommand> casters(SkinnedModel model) {
        Matrix4f modelMatrix = model.getModelMatrix();
        float[] palette = model.paletteSnapshot();
        List<RenderCommand> commands = new ArrayList<>(model.parts().size());
        for (SkinnedModel.Part part : model.parts()) {
            commands.add(new RenderCommand(part.mesh(), skinnedCasterMaterial, modelMatrix, palette));
        }
        return commands;
    }
}
