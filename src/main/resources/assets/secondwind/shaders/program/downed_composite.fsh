#version 150

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float time;
uniform vec3 cameraPos;
uniform vec3 lookVector;
uniform vec3 upVector;
uniform vec3 leftVector;
uniform mat4 invViewMat;
uniform mat4 invProjMat;
uniform float nearPlaneDistance;
uniform float farPlaneDistance;
uniform float fov;
uniform float aspectRatio;
uniform vec3 bobOffset;
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

vec3 sampleBlur(vec2 uv, vec2 texelStep) {
    vec3 blur = texture(DiffuseSampler, uv).rgb * 0.227027;
    blur += texture(DiffuseSampler, uv + texelStep * 1.384615).rgb * 0.316216;
    blur += texture(DiffuseSampler, uv - texelStep * 1.384615).rgb * 0.316216;
    blur += texture(DiffuseSampler, uv + texelStep * 3.230769).rgb * 0.070270;
    blur += texture(DiffuseSampler, uv - texelStep * 3.230769).rgb * 0.070270;
    return blur;
}

void main() {
    vec4 original = texture(DiffuseSampler, texCoord);
    vec2 centeredUv = texCoord * 2.0 - 1.0;
    vec2 texelSize = 1.0 / InSize;

    vec4 projected = invProjMat * vec4(centeredUv, 0.0, 1.0);
    vec4 viewDirection = invViewMat * vec4(0.0, 0.0, -1.0, 0.0);
    float depthSpan = max(farPlaneDistance - nearPlaneDistance, 0.0001);
    float cameraPhase = dot(cameraPos + bobOffset, vec3(0.0031, 0.0023, 0.0017));
    float orientationBias = dot(normalize(lookVector + upVector * 0.5 - leftVector * 0.35), vec3(0.57735));
    float lensBias = clamp(fov / 3.14159265, 0.0, 1.0);

    float pulsePhase = time * (3.5 + Urgency * 3.0)
            + cameraPhase
            + orientationBias * 0.35
            + projected.x * 0.015
            + viewDirection.z * 0.02
            + depthSpan * 0.0001;
    float pulse = 0.5 + 0.5 * sin(pulsePhase);
    float glowPulse = 0.96 + pulse * (0.01 + PulseStrength * 0.08);

    float grayscale = luma(original.rgb);
    vec3 desaturated = mix(original.rgb, vec3(grayscale), DesaturationStrength);
    vec3 bloodTint = vec3(grayscale * 1.08, grayscale * 0.52, grayscale * 0.54)
            + vec3(0.12, 0.0, 0.0) * Urgency;
    vec3 graded = mix(desaturated, bloodTint, TintStrength);

    vec3 blurred = sampleBlur(texCoord, vec2(texelSize.x * (2.0 + Urgency * 1.5), 0.0));
    blurred = mix(blurred, sampleBlur(texCoord, vec2(0.0, texelSize.y * (2.0 + Urgency * 1.5))), 0.5);
    vec3 bloom = max(blurred - original.rgb * 0.55, 0.0) * (0.74 + Urgency * 0.45 + lensBias * 0.08);
    vec3 bloomed = graded + bloom * BloomStrength * glowPulse;

    vec2 vignetteUv = vec2(centeredUv.x * aspectRatio, centeredUv.y);
    float dist = length(vignetteUv) * (0.82 + lensBias * 0.16);
    float vignette = smoothstep(0.35, 1.15 - Urgency * 0.12, dist);
    float vignetteDarkness = vignette * VignetteStrength * (0.9 + pulse * 0.025);
    vec3 vignetted = mix(bloomed, bloomed * (1.0 - vignetteDarkness), DownedBlend);

    vec3 finalColor = mix(original.rgb, vignetted, DownedBlend);
    fragColor = vec4(clamp(finalColor, 0.0, 1.0), original.a);
}
