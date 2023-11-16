#version 330 core

layout (location=0) in float texture;
layout (location=1) in vec3 aPos;
layout (location=2) in vec2 aPivot;
layout (location=3) in float aRotation;
layout (location=4) in float aTexPosition;

uniform mat4 projection;
uniform mat4 view;

flat out int out_texture;
out vec2 out_uv;

void main()
{
    vec3 vec3Pivot = vec3(aPivot, 0.0);
    float cosa = cos(aRotation);
    float sina = sin(aRotation);
    vec3 position = aPos;
    position -= vec3Pivot;
    vec3 pos = vec3(
        cosa * position.x - sina * position.y,
        cosa * position.y + sina * position.x,
        position.z
    ) + vec3Pivot;
    out_texture = int(texture);
    out_uv = vec2(mod(aTexPosition, 512.0), aTexPosition / 512.0) / 512.0;
    gl_Position = projection * view * vec4(pos, 1.0);
}