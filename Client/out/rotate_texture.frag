#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 fTexPosition;

out vec4 color;

void main()
{
    color = texture(TEX_SAMPLER, fTexPosition);
    if (color.w == 0.0f) discard;
}