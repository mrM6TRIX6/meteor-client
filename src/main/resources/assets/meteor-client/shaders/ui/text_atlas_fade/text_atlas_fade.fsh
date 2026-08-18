#version 150

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 TexCoord;
in vec4 VertexColor;

out vec4 OutColor;

float sampledCoverage(vec2 uv) {
    vec2 pixelX = dFdx(uv);
    vec2 pixelY = dFdy(uv);
    vec2 atlasSize = vec2(textureSize(Sampler0, 0));
    float footprint = max(length(pixelX * atlasSize), length(pixelY * atlasSize));

    float center = texture(Sampler0, uv).a;
    float axes =
        texture(Sampler0, uv + pixelX * 0.42).a +
        texture(Sampler0, uv - pixelX * 0.42).a +
        texture(Sampler0, uv + pixelY * 0.42).a +
        texture(Sampler0, uv - pixelY * 0.42).a;
    float diagonals =
        texture(Sampler0, uv + (pixelX + pixelY) * 0.34).a +
        texture(Sampler0, uv - (pixelX + pixelY) * 0.34).a +
        texture(Sampler0, uv + (pixelX - pixelY) * 0.34).a +
        texture(Sampler0, uv + (-pixelX + pixelY) * 0.34).a;

    float coverage = center * 0.36 + axes * 0.11 + diagonals * 0.05;
    float smallText = smoothstep(3.0, 5.5, footprint);
    coverage = mix(coverage, clamp(coverage * 1.14 + 0.026, 0.0, 1.0), smallText);

    coverage = smoothstep(0.025, mix(0.88, 0.78, smallText), coverage);
    return pow(clamp(coverage, 0.0, 1.0), mix(0.95, 0.86, smallText));
}

void main() {
    float fadeAlpha = clamp(VertexColor.a, 0.0, 1.0);
    float alpha = sampledCoverage(TexCoord) * fadeAlpha;
    if (alpha <= 0.001) {
        discard;
    }
    OutColor = vec4(VertexColor.rgb, alpha) * ColorModulator;
}
