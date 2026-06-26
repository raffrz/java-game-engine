#version 330 core

uniform sampler2D sampler;

uniform vec3 lightDirection;
uniform float ambientStrength;
uniform float diffuseStrength;

// Entradas vindas do Vertex Shader substituem os antigos 'varying'
in vec2 tex_coords;
in vec3 fragNormal;

// É obrigatório declarar explicitamente a cor de saída no OpenGL moderno
out vec4 fragColor;

void main() {
    // texture2D foi descontinuado, agora usamos apenas texture()
    vec4 texColor = texture(sampler, tex_coords); 
    
    vec3 normal = normalize(fragNormal);
    float diffuse = max(dot(normal, normalize(lightDirection)), 0.0);
    float light = ambientStrength + diffuse * diffuseStrength;
    
    // Substitui o antigo gl_FragColor pela nossa variável de saída
    fragColor = texColor * light; 
}
