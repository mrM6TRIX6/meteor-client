#version 410 core

in vec4 vColor;
in vec2 vUV;

out vec4 fragColor;

void main() {
    float dist = length(vUV);
    
    float coreGlow = exp(-dist * dist * 4.0);
    float outerGlow = exp(-dist * dist * 0.8) * 0.3;
    float softEdge = smoothstep(1.2, 0.0, dist);
    
    float glow = (coreGlow * 0.7 + outerGlow) * softEdge;
    
    float alpha = glow * vColor.a;
    
    if (alpha < 0.005) discard;
    
    fragColor = vec4(vColor.rgb, alpha);
}
