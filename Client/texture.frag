#version 330 core

uniform sampler2D SAMPLERS[2];

in vec2 out_tex_pos;
in vec4 out_color;
flat in int out_character;
flat in int out_fill;
flat in int out_tex;

out vec4 color;

void main()
{
    color = texture(SAMPLERS[out_tex], out_tex_pos);
    if (out_character == 1 && color.w != 0f) {
        color.x = out_color.x;
        color.y = out_color.y;
        color.z = out_color.z;
    } else if (out_fill == 1) {
        color.x = out_color.x;
        color.y = out_color.y;
        color.z = out_color.z;
        color.w = out_color.w;
    }
    if (color.w == 0f) discard;
}