#!/usr/bin/env python3
"""Optional Linux/Mesa GLES check of the actual route shader; does not build an APK."""
import ctypes as C
from pathlib import Path
P=C.c_void_p;I=C.c_int;U=C.c_uint;F=C.c_float
lib=C.CDLL('libEGL.so.1')
def egl(name,result,args):
 f=getattr(lib,name);f.restype=result;f.argtypes=args;return f
getproc=egl('eglGetProcAddress',P,[C.c_char_p])
platform=C.CFUNCTYPE(P,U,P,C.POINTER(I))(getproc(b'eglGetPlatformDisplayEXT'))
display=platform(0x31DD,None,None)
major=I();minor=I()
assert egl('eglInitialize',U,[P,C.POINTER(I),C.POINTER(I)])(display,C.byref(major),C.byref(minor)), 'Surfaceless EGL unavailable'
assert egl('eglBindAPI',U,[U])(0x30A0)
attrs=(I*15)(0x3033,1,0x3040,4,0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3025,0,0x3038)
config=P();count=I()
assert egl('eglChooseConfig',U,[P,C.POINTER(I),C.POINTER(P),I,C.POINTER(I)])(display,attrs,C.byref(config),1,C.byref(count)) and count.value
surface=egl('eglCreatePbufferSurface',P,[P,P,C.POINTER(I)])(display,config,(I*5)(0x3057,64,0x3056,64,0x3038))
context=egl('eglCreateContext',P,[P,P,P,C.POINTER(I)])(display,config,None,(I*3)(0x3098,2,0x3038))
assert context and surface and egl('eglMakeCurrent',U,[P,P,P,P])(display,surface,surface,context)
def gl(name,result,args):
 address=getproc(name.encode());assert address,name
 return C.CFUNCTYPE(result,*args)(address)
source=(Path(__file__).resolve().parents[1] / 'app/src/main/assets/shaders/bcycle_route.glsl').read_text().split('$$')
shaders=[]
for kind,text in zip([0x8B31,0x8B30],source):
 shader=gl('glCreateShader',U,[U])(kind)
 b=('#define GLES\n'+text).encode();s=C.c_char_p(b)
 gl('glShaderSource',None,[U,I,C.POINTER(C.c_char_p),C.POINTER(I)])(shader,1,C.byref(s),None)
 gl('glCompileShader',None,[U])(shader);ok=I()
 gl('glGetShaderiv',None,[U,U,C.POINTER(I)])(shader,0x8B81,C.byref(ok))
 if not ok.value:
  log=C.create_string_buffer(4096);gl('glGetShaderInfoLog',None,[U,I,P,P])(shader,4096,None,log);raise AssertionError(log.value)
 shaders.append(shader)
program=gl('glCreateProgram',U,[])()
for shader in shaders:gl('glAttachShader',None,[U,U])(program,shader)
gl('glLinkProgram',None,[U])(program);ok=I();gl('glGetProgramiv',None,[U,U,C.POINTER(I)])(program,0x8B82,C.byref(ok))
if not ok.value:
 log=C.create_string_buffer(4096);gl('glGetProgramInfoLog',None,[U,I,P,P])(program,4096,None,log);raise AssertionError(log.value)
use=gl('glUseProgram',None,[U]);use(program)
def uniform(name):return gl('glGetUniformLocation',I,[U,C.c_char_p])(program,name.encode())
gl('glUniformMatrix4fv',None,[I,I,U,C.POINTER(F)])(uniform('u_mvp'),1,0,(F*16)(1/256,0,0,0,0,1/256,0,0,0,0,1,0,0,0,0,1))
for name,value in [('u_width',3.5*8/2048),('u_radius',3.5),('u_edge',3),('u_core',2),('u_border',1)]:gl('glUniform1f',None,[I,F])(uniform(name),value)
gl('glUniform1i',None,[I,I])(uniform('u_round'),0)
gl('glUniform4f',None,[I,F,F,F,F])(uniform('u_color'),1,0,0,.4)
vertices=(C.c_short*16)(-24*8,0,0,2049,-24*8,0,2,-2047,24*8,0,0,2049,24*8,0,2,-2047)
attrib=gl('glGetAttribLocation',I,[U,C.c_char_p])(program,b'a_pos')
gl('glEnableVertexAttribArray',None,[U])(attrib)
gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(attrib,4,0x1402,0,0,C.cast(vertices,P))
gl('glViewport',None,[I,I,I,I])(0,0,64,64)
gl('glClearColor',None,[F,F,F,F])(.2,.2,.2,1);gl('glClear',None,[U])(0x4000)
gl('glEnable',None,[U])(0x0BE2);gl('glBlendFunc',None,[U,U])(1,0x0303)
gl('glDrawArrays',None,[U,I,I])(5,0,4)
def pixel(x,y):
 out=(C.c_ubyte*4)();gl('glReadPixels',None,[I,I,I,I,U,U,P])(x,y,1,1,0x1908,0x1401,out);return list(out)
center=pixel(32,32);border=pixel(32,34);outside=pixel(32,38)
assert all(abs(v-w)<=3 for v,w in zip(center,[133,31,31,255])),center
assert max(border[:3])-min(border[:3])<=2 and border[0]>60,border
assert all(abs(v-51)<=1 for v in outside[:3]),outside
gl('glClear',None,[U])(0x4000)
point=(C.c_short*2)(0,0)
gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(attrib,2,0x1402,0,0,C.cast(point,P))
gl('glUniform1i',None,[I,I])(uniform('u_point'),1)
gl('glUniform1f',None,[I,F])(uniform('u_radius'),7.5)
gl('glUniform1f',None,[I,F])(uniform('u_edge'),7)
gl('glUniform1f',None,[I,F])(uniform('u_core'),5)
gl('glUniform4f',None,[I,F,F,F,F])(uniform('u_color'),0,1,0,1)
gl('glDrawArrays',None,[U,I,I])(0,0,1)
assert pixel(32,32)==[0,255,0,255],pixel(32,32)
assert min(pixel(37,32)[:3])>245,pixel(37,32)
assert all(abs(v-51)<=1 for v in pixel(42,32)[:3]),pixel(42,32)
assert gl('glGetError',U,[])()==0
print('GLES shader compiled/linked; 40% opacity core, white border, outside and endpoint marker verified:',center,border,outside)
if not globals().get('keep_context',False):
 egl('eglMakeCurrent',U,[P,P,P,P])(display,None,None,None)
 egl('eglDestroyContext',U,[P,P])(display,context);egl('eglDestroySurface',U,[P,P])(display,surface);egl('eglTerminate',U,[P])(display)
