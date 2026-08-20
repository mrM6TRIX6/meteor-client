#version 150

#moj_import <meteor-client:common.glsl>

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;

out vec2 FragCoord;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    FragCoord = rvertexcoord(gl_VertexID);
}
