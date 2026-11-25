package com.farias.jengine.toolkit3d;

/**
 * GLSL source embutido para os shaders 3D da engine. O
 * com.farias.jengine.renderer.Shader recebe o source direto (sem ler arquivo
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
     * {@code lightSpaceMatrix} e {@code shadowBias}. Emissão, quando
     * {@code useEmission != 0}: {@code emissionMap} (lido na mesma UV da
     * textura de cor) vezes {@code emissionStrength}, somada depois da luz, sem
     * receber luz, sombra nem tint.
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
            uniform vec3 tintColor;
            uniform float tintStrength;
            uniform sampler2D emissionMap;
            uniform int useEmission;
            uniform float emissionStrength;

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

                vec3 albedo = mix(texColor.rgb, tintColor, tintStrength);
                vec3 emission = useEmission != 0 ? texture(emissionMap, tex_coords).rgb * emissionStrength : vec3(0.0);
                fragColor = vec4(albedo * light + vec3(specular) + emission, texColor.a);
            }
            """;

    /**
     * Máximo de juntas de um esqueleto com skin: o tamanho do array
     * {@code uJoints} dos shaders skinned (128 {@code mat4} = 2048 componentes
     * de uniform no vertex shader). Um esqueleto maior é recusado em
     * {@link SkinnedModel}.
     */
    public static final int MAX_JOINTS = 128;

    /**
     * Skinning: cada vértice é a soma das matrizes {@code uJoints} das suas 4
     * juntas (atributo 3) ponderadas pelos pesos (atributo 4). Comum aos
     * shaders skinned de cor e de sombra.
     */
    private static final String SKINNING = """
            layout(location = 3) in ivec4 joints;
            layout(location = 4) in vec4 weights;

            uniform mat4 uJoints[%d];

            mat4 skinMatrix() {
                return weights.x * uJoints[joints.x]
                        + weights.y * uJoints[joints.y]
                        + weights.z * uJoints[joints.z]
                        + weights.w * uJoints[joints.w];
            }
            """.formatted(MAX_JOINTS);

    /**
     * {@link #STANDARD_VERTEX} com skinning: posição e normal deformadas pelo
     * esqueleto antes da matriz de modelo. Mesmas saídas, então usa o
     * {@link #STANDARD_FRAGMENT}. A normal usa a parte 3x3 da matriz de skin,
     * exata para juntas sem escala não uniforme.
     */
    public static final String SKINNED_VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;
            layout(location = 1) in vec2 textures;
            layout(location = 2) in vec3 normals;
            """ + SKINNING + """

            out vec2 tex_coords;
            out vec3 fragNormal;
            out vec4 fragLightSpace;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            uniform mat3 uNormal;
            uniform mat4 lightSpaceMatrix;

            void main() {
                mat4 skin = skinMatrix();
                tex_coords = textures;
                fragNormal = normalize(uNormal * (mat3(skin) * normals));
                vec4 worldPosition = uModel * (skin * vec4(vertices, 1.0));
                fragLightSpace = lightSpaceMatrix * worldPosition;
                gl_Position = uProjection * uView * worldPosition;
            }
            """;

    /** {@link #SHADOW_DEPTH_VERTEX} com skinning. */
    public static final String SHADOW_DEPTH_SKINNED_VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;
            """ + SKINNING + """

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;

            void main() {
                gl_Position = uProjection * uView * uModel * (skinMatrix() * vec4(vertices, 1.0));
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
