#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 out_position;
in vec3 out_color;

out vec4 color;

void main()
{
    color = texture(TEX_SAMPLER, out_position);
    if (color.w != 0.0f) {
        color.r = out_color.r;
        color.g = out_color.g;
        color.b = out_color.b;
    }
    if (color.w < 0.9f) discard;
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}