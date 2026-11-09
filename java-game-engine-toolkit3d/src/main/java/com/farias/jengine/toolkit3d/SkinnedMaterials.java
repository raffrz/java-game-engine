package com.farias.jengine.toolkit3d;

import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Texture;

/**
 * Quem cria o material de cada parte de um modelo com esqueleto, a partir da
 * cor base lida do arquivo. {@link LitMaterials} é o padrão da engine; um jogo
 * com shader próprio implementa esta interface e a passa a
 * {@link ModelAsset#instance(SkinnedMaterials)}.
 *
 * <p>
 * O material precisa de um vertex shader com skinning (por exemplo,
 * {@link Shaders#SKINNED_VERTEX}): o {@code SkinnedModel} entrega a pose no
 * uniform {@code uJoints}.
 *
 * <pre>{@code
 * final class ToonMaterials implements SkinnedMaterials {
 *     public Material createSkinned(Texture baseColor) {
 *         return new Material(toonShader).setTexture("uTexture", baseColor, 0);
 *     }
 * }
 * SkinnedModel hero = models.loadGltf("hero.glb").instance(toonMaterials);
 * }</pre>
 */
public interface SkinnedMaterials {

    /**
     * Um material novo para uma parte com skin.
     *
     * @param baseColor a cor base da parte (a textura do arquivo, ou 1x1 da
     *                  cor do material quando o arquivo não tem textura)
     */
    Material createSkinned(Texture baseColor);
}
