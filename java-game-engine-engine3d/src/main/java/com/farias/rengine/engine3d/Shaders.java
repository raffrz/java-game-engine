package com.farias.rengine.engine3d;

/**
 * GLSL source embutido para os shaders 3D (lit) da engine. O
 * com.farias.rengine.renderer.Shader recebe o source direto (sem ler arquivo
 * de classpath), entao os sources ficam aqui como texto.
 */
public final class Shaders {

    private Shaders() {
    }

    public static final String LIT_VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;
            layout(location = 1) in vec2 textures;
            layout(location = 2) in vec3 normals;

            out vec2 tex_coords;
            out vec3 fragNormal;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            uniform mat3 uNormal;

            void main() {
                tex_coords = textures;
                fragNormal = normalize(uNormal * normals);
                gl_Position = uProjection * uView * uModel * vec4(vertices, 1.0);
            }
            """;

    public static final String LIT_FRAGMENT = """
            #version 330 core

            uniform sampler2D uTexture;
            uniform vec3 lightDirection;
            uniform float ambientStrength;
            uniform float diffuseStrength;

            in vec2 tex_coords;
            in vec3 fragNormal;
            out vec4 fragColor;

            void main() {
                vec4 texColor = texture(uTexture, tex_coords);

                vec3 normal = normalize(fragNormal);
                float diffuse = max(dot(normal, normalize(lightDirection)), 0.0);
                float light = ambientStrength + diffuse * diffuseStrength;

                // so a cor e afetada pela luz - o alpha vem direto da
                // textura, senao fragmentos escuros/em sombra ficam com
                // alpha baixo e, com blend ligado, parecem transparentes.
                fragColor = vec4(texColor.rgb * light, texColor.a);
            }
            """;
}
