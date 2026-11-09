package com.farias.jengine.toolkit3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.farias.jengine.animation.Pose;
import com.farias.jengine.animation.Skeleton;
import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.RenderCommand;

/**
 * Um modelo deformado por esqueleto: uma ou mais partes (mesh com skin +
 * material, tipicamente de {@link LitMaterials#createSkinned}) que
 * compartilham um {@link Skeleton}, com posição, rotação e escala no mundo
 * como em {@link TexturedModel}.
 *
 * <p>
 * A pose vem de fora: a cada frame o jogo calcula uma {@link Pose} (com um
 * {@code Animator}, IK ou à mão) e chama {@link #update(Pose)}; os comandos
 * criados depois disso desenham essa pose.
 *
 * <pre>{@code
 * animator.update(deltaTime);
 * model.update(animator.pose());
 * shadow.casters(model).forEach(shadowPass::submit);
 * model.commands().forEach(scenePass::submit);
 * }</pre>
 */
public final class SkinnedModel {

    /** Uma malha com skin e o material com que é desenhada. */
    public record Part(Mesh mesh, Material material) {
        public Part {
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(material, "material");
        }
    }

    private final List<Part> parts;
    private final Skeleton skeleton;
    private final float[] palette;
    private final Vector3f position = new Vector3f();
    private final Vector3f rotation = new Vector3f();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    /**
     * Começa na pose de repouso do esqueleto.
     *
     * @throws IllegalArgumentException sem partes, com uma malha sem skin ou
     *                                  com mais de {@link Shaders#MAX_JOINTS}
     *                                  juntas
     */
    public SkinnedModel(List<Part> parts, Skeleton skeleton) {
        this.parts = List.copyOf(parts);
        this.skeleton = Objects.requireNonNull(skeleton, "skeleton");
        if (this.parts.isEmpty()) {
            throw new IllegalArgumentException("a skinned model needs at least one part");
        }
        for (Part part : this.parts) {
            if (!part.mesh().skinned()) {
                throw new IllegalArgumentException("mesh has no skin (joints/weights): use a TexturedModel");
            }
        }
        if (skeleton.jointCount() > Shaders.MAX_JOINTS) {
            throw new IllegalArgumentException("skeleton has " + skeleton.jointCount() + " joints; the skinned shaders support "
                    + Shaders.MAX_JOINTS);
        }
        this.palette = new float[Pose.FLOATS_PER_JOINT * skeleton.jointCount()];
        update(new Pose(skeleton));
    }

    /** Um modelo de uma parte só. */
    public SkinnedModel(Mesh mesh, Material material, Skeleton skeleton) {
        this(List.of(new Part(mesh, material)), skeleton);
    }

    /**
     * Passa a desenhar {@code pose}: calcula as matrizes de skinning. A pose
     * não é guardada; pode ser reaproveitada e mexida depois desta chamada.
     */
    public void update(Pose pose) {
        if (pose.skeleton() != skeleton) {
            throw new IllegalArgumentException("pose belongs to another skeleton");
        }
        pose.skinningPalette(palette);
    }

    /**
     * Os comandos que desenham o modelo na pose do último {@link #update},
     * um por parte. Matriz de modelo e palette são cópias: os comandos podem
     * ser guardados até o {@code endFrame} mesmo que o modelo mude antes.
     */
    public List<RenderCommand> commands() {
        Matrix4f modelMatrix = getModelMatrix();
        float[] snapshot = paletteSnapshot();
        List<RenderCommand> commands = new ArrayList<>(parts.size());
        for (Part part : parts) {
            commands.add(new RenderCommand(part.mesh(), part.material(), modelMatrix, snapshot));
        }
        return commands;
    }

    /** Matriz de modelo nova, T * R * S, com a mesma convenção de {@link TexturedModel}. */
    public Matrix4f getModelMatrix() {
        return new Matrix4f().translation(position)
                .rotateXYZ((float) Math.toRadians(rotation.x), (float) Math.toRadians(rotation.y),
                        (float) Math.toRadians(rotation.z))
                .scale(scale);
    }

    /**
     * Converte um ponto do mundo para o espaço do modelo (o dos solvers de
     * IK), pela matriz de modelo atual.
     */
    public Vector3f toModelSpace(Vector3f world, Vector3f out) {
        return getModelMatrix().invert().transformPosition(world, out);
    }

    /**
     * Posição no mundo da junta na pose dada (por exemplo, para prender uma
     * espada na mão). Usa a {@code pose.modelSpace} já calculada, então chame
     * depois de {@code pose.computeModelSpace()} ou de {@link #update}.
     */
    public Vector3f jointWorldPosition(Pose pose, int joint, Vector3f out) {
        pose.modelSpace(joint).getTranslation(out);
        return getModelMatrix().transformPosition(out);
    }

    /**
     * Matriz da junta no mundo (posição e orientação), na pose dada; ver
     * {@link #jointWorldPosition}.
     */
    public Matrix4f jointWorldMatrix(Pose pose, int joint, Matrix4f out) {
        return getModelMatrix().mul(pose.modelSpace(joint), out);
    }

    /** Cópia do palette atual, para comandos que vivem até o fim do frame. */
    float[] paletteSnapshot() {
        return palette.clone();
    }

    public List<Part> parts() {
        return parts;
    }

    public Skeleton skeleton() {
        return skeleton;
    }

    /**
     * O material da parte como {@link LitMaterial}, para refiná-lo (luz,
     * sombra, tint).
     *
     * @throws IllegalStateException se a parte usa outro tipo de material
     */
    public LitMaterial lit(int part) {
        Material material = parts.get(part).material();
        if (material instanceof LitMaterial lit) {
            return lit;
        }
        throw new IllegalStateException("O material da parte " + part + " não é um LitMaterial: "
                + material.getClass().getName());
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }
}
