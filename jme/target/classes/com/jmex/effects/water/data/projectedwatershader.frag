varying vec2 refrCoords;
varying vec2 normCoords;
varying vec2 foamCoords;
varying vec4 viewCoords;
varying vec3 viewTangetSpace;
varying vec2 vnormal;
varying vec4 vVertex;

uniform sampler2D normalMap;
uniform sampler2D reflection;
uniform sampler2D dudvMap;
uniform sampler2D foamMap;

uniform vec4 waterColor;
uniform vec4 waterColorEnd;
uniform int abovewater;
uniform float amplitude;
//uniform float dudvPower; //0.005
//uniform float dudvColorPower; //0.01
//uniform float normalPower; //0.5
//uniform float normalOffsetPower; //0.6

void main()
{
	float fogDist = clamp((viewCoords.z-gl_Fog.start)*gl_Fog.scale,0.0,1.0);

	vec2 distOffset = texture2D(dudvMap, refrCoords).xy * 0.01;
	vec3 dudvColor = texture2D(dudvMap, normCoords + distOffset).xyz;
	dudvColor = normalize(dudvColor * 2.0 - 1.0) * 0.015;

	vec3 normalVector = texture2D(normalMap, normCoords + distOffset * 0.6).xyz;
	normalVector = normalVector * 2.0 - 1.0;
	normalVector = normalize(normalVector);
	normalVector.xy *= 0.5;

	vec3 localView = normalize(viewTangetSpace);
	float fresnel = dot(normalVector, localView);
	if ( abovewater == 0 ) {
		fresnel = -fresnel;
	}
	fresnel *= 1.0 - fogDist;
	float fresnelTerm = 1.0 - fresnel;
	fresnelTerm *= fresnelTerm;
	fresnelTerm *= fresnelTerm;
	fresnelTerm = fresnelTerm * 0.9 + 0.1;

	vec2 projCoord = viewCoords.xy / viewCoords.q;
	projCoord = (projCoord + 1.0) * 0.5;
	if ( abovewater == 1 ) {
		projCoord.x = 1.0 - projCoord.x;
	}

    projCoord += (vnormal + dudvColor.xy * 0.5 + normalVector.xy * 0.2);
	projCoord = clamp(projCoord, 0.001, 0.999);

	vec4 reflectionColor = texture2D(reflection, projCoord);
	if ( abovewater == 0 ) {
		reflectionColor *= vec4(0.5,0.6,0.7,1.0);
	}

	vec4 waterColorNew = mix(waterColor,waterColorEnd,fresnelTerm);
	vec4 endColor = mix(waterColorNew,reflectionColor,fresnelTerm);

	float foamVal = (vVertex.y-vVertex.w) / (amplitude * 2.0);
	foamVal = clamp(foamVal,0.0,1.0);
	vec4 foamTex = texture2D(foamMap, foamCoords + vnormal * 0.6 + normalVector.xy * 0.05);
	float normLength = length(vnormal*5.0);
	foamVal *= 1.0-normLength;
	foamVal *= foamTex.a;
	endColor = mix(endColor,foamTex,clamp(foamVal,0.0,0.95));

	gl_FragColor = mix(endColor,reflectionColor,fogDist);
}