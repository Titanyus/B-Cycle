#ifdef GLES
precision highp float;
precision mediump int;
#endif
uniform mat4 u_mvp;
uniform vec2 u_center;
uniform vec2 u_size;
uniform vec2 u_rotation;
attribute vec2 a_pos;
varying vec2 v_uv;
varying vec2 v_offset;
void main() {
    vec2 offset = a_pos * u_size;
    vec2 rotated = vec2(u_rotation.x * offset.x - u_rotation.y * offset.y,
                        u_rotation.y * offset.x + u_rotation.x * offset.y);
    gl_Position = u_mvp * vec4(u_center + rotated, 0.0, 1.0);
    v_uv = a_pos * 0.5 + 0.5;
    v_offset = offset;
}
$$
#ifdef GLES
precision highp float;
precision mediump int;
#endif
uniform sampler2D u_tex;
uniform int u_mode;
uniform float u_radius;
uniform float u_stroke;
varying vec2 v_uv;
varying vec2 v_offset;
void main() {
    if (u_mode == 0) {
        gl_FragColor = texture2D(u_tex, v_uv);
    } else {
        float distance = length(v_offset);
        float fill = clamp(u_radius - distance + 0.5, 0.0, 1.0) * (30.0 / 255.0);
        float stroke = clamp(u_stroke * 0.5 + 0.5 - abs(distance - u_radius), 0.0, 1.0) * (100.0 / 255.0);
        float alpha = stroke + fill * (1.0 - stroke);
        gl_FragColor = vec4(vec3(0.0, 100.0 / 255.0, 245.0 / 255.0) * alpha, alpha);
    }
}
