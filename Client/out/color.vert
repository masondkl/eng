#version 330 core

layout (location=0) in vec3 in_position;
layout (location=1) in vec4 in_color;

uniform mat4 projection;
uniform mat4 view;

out vec4 v_color;

void main()
{
    v_color = in_color;
    gl_Position = projection * view * vec4(in_position, 1.0);
}