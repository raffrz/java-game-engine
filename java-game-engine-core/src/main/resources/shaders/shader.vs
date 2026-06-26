#version 330 core

layout(location = 0) in vec3 vertices;
layout(location = 1) in vec2 textures;
layout(location = 2) in vec3 normals;

out vec2 tex_coords;
out vec3 fragNormal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform mat3 normalMatrix;

void main() {
    tex_coords = textures;
    vec4 worldPosition = model * vec4(vertices, 1.0);

    fragNormal = normalize(normalMatrix * normals);

    gl_Position = projection * view * worldPosition;
}
