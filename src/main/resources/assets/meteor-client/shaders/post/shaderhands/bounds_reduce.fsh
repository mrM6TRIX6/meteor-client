#version 150

uniform sampler2D BoundsTex;

layout(std140) uniform ReduceConfig {
    vec4 SourceTexel;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 o = SourceTexel.xy * 0.5;
    vec4 a = texture(BoundsTex, texCoord + vec2(-o.x, -o.y));
    vec4 b = texture(BoundsTex, texCoord + vec2( o.x, -o.y));
    vec4 c = texture(BoundsTex, texCoord + vec2(-o.x,  o.y));
    vec4 d = texture(BoundsTex, texCoord + vec2( o.x,  o.y));
    vec2 mn = min(min(a.xy, b.xy), min(c.xy, d.xy));
    vec2 mx = max(max(a.zw, b.zw), max(c.zw, d.zw));
    fragColor = vec4(mn, mx);
}
