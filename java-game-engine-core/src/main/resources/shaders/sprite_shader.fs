#version 330 core

uniform sampler2D sampler;

in vec2 tex_coords;

out vec4 fragColor;

void main() {
    fragColor = texture(sampler, tex_coords);
}