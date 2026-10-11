package com.farias.rengine.engine3d;

import org.joml.Vector3f;

import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;

/**
 * Fabrica de Materials para os casos comuns (modelo 3D com uma luz
 * direcional simples). So monta o Material a partir dos parametros recebidos
 * (o Renderer e injetado, nao ha estado alem dele).
 */
public final class Materials {
    private final Renderer renderer;

    public Materials(Renderer renderer) {
        this.renderer = renderer;
    }

    public Material lit(Shader litShader, Texture texture, Vector3f lightDirection,
            float ambientStrength, float diffuseStrength) {
        Material material = renderer.createMaterial(litShader, texture);
        material.setVector3f("lightDirection", lightDirection);
        material.setFloat("ambientStrength", ambientStrength);
        material.setFloat("diffuseStrength", diffuseStrength);
        return material;
    }

    public Material lit(Shader litShader, Texture texture) {
        // aponta para o mesmo octante da camera isometrica usada por
        // ModelViewer/IsometricChess (eye = target + (d,d,d)), senao o lado
        // visivel do modelo fica so com a luz ambiente (ver dot(normal,
        // lightDirection) no shader - sem o sinal batendo com a face virada
        // pra camera, ela nunca pega diffuse).
        return lit(litShader, texture, new Vector3f(1, 1, 1), 0.2f, 0.8f);
    }
}
