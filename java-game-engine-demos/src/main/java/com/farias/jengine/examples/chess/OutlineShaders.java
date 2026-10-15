package com.farias.jengine.examples.chess;

/** GLSL do passe de contorno de {@link ModelOutline}. */
final class OutlineShaders {

    private OutlineShaders() {
    }

    /** Quad de tela cheia; a UV sai da posição, de (0, 0) embaixo à esquerda. */
    static final String VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;

            out vec2 uv;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;

            void main() {
                uv = vertices.xy * 0.5 + 0.5;
                gl_Position = uProjection * uView * uModel * vec4(vertices, 1.0);
            }
            """;

    /**
     * {@code mask} é a profundidade do modelo sozinho: 1.0 onde ele não está.
     * Pinta o pixel vazio que tem o modelo em algum ponto de dois anéis à sua
     * volta (a {@code thickness} pixels e à metade disso); o resto é
     * descartado e a cena aparece.
     */
    static final String FRAGMENT = """
            #version 330 core

            uniform sampler2D mask;
            uniform vec2 texelSize;
            uniform float thickness;
            uniform vec3 color;

            in vec2 uv;
            out vec4 fragColor;

            const int DIRECTIONS = 16;
            const float TWO_PI = 6.28318530718;

            bool covered(vec2 position) {
                return texture(mask, position).r < 1.0;
            }

            void main() {
                if (covered(uv)) {
                    discard;
                }

                for (int ring = 1; ring <= 2; ring++) {
                    float radius = thickness * float(ring) * 0.5;
                    for (int i = 0; i < DIRECTIONS; i++) {
                        float angle = TWO_PI * float(i) / float(DIRECTIONS);
                        vec2 offset = vec2(cos(angle), sin(angle)) * radius * texelSize;
                        if (covered(uv + offset)) {
                            fragColor = vec4(color, 1.0);
                            return;
                        }
                    }
                }
                discard;
            }
            """;
}
