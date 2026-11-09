package com.farias.jengine.toolkit3d;

import java.util.List;

import com.farias.jengine.animation.AnimationClip;
import com.farias.jengine.animation.Skeleton;
import com.farias.jengine.renderer.MeshData;

/**
 * O conteúdo de um glTF já convertido para tipos da engine, sem nada na GPU:
 * o que o {@link GltfReader} produz e o {@link Models} sobe. Toda malha tem
 * skin (as malhas rígidas são presas a uma junta com peso 1), então um modelo
 * glTF é sempre desenhado como {@link SkinnedModel}.
 *
 * @param skeleton   os nós necessários da cena, como juntas
 * @param primitives uma entrada por primitive de malha
 * @param clips      as animações, na ordem do arquivo
 * @param warnings   o que foi ignorado (morph targets, canais de nós fora do
 *                   esqueleto, primitives que não são triângulos...)
 */
record GltfData(Skeleton skeleton, List<Primitive> primitives, List<AnimationClip> clips, List<String> warnings) {

    /**
     * @param mesh      a malha, em espaço de bind, com juntas e pesos
     * @param image     a textura de cor base, ou null
     * @param baseColor RGBA do {@code baseColorFactor} (usado quando não há
     *                  textura)
     */
    record Primitive(MeshData mesh, Image image, float[] baseColor) {
    }

    /**
     * Uma imagem: ou um arquivo ({@code path}, relativo à raiz de assets), ou
     * bytes embutidos com a chave de cache {@code key}.
     */
    record Image(String path, byte[] bytes, String key) {
    }
}
