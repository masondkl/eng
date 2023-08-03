#version 330 core

uniform sampler2D TEX_SAMPLER;
uniform float triangles[1023];

in vec2 point;
in vec2 out_position;

out vec4 color;

void main()
{
    bool inside = false;
    for (int i = 0; i < 128; i++) {
        int offset = i * 3 * 2;
        float ax = triangles[offset];
        float ay = triangles[offset + 1];

        float bx = triangles[offset + 2];
        float by = triangles[offset + 3];

        float cx = triangles[offset + 4];
        float cy = triangles[offset + 5];

        float s1 = cy - ay;
        float s2 = cx - ax;
        float s3 = by - ay;

        float s4 = point.y - ay;

        float w1 = (ax * s1 + s4 * s2 - point.x * s1) / (s3 * s2 - (bx-ax) * s1);
        float w2 = (s4- w1 * s3) / s1;
        if (inside = (w1 >= 0 && w2 >= 0 && (w1 + w2) <= 1)) break;
    }

    if (inside) color = texture(TEX_SAMPLER, out_position);
    else color = vec4(0.0, 0.0, 0.0, 0.0);
//    color = vec4(1.0, 1.0, 1.0, 1.0);
}