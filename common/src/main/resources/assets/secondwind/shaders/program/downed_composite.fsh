#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float time;
uniform float aspectRatio;
uniform float DownedBlend;
uniform float Urgency;
uniform float PulseStrength;
uniform float VignetteStrength;
uniform float DesaturationStrength;
uniform float TintStrength;
uniform float BloomStrength;

in vec2 texCoord;
out vec4 fragColor;

float luma(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec2 centeredUv = texCoord * 2.0 - 1.0;

    float pulsePhase = time * (3.5 + Urgency * 3.0)
            + centeredUv.x * 0.08
            + centeredUv.y * 0.05 * aspectRatio;
    float pulse = 0.5 + 0.5 * sin(pulsePhase);
    float glowPulse = 0.96 + pulse * (0.01 + PulseStrength * 0.08);

    float grayscale = luma(original.rgb);
    vec3 desaturated = mix(original.rgb, vec3(grayscale), DesaturationStrength);
    vec3 bloodTint = vec3(grayscale * 1.08, grayscale * 0.52, grayscale * 0.54)
            + vec3(0.12, 0.0, 0.0) * Urgency;
    vec3 graded = mix(desaturated, bloodTint, TintStrength);

    vec2 vignetteUv = vec2(centeredUv.x * aspectRatio, centeredUv.y);
    float dist = length(vignetteUv) * 0.9;
    float vignette = smoothstep(0.35, 1.15 - Urgency * 0.12, dist);
    float vignetteDarkness = vignette * VignetteStrength * (0.9 + pulse * 0.025);
    vec3 vignetted = mix(graded, graded * (1.0 - vignetteDarkness), DownedBlend);

    vec3 finalColor = mix(original.rgb, vignetted, DownedBlend);
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), original.a);
}
