/* Pure sRGB/OKLCH math, deliberately independent of DOM/Canvas for a Kotlin port.
 * Björn Ottosson's D65 OKLab matrices; all calculations use doubles and radians.
 * Gamut mapping reduces chroma at fixed target lightness/hue. A small OKLab
 * clipping tolerance (0.01) avoids discontinuities at the saturated-blue cusp.
 */
const ChromaticColor=(()=>{
 const clamp=x=>Math.max(0,Math.min(1,x));
 const linear=x=>x<=.04045?x/12.92:((x+.055)/1.055)**2.4;
 const encoded=x=>x<=.0031308?12.92*x:1.055*x**(1/2.4)-.055;
 function toLab(color){
  const [r,g,b]=[1,3,5].map(i=>linear(parseInt(color.slice(i,i+2),16)/255));
  return labLinear(r,g,b);
 }
 function labLinear(r,g,b){
  const l=Math.cbrt(.4122214708*r+.5363325363*g+.0514459929*b);
  const m=Math.cbrt(.2119034982*r+.6806995451*g+.1073969566*b);
  const s=Math.cbrt(.0883024619*r+.2817188376*g+.6299787005*b);
  return [.2104542553*l+.793617785*m-.0040720468*s,
   1.9779984951*l-2.428592205*m+.4505937099*s,
   .0259040371*l+.7827717662*m-.808675766*s];
 }
 function toLinear(L,C,h){
  const a=C*Math.cos(h),b=C*Math.sin(h);
  const l=(L+.3963377774*a+.2158037573*b)**3;
  const m=(L-.1055613458*a-.0638541728*b)**3;
  const s=(L-.0894841775*a-1.291485548*b)**3;
  return [4.0767416621*l-3.3077115913*m+.2309699292*s,
   -1.2684380046*l+2.6097574011*m-.3413193965*s,
   -.0041960863*l-.7034186147*m+1.707614701*s];
 }
 function inGamut(channels){return channels.every(x=>x>=-1e-7&&x<=1+1e-7);}
 // Local-MINDE-style mapping: only accept clipping within 0.01 OKLab distance.
 // Strict radial mapping alone can jump at pure blue's non-convex gamut cusp.
 function acceptable(channels,L,C,h){
  if(inGamut(channels))return true;
  const clipped=labLinear(...channels.map(clamp));
  return Math.hypot(clipped[0]-L,clipped[1]-C*Math.cos(h),clipped[2]-C*Math.sin(h))<=.01;
 }
 function toHex(L,C,h){
  let channels=toLinear(L,C,h);
  if(!acceptable(channels,L,C,h)){
   let lo=0,hi=C;
   for(let i=0;i<20;i++){const mid=(lo+hi)/2;if(acceptable(toLinear(L,mid,h),L,mid,h))lo=mid;else hi=mid;}
   channels=toLinear(L,lo,h);
  }
  // Remaining clipping is limited by the perceptual tolerance above.
  return '#'+channels.map(x=>Math.round(clamp(encoded(clamp(x)))*255).toString(16).padStart(2,'0')).join('');
 }
 function tone(base,strength,chromatic,highlight){
  const t=clamp(strength),k=clamp(chromatic);
  if(t===0)return base;
  if(t===1)return highlight?'#ffffff':'#000000';
  const [L,a,b]=toLab(base),C=Math.hypot(a,b),h=Math.atan2(b,a);
  // Smooth periodic hue field: highlights toward yellow (100°), shadows violet (300°).
  // No hue sectors or +/-180° wrap decisions; neutrals cannot pick up a color cast.
  const shift=k*Math.sin(t*Math.PI/2)*Math.min(1,C/.04)*
   Math.sin((highlight?100:300)*Math.PI/180-h)*(highlight?32:45)*Math.PI/180;
  const nextL=highlight?L+(1-L)*t:L*(1-t);
  const nextC=C<1e-7?0:C*(1-t)**(highlight?1.5:1.15);
  return toHex(nextL,nextC,h+shift);
 }
 function palette(base,lighten,darken,chromatic){
  return {base,light:tone(base,lighten,chromatic,true),dark:tone(base,darken,chromatic,false)};
 }
 return {palette,toLab,version:'oklch-chromatic-v1'};
})();
