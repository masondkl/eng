#version 330 core

layout (location=0) in vec3 in_position;
layout (location=1) in float in_tex_pos;

uniform mat4 projection;
uniform mat4 view;

out vec2 out_tex_pos;

void main()
{
    out_tex_pos = vec2(mod(in_tex_pos, 512.0), in_tex_pos / 512.0) / 512.0;
    gl_Position = projection * view * vec4(in_position, 1.0);
}