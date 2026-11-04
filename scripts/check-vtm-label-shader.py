#!/usr/bin/env python3
"""Optional Linux/EGL pixel check for native label fading; no APK or Android emulator."""
import os
os.environ.setdefault("MESA_SHADER_CACHE_DISABLE","true")
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
source=(Path(__file__).resolve().parents[1]/'app/src/main/assets/shaders/bcycle_labels.glsl').read_text().split('$$')
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

orders=[(0,1,2),(1,2,0),(2,0,1),(4,5,7)]
for order in orders:
 program=gl('glCreateProgram',U,[])()
 for shader in shaders:gl('glAttachShader',None,[U,U])(program,shader)
 for name,slot in zip([b'a_pos',b'a_tex_coord',b'a_fade'],order):gl('glBindAttribLocation',None,[U,U,C.c_char_p])(program,slot,name)
 gl('glLinkProgram',None,[U])(program);ok=I();gl('glGetProgramiv',None,[U,U,C.POINTER(I)])(program,0x8B82,C.byref(ok));assert ok.value
 gl('glUseProgram',None,[U])(program)
 def uniform(name):return gl('glGetUniformLocation',I,[U,C.c_char_p])(program,name.encode())
 def attrib(name):return gl('glGetAttribLocation',I,[U,C.c_char_p])(program,name.encode())
 assert tuple(attrib(name) for name in ['a_pos','a_tex_coord','a_fade'])==order
 identity=(F*16)(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1)
 projection=(F*16)(1/32,0,0,0,0,1/32,0,0,0,0,1,0,0,0,0,1)
 gl('glUniformMatrix4fv',None,[I,I,U,C.POINTER(F)])(uniform('u_mv'),1,0,identity)
 gl('glUniformMatrix4fv',None,[I,I,U,C.POINTER(F)])(uniform('u_proj'),1,0,projection)
 gl('glUniform1f',None,[I,F])(uniform('u_scale'),1);gl('glUniform1f',None,[I,F])(uniform('u_coord_scale'),8)
 gl('glUniform2f',None,[I,F,F])(uniform('u_div'),.25,.25)
 texture=U();gl('glGenTextures',None,[I,C.POINTER(U)])(1,C.byref(texture));gl('glBindTexture',None,[U,U])(0x0DE1,texture)
 for parameter in [0x2800,0x2801]:gl('glTexParameteri',None,[U,U,I])(0x0DE1,parameter,0x2600)
 for parameter in [0x2802,0x2803]:gl('glTexParameteri',None,[U,U,I])(0x0DE1,parameter,0x812F)
 # Transparent origin, opaque glyph at the opposite corner: disabled UVs must be visible to the test.
 pixels=(C.c_ubyte*16)(*([0]*12+[255]*4))
 gl('glTexImage2D',None,[U,I,I,I,I,I,U,U,P])(0x0DE1,0,0x1908,2,2,0,0x1908,0x1401,C.cast(pixels,P))
 vertices=(C.c_short*16)(0,0,-16,-16,0,0,-16,16,0,0,16,-16,0,0,16,16)
 uv=(C.c_short*8)(*([3,3]*4))
 pos,tex,fade=order
 gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(pos,4,0x1402,0,0,C.cast(vertices,P))
 gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(tex,2,0x1402,0,0,C.cast(uv,P))
 gl('glVertexAttrib2f',None,[U,F,F])(tex,0,0)
 gl('glViewport',None,[I,I,I,I])(0,0,64,64)
 def sample(start,target,time,legacy=False):
  for slot in range(8):gl('glDisableVertexAttribArray',None,[U])(slot)
  active=({slot for slot in [pos,tex] if slot in (0,1)}|{fade}) if legacy else set(order)
  for slot in active:gl('glEnableVertexAttribArray',None,[U])(slot)
  data=(C.c_short*16)(*([start,target,0,240]*4))
  gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(fade,4,0x1402,0,0,C.cast(data,P))
  gl('glUniform1f',None,[I,F])(uniform('u_time'),time)
  gl('glClearColor',None,[F,F,F,F])(0,0,0,0);gl('glClear',None,[U])(0x4000);gl('glDrawArrays',None,[U,I,I])(5,0,4)
  pixel=(C.c_ubyte*4)();gl('glReadPixels',None,[I,I,I,I,U,U,P])(32,32,1,1,0x1908,0x1401,pixel);return list(pixel)
 if pos>1 or tex>1:assert sample(1000,1000,240,True)==[0,0,0,0],('legacy did not reproduce',order)
 for args,expected in [((0,1000,0),0),((0,1000,120),128),((0,1000,240),255),((1000,0,0),255),((1000,0,120),128),((1000,0,240),0),((400,0,0),102)]:
  pixel=sample(*args);assert all(abs(channel-expected)<=2 for channel in pixel),(order,args,pixel,expected)
 assert gl('glGetError',U,[])()==0
 gl('glUseProgram',None,[U])(0);gl('glDeleteProgram',None,[U])(program)
 gl('glDeleteTextures',None,[I,C.POINTER(U)])(1,C.byref(texture))
print('GLES: missing glyphs reproduced with legacy slot handling; textured glyph/fade pixels verified for four forced attribute layouts.')
egl('eglMakeCurrent',U,[P,P,P,P])(display,None,None,None);egl('eglDestroyContext',U,[P,P])(display,context);egl('eglDestroySurface',U,[P,P])(display,surface);egl('eglTerminate',U,[P])(display)
