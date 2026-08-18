#version 150

layout(std140) uniform Uniforms {
    mat4 uInvProjection;
    mat4 uInvView;
    vec4 uCameraPosRadius;
    vec4 uCenterWidth;
    vec4 uOuterColor;
    vec4 uMidColor;
    vec4 uInnerColor;
    vec4 uScanlineColor;
    vec4 uMeta;
};

in vec2 vUV;
out vec4 fragColor;

uniform sampler2D DepthSampler;

float scanlines() {
    return sin(gl_FragCoord.y) * 0.5 + 0.5;
}

float hash12(vec2 value) {
    vec3 p = fract(vec3(value.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec2 surfaceProjection(vec3 worldPosition) {
    vec3 dx = dFdx(worldPosition);
    vec3 dy = dFdy(worldPosition);
    vec3 faceNormal = cross(dx, dy);
    vec3 normal = abs(faceNormal) / max(length(faceNormal), 0.0001);
    if (normal.y >= normal.x && normal.y >= normal.z) {
        return worldPosition.xz;
    }
    if (normal.x >= normal.z) {
        return worldPosition.zy;
    }
    return worldPosition.xy;
}

vec3 reconstructWorldPos(float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(vUV * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = uInvProjection * clipSpacePosition;
    viewSpacePosition /= max(viewSpacePosition.w, 0.0001);
    vec4 worldSpacePosition = uInvView * viewSpacePosition;
    return uCameraPosRadius.xyz + worldSpacePosition.xyz;
}

void main() {
    float depth = texture(DepthSampler, vUV).r;
    if (depth >= 1.0) {
        discard;
    }

    vec3 worldPosition = reconstructWorldPos(depth);
    float radius = uCameraPosRadius.w;
    float width = max(uCenterWidth.w, 0.0001);
    float flick = clamp(uMeta.z, 0.0, 1.0);
    float cellSize = mix(0.27, 0.18, flick);

    vec2 surfacePosition = surfaceProjection(worldPosition);
    vec2 cell = floor(surfacePosition / cellSize);
    vec2 cellUv = fract(surfacePosition / cellSize);
    float cellNoise = hash12(cell);
    float cellNoise2 = hash12(cell + vec2(floor(uMeta.y * 18.0), floor(uMeta.y * 11.0)));

    float dist = length(worldPosition - uCenterWidth.xyz);
    if (dist >= radius || dist <= radius - width) {
        discard;
    }

    float diff = clamp(1.0 - (radius - dist) / width, 0.0, 1.0);
    float edgePower = max(1.35, uMeta.x * 0.085);
    float front = pow(diff, edgePower);
    float circularEdge = 1.0 - smoothstep(radius - max(cellSize * 0.85, 0.16), radius, dist);
    float circularSpread = smoothstep(0.015, 0.13, diff);
    float trail = smoothstep(0.035, 0.46, diff) * (1.0 - smoothstep(0.90, 1.0, uMeta.y));

    vec2 squareUv = abs(cellUv - vec2(0.5));
    float squareCore = 1.0 - smoothstep(0.38, 0.50, max(squareUv.x, squareUv.y));
    float softCell = mix(0.72, 1.0, squareCore);
    float corruption = max(smoothstep(cellNoise * 0.14, 0.88, diff), trail * (0.42 + cellNoise2 * 0.28));
    float pulse = 0.72 + 0.28 * cellNoise2;
    float sparks = step(0.82, cellNoise2) * squareCore * front;

    vec4 infection = mix(uInnerColor, uMidColor, corruption);
    vec4 edge = mix(infection, uOuterColor, front);
    float squareMask = max(softCell * corruption * pulse, circularSpread * 0.62);
    float intensity = squareMask * 0.92 + front * 1.05 + trail * softCell * 0.36 + sparks * 0.42;

    vec4 color = edge * intensity;
    color += uMidColor * trail * softCell * 0.28;
    color += uScanlineColor * scanlines() * (front * 0.68 + trail * 0.24 + sparks * 0.34) * squareCore;
    color *= (0.42 + diff * 0.72) * circularEdge;
    fragColor = color;
}
