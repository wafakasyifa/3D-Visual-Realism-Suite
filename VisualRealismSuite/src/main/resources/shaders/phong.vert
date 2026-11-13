#version 330 core
// ==========================================================
// Vertex Shader: Pencahayaan per-pixel (Phong/Blinn-Phong) + Shadow Mapping
// ==========================================================
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoords;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;   // P_light * V_light
uniform mat3 normalMatrix;       // transpose(inverse(mat3(model))) dihitung di CPU

void main() {
    vec4 worldPos = model * vec4(aPos, 1.0);
    FragPos = worldPos.xyz;
    Normal = normalMatrix * aNormal;
    TexCoords = aTexCoords;
    FragPosLightSpace = lightSpaceMatrix * worldPos;   // untuk uji bayangan
    gl_Position = projection * view * worldPos;
}
