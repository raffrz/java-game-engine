package com.farias.rengine.toolkit2d;

/**
 * GLSL source embutido do shader de sprite 2D. O
 * com.farias.rengine.renderer.Shader recebe o source direto (sem ler arquivo
 * de classpath), entao o source fica aqui como texto.
 */
final class SpriteShaders {

    private SpriteShaders() {
    }

    public static final String SPRITE_VERTEX = """
            #version 330 core

            layout(location = 0) in vec3 vertices;
            layout(location = 1) in vec2 textures;
            layout(location = 2) in vec3 normals;

            out vec2 tex_coords;

            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            uniform vec2 uvOffset;
            uniform vec2 uvScale;

            void main() {
                tex_coords = textures * uvScale + uvOffset;
                gl_Position = uProjection * uView * uModel * vec4(vertices, 1.0);
            }
            """;

    public static final String SPRITE_FRAGMENT = """
            #version 330 core

            uniform sampler2D uTexture;

            in vec2 tex_coords;
            out vec4 fragColor;

            void main() {
                fragColor = texture(uTexture, tex_coords);
            }
            """;
}
