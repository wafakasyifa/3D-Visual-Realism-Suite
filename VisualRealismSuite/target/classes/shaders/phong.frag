#version 330 core
// ==========================================================
// Fragment Shader: Phong / Blinn-Phong per-pixel + Texture + Shadow Mapping (PCF 3x3)
// ==========================================================
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;      // posisi cahaya (dianggap directional: arah = normalize(lightPos))
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;   // unit 0
uniform sampler2D shadowMap;        // unit 1
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform float shadowBias;

// Mengembalikan 0.0 (terang) .. 1.0 (bayangan penuh)
float calcShadow(vec4 fragPosLight, vec3 N, vec3 L) {
    vec3 p = fragPosLight.xyz / fragPosLight.w;   // perspective divide -> NDC
    p = p * 0.5 + 0.5;                            // NDC [-1,1] -> [0,1]
    if (p.z > 1.0 || p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0)
        return 0.0;                               // di luar frustum cahaya -> terang

    // Slope-scaled bias: makin miring permukaan terhadap cahaya, makin besar bias
    float bias = max(shadowBias * (1.0 - dot(N, L)), shadowBias * 0.1);

    // PCF 3x3 untuk tepi bayangan yang lebih halus
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closest = texture(shadowMap, p.xy + vec2(x, y) * texel).r;
            shadow += (p.z - bias > closest) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse (Lambert)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos);          // arah menuju cahaya (directional)
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) {
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Phong standar
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow: faktor 1.0 (terang) atau 0.2 (bayangan) pada komponen diffuse+specular
    float shadow = useShadow ? calcShadow(FragPosLightSpace, norm, lightDir) : 0.0;
    float lit = mix(1.0, 0.2, shadow);

    // 5. Tekstur / warna dasar
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;

    vec3 result = (ambient + lit * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
