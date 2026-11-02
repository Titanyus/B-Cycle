#!/usr/bin/env python3
"""Optional Mesa/GLES check of actual GPS shader and texture/accuracy blending; no APK build."""
import runpy
from pathlib import Path
# Reuse the existing real EGL context, ctypes GL bindings and route pixel checks.
h=runpy.run_path(str(Path(__file__).with_name('check-vtm-route-shader.py')),init_globals={'keep_context':True})
C,P,I,U,F,gl,pixel=(h[k] for k in ['C','P','I','U','F','gl','pixel'])
program=gl('glCreateProgram',U,[])()
parts=(Path(__file__).resolve().parents[1]/'app/src/main/assets/shaders/bcycle_gps.glsl').read_text().split('$$')
for kind,text in zip([0x8B31,0x8B30],parts):
 shader=gl('glCreateShader',U,[U])(kind);source=C.c_char_p(('#define GLES\n'+text).encode())
 gl('glShaderSource',None,[U,I,C.POINTER(C.c_char_p),C.POINTER(I)])(shader,1,C.byref(source),None)
 gl('glCompileShader',None,[U])(shader);ok=I();gl('glGetShaderiv',None,[U,U,C.POINTER(I)])(shader,0x8B81,C.byref(ok))
 if not ok.value:
  log=C.create_string_buffer(4096);gl('glGetShaderInfoLog',None,[U,I,P,P])(shader,4096,None,log);raise AssertionError(log.value)
 gl('glAttachShader',None,[U,U])(program,shader)
gl('glLinkProgram',None,[U])(program);ok=I();gl('glGetProgramiv',None,[U,U,C.POINTER(I)])(program,0x8B82,C.byref(ok))
if not ok.value:
 log=C.create_string_buffer(4096);gl('glGetProgramInfoLog',None,[U,I,P,P])(program,4096,None,log);raise AssertionError(log.value)
gl('glUseProgram',None,[U])(program)
def uniform(name):return gl('glGetUniformLocation',I,[U,C.c_char_p])(program,name.encode())
def one(name,value):gl('glUniform1f',None,[I,F])(uniform(name),value)
def two(name,x,y):gl('glUniform2f',None,[I,F,F])(uniform(name),x,y)
def mode(value):gl('glUniform1i',None,[I,I])(uniform('u_mode'),value)
gl('glUniformMatrix4fv',None,[I,I,U,C.POINTER(F)])(uniform('u_mvp'),1,0,(F*16)(1/32,0,0,0,0,1/32,0,0,0,0,1,0,0,0,0,1))
quad=(F*8)(-1,-1,1,-1,-1,1,1,1);attrib=gl('glGetAttribLocation',I,[U,C.c_char_p])(program,b'a_pos')
gl('glEnableVertexAttribArray',None,[U])(attrib);gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(attrib,2,0x1406,0,0,C.cast(quad,P))
tex=U();gl('glGenTextures',None,[I,C.POINTER(U)])(1,C.byref(tex));gl('glBindTexture',None,[U,U])(0x0DE1,tex)
for name in [0x2800,0x2801]:gl('glTexParameteri',None,[U,U,I])(0x0DE1,name,0x2600)
colors=(C.c_ubyte*16)(255,0,0,255,0,255,0,255,0,0,255,255,255,255,255,255)
gl('glTexImage2D',None,[U,I,I,I,I,I,U,U,P])(0x0DE1,0,0x1908,2,2,0,0x1908,0x1401,C.cast(colors,P))
gl('glUniform1i',None,[I,I])(uniform('u_tex'),0)
two('u_center',0,0);two('u_size',8,8);two('u_rotation',1,0);mode(0)
gl('glClearColor',None,[F,F,F,F])(0,0,0,0);gl('glClear',None,[U])(0x4000);gl('glDrawArrays',None,[U,I,I])(5,0,4)
assert pixel(28,28)==[255,0,0,255],pixel(28,28)
assert pixel(36,28)==[0,255,0,255],pixel(36,28)
# Move and rotate an existing texture, without changing or re-uploading its pixels.
two('u_center',8,0);two('u_rotation',0,1);gl('glClear',None,[U])(0x4000);gl('glDrawArrays',None,[U,I,I])(5,0,4)
assert pixel(44,28)==[255,0,0,255],pixel(44,28)
assert pixel(28,28)==[0,0,0,0],pixel(28,28)
# Accuracy circle is a quad, so large circles do not depend on GPU point-size limits.
two('u_center',0,0);two('u_size',14,14);two('u_rotation',1,0);one('u_radius',12);one('u_stroke',1);mode(1)
gl('glClear',None,[U])(0x4000);gl('glDrawArrays',None,[U,I,I])(5,0,4)
inside=pixel(32,32);ring=pixel(43,32);outside=pixel(47,32)
assert abs(inside[3]-30)<=1 and abs(inside[2]-29)<=1,inside
assert ring[3]>inside[3] and ring[0]==0,ring
assert outside==[0,0,0,0],outside
assert gl('glGetError',U,[])()==0
print('GPS GLES shader linked; texture orientation/motion/rotation and accuracy alpha verified:',inside,ring,outside)

h['egl']('eglMakeCurrent',U,[P,P,P,P])(h['display'],None,None,None)
h['egl']('eglDestroyContext',U,[P,P])(h['display'],h['context'])
h['egl']('eglDestroySurface',U,[P,P])(h['display'],h['surface'])
h['egl']('eglTerminate',U,[P])(h['display'])
