package com.farias.jengine.toolkit3d;

import org.joml.Vector3f;

import com.farias.jengine.renderer.Material;
import com.farias.jengine.renderer.Shader;
import com.farias.jengine.renderer.Texture;

/**
 * Um {@link Material} para o shader padrão da engine: textura, iluminação
 * difusa por uma luz direcional e, opcionalmente, brilho especular e
 * recepção de sombra. É um {@code Material} comum (serve em
 * {@link TexturedModel} e {@code RenderCommand}) com métodos tipados no lugar
 * dos nomes de uniform. Criado por {@link LitMaterials#create(Texture)}.
 *
 * <p>
 * Por padrão é fosco, não recebe sombra, não tem tint e a luz vem de
 * (1, 1, 1): de cima e de frente para quem olha a cena dos eixos positivos.
 *
 * <pre>{@code
 * Material plastic = litMaterials.create(texture)
 *         .receiving(shadow)
 *         .glossy(0.5f, 48f, camera.directionToEye());
 * }</pre>
 */
public final class LitMaterial extends Material {

    private static final int TEXTURE_UNIT = 0;
    private static final int SHADOW_MAP_UNIT = 1;

    LitMaterial(Shader shader, Texture texture) {
        super(shader);
        setTexture("uTexture", texture, TEXTURE_UNIT);
        light(new Vector3f(1f, 1f, 1f), 0.2f, 0.8f);
        setFloat("specularStrength", 0f);
        setInt("receiveShadows", 0);
        tint(new Vector3f(1f, 1f, 1f), 0f);
    }

    /**
     * @param direction direção da superfície para a luz (não precisa estar
     *                  normalizada)
     * @param ambient   parcela de luz que chega em qualquer face
     * @param diffuse   parcela proporcional ao ângulo com a luz
     */
    public LitMaterial light(Vector3f direction, float ambient, float diffuse) {
        setVector3f("lightDirection", direction);
        setFloat("ambientStrength", ambient);
        setFloat("diffuseStrength", diffuse);
        return this;
    }

    /**
     * Brilho especular branco (acabamento de plástico).
     *
     * @param strength      intensidade do brilho (0 = fosco)
     * @param shininess     concentração: maior = reflexo mais fechado
     * @param viewDirection direção unitária da superfície para a câmera
     */
    public LitMaterial glossy(float strength, float shininess, Vector3f viewDirection) {
        setFloat("specularStrength", strength);
        setFloat("shininess", shininess);
        setVector3f("viewDirection", viewDirection);
        return this;
    }

    /**
     * Troca só a direção da câmera do brilho especular, sem mexer em
     * intensidade nem concentração. Para câmeras que se movem: chame quando a
     * {@code viewDirection} passada em {@link #glossy} deixar de valer.
     *
     * @param viewDirection direção unitária da superfície para a câmera
     */
    public LitMaterial viewDirection(Vector3f viewDirection) {
        setVector3f("viewDirection", viewDirection);
        return this;
    }

    /**
     * Mistura uma cor sobre a superfície, para destacar o modelo (hover,
     * seleção). Como os uniforms são do material, todo modelo que compartilha
     * este material recebe o mesmo tint.
     *
     * @param color    cor do tint
     * @param strength quanto da cor entra (0 = sem tint, 1 = só a cor)
     */
    public LitMaterial tint(Vector3f color, float strength) {
        setVector3f("tintColor", color);
        setFloat("tintStrength", strength);
        return this;
    }

    /**
     * Passa a receber a sombra de {@code shadow}. A direção da luz do material
     * vira a da luz da sombra, para o sombreamento e a sombra concordarem.
     */
    public LitMaterial receiving(ShadowMap shadow) {
        setVector3f("lightDirection", shadow.light().direction());
        setMatrix4f("lightSpaceMatrix", shadow.light().lightSpaceMatrix());
        setFloat("shadowBias", shadow.bias());
        setTexture("shadowMap", shadow.texture(), SHADOW_MAP_UNIT);
        setInt("receiveShadows", 1);
        return this;
    }
}
