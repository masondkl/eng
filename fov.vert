#version 330 core
layout (location=0) in vec2 in_position;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 out_position;

void main() {
    gl_Position = uProjection * uView * vec4(in_position, 1.0, 1.0);
}