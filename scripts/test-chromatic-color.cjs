// No packages required. Run: node scripts/test-chromatic-color.cjs
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const root=path.resolve(__dirname,'..'),context=vm.createContext({});
vm.runInContext(fs.readFileSync(path.join(root,'docs/previews/chromatic-color.js'),'utf8')+'\nglobalThis.engine=ChromaticColor;',context);
const engine=context.engine;
const html=fs.readFileSync(path.join(root,'docs/previews/input-designer.html'),'utf8');
const themes=JSON.parse(html.match(/const THEMES=(.*);/)[1]);
const bases=[...themes.map(t=>t.primary),'#000000','#ffffff','#888888','#ff0000','#00ff00','#0000ff','#00ffff','#ff00ff','#ffff00'];
const channels=c=>[1,3,5].map(i=>parseInt(c.slice(i,i+2),16));
const quantizedLightnessContains=(color,L)=>{
 const rgb=channels(color),asHex=c=>'#'+c.map(x=>x.toString(16).padStart(2,'0')).join('');
 const lo=engine.toLab(asHex(rgb.map(x=>Math.max(0,x-1))))[0];
 const hi=engine.toLab(asHex(rgb.map(x=>Math.min(255,x+1))))[0];
 return L>=lo-.011&&L<=hi+.011;
};
const distance=(a,b)=>Math.hypot(...a.map((x,i)=>x-b[i]));
const hue=c=>{const [,a,b]=engine.toLab(c);return Math.atan2(b,a)*180/Math.PI;};
const delta=(a,b)=>(b-a+540)%360-180;
assert(Math.abs(engine.toLab('#ff0000')[0]-.62795536)<1e-7,'D65 OKLab red reference');
for(const base of bases){
 const original=engine.toLab(base);
 for(const t of [.000001,.00001,.0001]){const p=engine.palette(base,t,t,1);for(const key of ['light','dark'])assert(distance(engine.toLab(p[key]),original)<.003,'No cusp jump immediately after 0 %');}
 for(const k of [0,.5,1]){
  const zero=engine.palette(base,0,0,k),end=engine.palette(base,1,1,k);
  assert.equal(zero.base,base);assert.equal(zero.light,base);assert.equal(zero.dark,base);
  assert.equal(end.light,'#ffffff');assert.equal(end.dark,'#000000');
  let previousLight=original,previousDark=original,previousDarkHex=base,previousLightHex=base;
  for(let i=1;i<=100;i++){
   const colors=engine.palette(base,i/100,i/100,k);
   for(const color of Object.values(colors))assert.match(color,/^#[0-9a-f]{6}$/i);
   const light=engine.toLab(colors.light),dark=engine.toLab(colors.dark);
   assert(light[0]>=previousLight[0]-.002,'Highlight lightness must rise');
   assert(dark[0]<=previousDark[0]+.002,'Shadow lightness must fall');
   assert(distance(light,previousLight)<.04 || Math.max(light[0],previousLight[0])<.1 && distance(channels(colors.light),channels(previousLightHex))<=2,`No abrupt highlight jumps ${base} ${i} ${k} ${previousLightHex} ${colors.light} ${distance(light,previousLight)}`);
   // A single sRGB code step at black is ~0.05 OKLab units: allow that quantization only.
   assert(distance(dark,previousDark)<.04 || Math.max(dark[0],previousDark[0])<.1 && distance(channels(colors.dark),channels(previousDarkHex))<=2,'No abrupt shadow jumps');
   // Fixed target L: allow the 0.01 gamut tolerance and final HEX quantization.
   assert(quantizedLightnessContains(colors.light,original[0]+(1-original[0])*i/100),'Chromatic strength preserves highlight L');
   assert(quantizedLightnessContains(colors.dark,original[0]*(1-i/100)),'Chromatic strength preserves shadow L');
   previousLight=light;previousDark=dark;previousDarkHex=colors.dark;previousLightHex=colors.light;
  }
 }
 const a=engine.palette(base,.25,.7,.5),b=engine.palette(base,.8,.7,.5),c=engine.palette(base,.25,.1,.5);
 assert.equal(a.dark,b.dark);assert.equal(a.light,c.light);
 if(/^#([0-9a-f]{2})\1\1$/i.test(base)){
  for(const color of Object.values(engine.palette(base,.4,.6,1))){const [r,g,b]=[1,3,5].map(i=>parseInt(color.slice(i,i+2),16));assert.equal(r,g);assert.equal(g,b);}
 }
}
for(const [base,lightDirection,darkDirection] of [['#ff0000',1,-1],['#0000ff',-1,1]]){
 const p=engine.palette(base,.5,.5,1);
 assert(delta(hue(base),hue(p.light))*lightDirection>0,'Natural highlight hue direction');
 assert(delta(hue(base),hue(p.dark))*darkDirection>0,'Cool shadow hue direction');
 const [,a,b]=engine.toLab(base),[,la,lb]=engine.toLab(engine.palette(base,.5,.5,0).light);
 assert(Math.hypot(la,lb)<Math.hypot(a,b)*.5,'Highlights desaturate nonlinearly');
}
// Hue continuity, including the 360°/0° boundary, without sector switches.
let previous;
for(let h=0;h<=360;h++){
 const f=n=>{const k=(n+h/60)%6;return Math.round(255*(1-Math.max(0,Math.min(k,4-k,1))));};
 const base='#'+[f(5),f(3),f(1)].map(v=>v.toString(16).padStart(2,'0')).join('');
 const p=engine.palette(base,.45,.45,1);
 if(previous)for(const key of ['light','dark'])assert(distance(engine.toLab(p[key]),engine.toLab(previous[key]))<.025,'Continuous hue field');
 previous=p;
}
console.log(`Chromatic engine: ${themes.length} themes, endpoints, monotonic lightness, independence, neutrals, hue direction and continuity passed.`);
