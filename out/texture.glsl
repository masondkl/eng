#version 330 core
layout (location=0) in vec2 aPos;
layout (location=1) in float aTexPosition;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 fTexPosition;

void main()
{
    fTexPosition = vec2(mod(aTexPosition, 512.0), aTexPosition / 512.0) / 512.0;
    gl_Position = uProjection * uView * vec4(aPos, 1.0, 1.0);
}

//|

#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 fTexPosition;

out vec4 color;

void main()
{
    color = texture(TEX_SAMPLER, fTexPosition);
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}