package com.farias.jengine.toolkit3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.farias.jengine.animation.AnimationClip;
import com.farias.jengine.animation.Skeleton;
import com.farias.jengine.renderer.Mesh;
import com.farias.jengine.renderer.Texture;

/**
 * Um modelo glTF carregado por {@link Models#loadGltf}: malhas e texturas já
 * na GPU, o {@link Skeleton} e os {@link AnimationClip}s. É compartilhado
 * (cache por caminho); cada personagem na cena é uma {@link #instance()}.
 *
 * <pre>{@code
 * ModelAsset fox = models.loadGltf("models/fox.glb");
 * SkinnedModel model = fox.instance();
 * Animator animator = new Animator(fox.skeleton()).play(fox.clip("Walk"), true);
 * }</pre>
 */
public final class ModelAsset {

    private final Skeleton skeleton;
    private final List<Mesh> meshes;
    private final List<Texture> textures;
    private final Map<String, AnimationClip> clips = new LinkedHashMap<>();
    private final SkinnedMaterials defaultMaterials;

    ModelAsset(Skeleton skeleton, List<Mesh> meshes, List<Texture> textures, List<AnimationClip> clips,
            SkinnedMaterials defaultMaterials) {
        this.skeleton = skeleton;
        this.meshes = List.copyOf(meshes);
        this.textures = List.copyOf(textures);
        this.defaultMaterials = defaultMaterials;
        for (AnimationClip clip : clips) {
            this.clips.putIfAbsent(clip.name(), clip);
        }
    }

    public Skeleton skeleton() {
        return skeleton;
    }

    /** Os clips do arquivo, na ordem dele (nomes repetidos: vale o primeiro). */
    public List<AnimationClip> clips() {
        return List.copyOf(clips.values());
    }

    /** @throws IllegalArgumentException se o arquivo não tem um clip com esse nome */
    public AnimationClip clip(String name) {
        AnimationClip clip = clips.get(name);
        if (clip == null) {
            throw new IllegalArgumentException("no animation '" + name + "'; available: " + clips.keySet());
        }
        return clip;
    }

    /**
     * Um modelo novo para pôr na cena, no repouso. Compartilha malhas,
     * texturas e esqueleto com as outras instâncias, mas tem os seus próprios
     * materiais ({@link LitMaterial}, um por parte), transformação e pose.
     */
    public SkinnedModel instance() {
        return instance(defaultMaterials);
    }

    /**
     * Idem, com os materiais de {@code materials} (um shader próprio do jogo,
     * por exemplo): um por parte, criado com a cor base daquela parte. O
     * material é escolhido aqui, e não no {@link Models#loadGltf}, porque o
     * asset é compartilhado: o mesmo arquivo pode ter instâncias com visuais
     * diferentes.
     */
    public SkinnedModel instance(SkinnedMaterials materials) {
        List<SkinnedModel.Part> parts = new ArrayList<>(meshes.size());
        for (int i = 0; i < meshes.size(); i++) {
            parts.add(new SkinnedModel.Part(meshes.get(i), materials.createSkinned(textures.get(i))));
        }
        return new SkinnedModel(parts, skeleton);
    }
}
