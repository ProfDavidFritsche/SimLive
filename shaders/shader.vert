#version 130

in vec3 inPosition;
in float inValue;
uniform mat4 projection;
uniform mat4 modelview;
out float passValue;

void main() {
    gl_Position = projection * modelview * vec4(inPosition, 1.0);
	passValue = inValue;
}