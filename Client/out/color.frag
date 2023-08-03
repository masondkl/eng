#version 330 core

in vec4 v_color;

out vec4 color;

void main()
{
    color = v_color;
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}