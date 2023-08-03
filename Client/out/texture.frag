#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 out_position;

out vec4 color;

void main()
{
    color = texture(TEX_SAMPLER, out_position);
    if (color.w == 0.0f) discard;
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}