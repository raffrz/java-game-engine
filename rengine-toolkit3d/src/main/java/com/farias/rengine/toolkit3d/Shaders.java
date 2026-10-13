package com.farias.rengine.toolkit3d;

/**
 * GLSL source embutido para os shaders 3D da engine. O
 * com.farias.rengine.renderer.Shader recebe o source direto (sem ler arquivo
 * de classpath), entao os sources ficam aqui como texto.
 */
public final class Shaders {

    private Shaders() {
    }

    /**
     * Vertex shader padrão: posição, UV e normal, mais a posição no espaço da
     * luz ({@code lightSpaceMatrix}) para o shadow mapping.
     * Usado por {@link LitMaterial}.
     */
    public static final String STANDARD_VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;
            layout(location = 1) in vec2 textures;
            layout(location = 2) in vec3 normals;

            out vec2 tex_coords;
            out vec3 fragNormal;
            out vec4 fragLightSpace;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            uniform mat3 uNormal;
            uniform mat4 lightSpaceMatrix;

            void main() {
                tex_coords = textures;
                fragNormal = normalize(uNormal * normals);
                vec4 worldPosition = uModel * vec4(vertices, 1.0);
                fragLightSpace = lightSpaceMatrix * worldPosition;
                gl_Position = uProjection * uView * worldPosition;
            }
            """;

    /**
     * Fragment shader padrão: textura com luz direcional difusa, e brilho
     * especular e sombra opcionais. Usado por {@link LitMaterial}. Só a cor é
     * afetada pela luz: o alpha vem direto da textura, senão fragmentos
     * escuros ficariam transparentes com blend ligado.
     *
     * <p>
     * Uniforms: {@code uTexture}, {@code lightDirection},
     * {@code ambientStrength} e {@code diffuseStrength}. Brilho especular
     * (Blinn-Phong, branco, estilo plástico): {@code specularStrength} (0 =
     * fosco), {@code shininess} e {@code viewDirection}, a direção da
     * superfície para a câmera (exata em projeção ortográfica, aproximação em
     * perspectiva). Sombra, quando {@code receiveShadows != 0}:
     * {@code shadowMap} (profundidade do ponto de vista da luz, filtro 3x3),
     * {@code lightSpaceMatrix} e {@code shadowBias}.
     */
    public static final String STANDARD_FRAGMENT = """
            #version 330 core

            uniform sampler2D uTexture;
            uniform sampler2D shadowMap;
            uniform vec3 lightDirection;
            uniform float ambientStrength;
            uniform float diffuseStrength;
            uniform float shadowBias;
            uniform int receiveShadows;
            uniform vec3 viewDirection;
            uniform float specularStrength;
            uniform float shininess;

            in vec2 tex_coords;
            in vec3 fragNormal;
            in vec4 fragLightSpace;
            out vec4 fragColor;

            float calculateShadow(vec4 lightSpacePosition, vec3 normal, vec3 lightDir) {
                vec3 projected = lightSpacePosition.xyz / lightSpacePosition.w;
                projected = projected * 0.5 + 0.5;

                if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0
                        || projected.y < 0.0 || projected.y > 1.0) {
                    return 0.0;
                }

                float bias = max(shadowBias * (1.0 - dot(normal, lightDir)), shadowBias * 0.25);
                vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
                float shadow = 0.0;

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        float closest = texture(shadowMap, projected.xy + vec2(x, y) * texelSize).r;
                        shadow += projected.z - bias > closest ? 1.0 : 0.0;
                    }
                }

                return shadow / 9.0;
            }

            void main() {
                vec4 texColor = texture(uTexture, tex_coords);

                vec3 normal = normalize(fragNormal);
                vec3 lightDir = normalize(lightDirection);

                float diffuse = max(dot(normal, lightDir), 0.0);
                float shadow = receiveShadows != 0 ? calculateShadow(fragLightSpace, normal, lightDir) : 0.0;
                float light = ambientStrength + (1.0 - shadow) * diffuse * diffuseStrength;

                vec3 halfway = normalize(lightDir + normalize(viewDirection));
                float highlight = pow(max(dot(normal, halfway), 0.0), max(shininess, 1.0));
                float specular = specularStrength * highlight * (diffuse > 0.0 ? 1.0 : 0.0) * (1.0 - shadow);

                fragColor = vec4(texColor.rgb * light + vec3(specular), texColor.a);
            }
            """;

    /** Passe de profundidade do ponto de vista da luz: só a posição, sem cor. */
    public static final String SHADOW_DEPTH_VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;

            void main() {
                gl_Position = uProjection * uView * uModel * vec4(vertices, 1.0);
            }
            """;

    public static final String SHADOW_DEPTH_FRAGMENT = """
            #version 330 core

            void main() {
            }
            """;
}
