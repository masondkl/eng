#version 330 core

layout (location=0) in vec3 in_position;
layout (location=1) in float in_texture_position;

uniform mat4 projection;
uniform mat4 view;

out vec2 out_position;

void main()
{
    out_position = vec2(mod(in_texture_position, 512.0), in_texture_position / 512.0) / 512.0;
    gl_Position = projection * view * vec4(in_position, 1.0);
}