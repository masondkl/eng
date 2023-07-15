#version 330 core
layout (location=0) in vec2 in_position;
layout (location=1) in float in_texture_position;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 out_position;

void main()
{
    out_position = vec2(mod(in_texture_position, 512.0), in_texture_position / 512.0) / 512.0;
    gl_Position = uProjection * uView * vec4(in_position, 1.0, 1.0);
}

//|

#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 out_position;

out vec4 color;

void main()
{
    color = texture(TEX_SAMPLER, out_position);
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}