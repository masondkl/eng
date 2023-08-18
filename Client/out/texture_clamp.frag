#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 out_tex_pos;
in vec2 out_tex_center;
in vec2 out_tex_radius;

out vec4 color;

void main()
{
    vec2 tex_pos = out_tex_pos;
    color = texture(TEX_SAMPLER, tex_pos);
//    if (tex_pos.x < out_tex_center.x - out_tex_radius.x) tex_pos.x = out_tex_center.x - out_tex_radius.x;
//    if (tex_pos.y < out_tex_center.y - out_tex_radius.y) tex_pos.y = out_tex_center.y - out_tex_radius.y;
//    if (tex_pos.x > out_tex_center.x + out_tex_radius.x) tex_pos.x = out_tex_center.x + out_tex_radius.x;
//    if (tex_pos.y > out_tex_center.y + out_tex_radius.y) tex_pos.y = out_tex_center.y + out_tex_radius.y;

    if (tex_pos.x < out_tex_center.x - out_tex_radius.x) { color.r = 1.0f; color.g = 1.0f; color.b = 1.0f; }
    if (tex_pos.y < out_tex_center.y - out_tex_radius.y) { color.r = 1.0f; color.g = 1.0f; color.b = 1.0f; }
    if (tex_pos.x > out_tex_center.x + out_tex_radius.x) { color.r = 1.0f; color.g = 1.0f; color.b = 1.0f; }
    if (tex_pos.y > out_tex_center.y + out_tex_radius.y) { color.r = 1.0f; color.g = 1.0f; color.b = 1.0f; }
    if (color.w == 0.0f) discard;
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}