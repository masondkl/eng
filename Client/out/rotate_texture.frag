#version 330 core

uniform sampler2D SAMPLERS[];

flat in int out_texture;
in vec2 out_uv;

out vec4 color;

void main()
{
    color = texture(SAMPLERS[out_texture], out_uv);
    if (color.w == 0.0f) discard;
}