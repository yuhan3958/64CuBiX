#version 330 core
in vec2 vUV;
flat in int vLayer;
out vec4 FragColor;

uniform sampler2DArray uBlockTextures;

void main(){
    FragColor = texture(uBlockTextures, vec3(vUV, vLayer));
}
