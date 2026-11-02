// Based on VTM line_aa.glsl (Mapsforge-VTM contributors, LGPL-3.0 with project exception).
// One pass keeps the live track's core and white border at the same opacity.
#ifdef GLES
precision highp float;
precision mediump int;
#endif
uniform mat4 u_mvp;
uniform float u_width;
uniform float u_radius;
uniform int u_point;
attribute vec4 a_pos;
varying vec2 v_st;
void main() {
    if (u_point == 1) {
        gl_Position = u_mvp * vec4(a_pos.xy, 0.0, 1.0);
        gl_PointSize = 2.0 * u_radius;
        v_st = vec2(0.0);
    } else {
        gl_Position = u_mvp * vec4(a_pos.xy + u_width * a_pos.zw, 0.0, 1.0);
        v_st = abs(mod(a_pos.zw, 4.0)) - 1.0;
    }
}
$$
#ifdef GLES
precision highp float;
precision mediump int;
#endif
uniform vec4 u_color;
uniform float u_radius;
uniform float u_core;
uniform float u_edge;
uniform float u_border;
uniform int u_round;
uniform int u_point;
varying vec2 v_st;
void main() {
    float distance = (u_point == 1 ? length(2.0 * gl_PointCoord - 1.0) : (u_round == 1 ? length(v_st) : abs(v_st.s))) * u_radius;
    float coverage = clamp(u_edge - distance + 0.5, 0.0, 1.0);
    float core = mix(1.0, clamp(u_core - distance + 0.5, 0.0, 1.0), u_border);
    float alpha = u_color.a * coverage;
    gl_FragColor = vec4(mix(vec3(1.0), u_color.rgb, core) * alpha, alpha);
}
