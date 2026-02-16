#version 330 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec2 aUV;

uniform mat4 uProj;
uniform mat4 uView;
uniform vec3 uChunkPos;

out vec2 vUV;

void main(){
    vUV = aUV;
    vec3 worldPos = aPos + uChunkPos;
    gl_Position = uProj * uView * vec4(worldPos, 1.0);
}
