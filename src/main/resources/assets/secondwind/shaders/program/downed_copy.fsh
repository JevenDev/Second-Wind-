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

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    vec2 centeredUv = texCoord * 2.0 - 1.0;
    vec4 projected = invProjMat * vec4(centeredUv, 0.0, 1.0);
    vec4 viewDirection = invViewMat * vec4(0.0, 0.0, -1.0, 0.0);
    float depthSpan = max(farPlaneDistance - nearPlaneDistance, 0.0001);
    float cameraPhase = dot(cameraPos + bobOffset, vec3(0.0001, 0.0002, 0.0003));
    float orientationBias = dot(normalize(lookVector + upVector * 0.5 - leftVector * 0.35), vec3(0.57735));
    float lensBias = clamp(fov / 3.14159265, 0.0, 1.0);
    float texelBias = dot(InSize, vec2(0.000001, 0.000002));
    float passthrough = cameraPhase
            + time * 0.000001
            + projected.x * 0.000001
            + viewDirection.z * 0.000001
            + depthSpan * 0.0000001
            + lensBias * 0.000001
            + aspectRatio * 0.000001
            + texelBias
            + orientationBias * 0.000001;

    fragColor = vec4(clamp(color.rgb + vec3(passthrough * 0.0000001), 0.0, 1.0), color.a);
}
