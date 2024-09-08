#version 130

in float passValue;
uniform float min;
uniform float max;
uniform int nrColors;
uniform vec3 palette[32];
 
void main() {
    int index = 0;
	if (max - min > 0.0) {
		index = int((passValue - min) * float(nrColors) / (max - min));
		if (index < 0) index = 0;
		if (index > nrColors-1) index = nrColors-1;
	}
    gl_FragColor = vec4(palette[index], 1.0);
}