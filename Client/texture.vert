#version 330 core

layout (location=0) in float in_pack_pos;
layout (location=1) in float in_pack_uv_flags;
layout (location=2) in float in_pack_rgb;


uniform mat4 uiProjection;
uniform mat4 uiView;

uniform mat4 projection;
uniform mat4 view;

flat out int out_character;
flat out int out_tex;
flat out int out_fill;
out vec2 out_tex_pos;
out vec4 out_color;

void main()
{
    int pos = floatBitsToInt(in_pack_pos);
    int rgb = floatBitsToInt(in_pack_rgb);
    float a = (rgb & 0xFF) / 128f;
    float b = ((rgb >> 8) & 0xFF) / 128f;
    float g = ((rgb >> 16) & 0xFF) / 128f;
    float r = ((rgb >> 24) & 0xFF) / 128f;
    //   32767 16381 8192 4096 2048 1024 512 256 | 128 64 32 16 8 4 2 1
    //           F            |         F       |     F     |    F
    float x = float(pos >> 16 & 0xFFFF) / 16f - 256;
    float y = float(pos & 0xFFFF) / 16f - 256;
    int uv_flags = floatBitsToInt(in_pack_uv_flags);
    int tex_pos_x = (uv_flags >> 24) & 0xFF;
    int tex_pos_y = (uv_flags >> 16) & 0xFF;
    int flags = (uv_flags >> 12) & 0xF;
    int ui = flags & 1;
    int character = (flags >> 1) & 1;
    int fill = (flags >> 2) & 1;
    int tex = (uv_flags >> 8) & 0xF;
    int z = uv_flags & 0xFF;
    out_tex = tex;
    out_fill = fill;
    out_tex_pos = vec2(float(tex_pos_x), float(tex_pos_y)) / 128.0;
    out_character = character;
    out_color = vec4(r, g, b, a);
    if (ui == 1) {
        gl_Position = uiProjection * uiView * vec4(x, y, float(z), 1.0);
    } else {
        gl_Position = projection * view * vec4(x, y, float(z), 1.0);
    }
}