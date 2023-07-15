#version 330 core

uniform sampler2D TEX_SAMPLER;
uniform float triangles[1024];

out vec4 color;

in vec2 point;

//float area(float x1, float y1, float x2, float y2, float x3, float y3)
//{
//    return abs((x1*(y2-y3) + x2*(y3-y1)+ x3*(y1-y2))/2.0);
//}
//bool isInside(float x1, float y1, float x2, float y2, float x3, float y3, float x, float y)
//{
//    if (x1 == y1 && x1 == x2 && x1 == y2 && x1 == x3 && x1 == y3) return false;
//    float A = area (x1, y1, x2, y2, x3, y3);
//    float A1 = area (x, y, x2, y2, x3, y3);
//    float A2 = area (x1, y1, x, y, x3, y3);
//    float A3 = area (x1, y1, x2, y2, x, y);
//    float fixedA = A;
//    float fixedSum = A1 + A2 + A3;
//    float difference = fixedA - fixedSum;
//    return difference < 0.0001 && difference > -0.0001;
//}

void main()
{
    vec2 a = vec2(triangles[0], triangles[1]);
    vec2 b = vec2(triangles[2], triangles[3]);
    vec2 c = vec2(triangles[4], triangles[5]);

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
//        if (inside) break;
    }

    if (inside) {
        color = vec4(0.0, 0.0, 0.0, 0.0);
    } else {
        color = vec4(0.0, 0.0, 0.0, 0.2);
    }
}