#!/usr/bin/env python3
"""Compile the checked-in Bright reference into VTM instructions; no network at build/runtime.

The runtime adapter evaluates the original filters, including numeric comparisons. This
compiler emits only paint/layout instructions, ordered exactly like the source layers.
"""
import argparse, colorsys, hashlib, json, math, re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
SOURCE = ASSETS / 'styles/bright.json'
OUTPUT = ASSETS / 'styles/bright-vtm.xml'
REPORT = ROOT / 'docs/bright-vtm-mapping.json'
# Match the actual camera range; extending vector zoom must not silently drop all base paints.
MAX_ZOOM = int(re.search(r'const val maximum=(\d+)',
    (ROOT / 'app/src/main/java/de/bcycle/app/MapZoom.kt').read_text()).group(1))
SPRITES = json.loads((ASSETS / 'styles/bright/sprite.json').read_text())


def rgba(value):
    if isinstance(value, (list, tuple)):
        return tuple(value)
    if value.startswith('#'):
        h = value[1:]
        if len(h) in (3, 4):
            h = ''.join(c * 2 for c in h)
        if len(h) == 6:
            h += 'ff'
        return tuple(int(h[i:i+2], 16) / 255 for i in (0, 2, 4, 6))
    m = re.fullmatch(r'(rgba?|hsla?)\((.*)\)', value)
    if not m:
        raise ValueError(f'Unsupported color: {value}')
    parts = [v.strip() for v in m[2].split(',')]
    alpha = float(parts[3]) if len(parts) == 4 else 1
    if m[1].startswith('hsl'):
        h, s, l = float(parts[0]) / 360, float(parts[1].strip('%')) / 100, float(parts[2].strip('%')) / 100
        return (*colorsys.hls_to_rgb(h, l, s), alpha)
    return (*(float(v) / 255 for v in parts[:3]), alpha)


def interpolate(a, b, t):
    if isinstance(a, bool) or isinstance(a, str) and not a.startswith(('#', 'rgb', 'hsl')):
        return a
    if isinstance(a, str):
        return tuple(x + (y - x) * t for x, y in zip(rgba(a), rgba(b)))
    if isinstance(a, list):
        return [x + (y - x) * t for x, y in zip(a, b)]
    return a + (b - a) * t


def evaluate(value, zoom):
    if not isinstance(value, dict):
        return value
    if set(value) - {'stops', 'base', 'type'} or 'stops' not in value:
        raise ValueError(f'Unsupported expression: {value}')
    stops = value['stops']
    if zoom <= stops[0][0]: return stops[0][1]
    if zoom >= stops[-1][0]: return stops[-1][1]
    for (lo, a), (hi, b) in zip(stops, stops[1:]):
        if lo <= zoom < hi:
            base = value.get('base', 1)
            t = (zoom-lo)/(hi-lo) if base == 1 else (base**(zoom-lo)-1)/(base**(hi-lo)-1)
            return interpolate(a, b, t)
    raise AssertionError(value)


def color(value, opacity=1):
    r, g, b, a = rgba(value)
    return '#' + ''.join(f'{round(max(0,min(1,v))*255):02x}' for v in (a*opacity, r, g, b))


def num(value):
    return f'{value:.5f}'.rstrip('0').rstrip('.') or '0'


def elem(tag, **attrs):
    return ET.Element(tag, {k.replace('_','-'): str(v) for k, v in attrs.items()})


def icon_paths(template):
    fields = re.findall(r'\{([^}]+)\}', template)
    if not fields:
        return [({}, template)] if template in SPRITES else []
    pattern = '^' + re.sub(r'\\\{[^}]+\\\}', '(.+)', re.escape(template)) + '$'
    result = []
    for name in SPRITES:
        m = re.fullmatch(pattern, name)
        if m:
            result.append((dict(zip(fields, m.groups())), name))
    return result


def sprite_path(name, scale=1, rotate=0, opacity=1):
    # Pre-cut local images keep VTM free of runtime HTTP sprite dependencies.
    suffix = f'{name}-s{num(scale)}-r{num(rotate)}-a{num(opacity)}.png'
    return 'styles/bright/icons/' + suffix


def compile_style(style):
    root = elem('rendertheme', xmlns='http://opensciencemap.org/rendertheme', version=1,
                map_background=color(next(l for l in style['layers'] if l['type']=='background')['paint']['background-color']), base_stroke_width=1)
    root.append(ET.Comment('Generated from styles/bright.json by scripts/build-bright-vtm.py. Do not edit. Flat buildings; VTM approximations: docs/vtm-bright-style.md.'))
    report = {
        'source_sha256': hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        'sprite_png_sha256': hashlib.sha256((ASSETS/'styles/bright/sprite.png').read_bytes()).hexdigest(),
        'sprite_json_sha256': hashlib.sha256((ASSETS/'styles/bright/sprite.json').read_bytes()).hexdigest(),
        'maplibre_zoom': 'VTM zoom - 1 (256 vs 512 pixel world)',
        'limitations': [
            f'Paint/size functions sampled at integer VTM zoom 0..{MAX_ZOOM}; no continuous MapLibre interpolation between tile levels.',
            'Native VTM solid-line minimum half-width is 1 px; very thin strokes differ from MapLibre.',
            'Native VTM caps/joins/antialiasing and subpixel dash textures are approximations.',
            'Native labels use existing localized name and Android fonts/collision placement, not remote Noto glyphs.',
            'Pattern translation, shifted water copy and building top not applied; map remains flat.'
        ], 'layers': []}
    needed = set()
    for index, layer in enumerate(style['layers']):
        lid, kind = layer['id'], layer['type']
        entry = {'id': lid, 'type': kind, 'source-layer': layer.get('source-layer'), 'status': 'mapped', 'notes': []}
        approximated = {'fill-antialias','fill-outline-color','line-width','line-dasharray','line-join',
                        'symbol-spacing','text-field','text-font','text-anchor','text-offset','text-padding',
                        'icon-allow-overlap','icon-ignore-placement','icon-optional','text-allow-overlap',
                        'text-ignore-placement','text-optional','icon-padding','text-rotation-alignment'}
        omitted = {'fill-translate','text-halo-blur','text-letter-spacing','text-max-width','text-transform'}
        properties = set(layer.get('paint',{})) | set(layer.get('layout',{}))
        entry['approximated_properties'] = sorted(properties & approximated)
        entry['omitted_properties'] = sorted(properties & omitted)
        known = approximated | omitted | {'background-color','fill-color','fill-opacity','fill-pattern',
                'line-color','line-opacity','text-color','text-halo-color','text-halo-width','text-size',
                'icon-image','icon-opacity','icon-rotate','icon-size','symbol-placement','visibility','line-cap',
                'icon-rotation-alignment'}
        if properties-known: raise ValueError(f'Unreviewed properties in {lid}: {properties-known}')
        report['layers'].append(entry)
        if kind == 'background': continue
        if lid in ('building-top','water-offset'):
            entry['status']='omitted';entry['notes']=['No translated building top (flat view requested).' if lid=='building-top' else 'No translated water copy; base water retained.']
            continue
        if kind not in ('fill','line','symbol'): raise ValueError(f'Unsupported layer {lid}: {kind}')
        parent = elem('m', k='bcycle:bright:'+lid, v='1')
        groups = []
        for vtm_zoom in range(MAX_ZOOM + 1):
            z = vtm_zoom - 1
            if not layer.get('minzoom',0) <= z < layer.get('maxzoom',24): continue
            paint = {k: evaluate(v,z) for k,v in layer.get('paint',{}).items()}
            layout = {k: evaluate(v,z) for k,v in layer.get('layout',{}).items()}
            if layout.get('visibility') == 'none': continue
            instructions = []
            if kind == 'fill':
                attrs = {'id': lid, 'fill': color(paint.get('fill-color','#000'),paint.get('fill-opacity',1))}
                if paint.get('fill-pattern'):
                    name = paint['fill-pattern'];needed.add((name,1,0,1))
                    attrs.update(src='assets:'+sprite_path(name),fill='#ffffffff')
                    entry['notes']=['Native repeating pattern; source translation not supported.']
                if 'fill-outline-color' in paint:
                    attrs.update(stroke=color(paint['fill-outline-color']),stroke_width='0.5')
                instructions.append(elem('area', **attrs))
            elif kind == 'line':
                width=paint.get('line-width',1)
                if width <= 0 or paint.get('line-opacity',1) <= 0: continue
                # VTM extrudes a half-width; fixed avoids its latitude/zoom-dependent line multiplier.
                attrs={'id':lid,'stroke':color(paint.get('line-color','#000'),paint.get('line-opacity',1)),
                       'width':num(width/2),'fix':'true','cap':layout.get('line-cap','butt')}
                if 'line-dasharray' in paint:
                    attrs['dasharray']=','.join(num(v*width) for v in paint['line-dasharray'])
                instructions.append(elem('line', **attrs))
                entry['notes']=['Integer-zoom sampling; VTM line joins/rasterization and minimum width differ.']
                if 'line-dasharray' in paint: entry['notes'].append('Source relative dash lengths converted to pixels; native texture quantizes subpixel dashes.')
            else:
                text=layout.get('text-field')
                size=layout.get('text-size',16)
                template=layout.get('icon-image')
                choices=icon_paths(template) if template else [({},None)]
                if not choices: choices=[({},None)];entry['notes'].append('Sprite not found; text retained.')
                for conditions, name in choices:
                    branch=[]
                    path=None
                    if name:
                        scale=layout.get('icon-size',1);rotate=layout.get('icon-rotate',0);opacity=paint.get('icon-opacity',1)
                        needed.add((name,scale,rotate,opacity));path='assets:'+sprite_path(name,scale,rotate,opacity)
                    line=layout.get('symbol-placement','point')=='line'
                    if text:
                        attrs={'id':lid,'k':'ref' if '{ref}' in text else 'name','size':num(size),
                               'fill':color(paint.get('text-color','#000')), 'stroke':color(paint.get('text-halo-color','#00000000')),
                               'stroke_width':num(paint.get('text-halo-width',0)), 'caption':str(not line).lower(),
                               'priority':str(0 if layer.get('source-layer')=='place' else 8 if layer.get('source-layer')=='transportation_name' else 10)}
                        font=' '.join(layout.get('text-font',[])).lower()
                        attrs['style']='bold' if 'bold' in font else 'italic' if 'italic' in font else 'normal'
                        if layout.get('text-offset'): attrs['dy']=num(-layout['text-offset'][1]*size)
                        if path: attrs['symbol']=path
                        branch.append(elem('text', **attrs))
                    elif path:
                        branch.append(elem('symbol', src=path, repeat=str(line).lower(), rotate=str(line).lower(), billboard=str(not line).lower(),
                                           repeat_gap=num(layout.get('symbol-spacing',200)),repeat_start=0))
                    if conditions:
                        outer=None;inner=None
                        for key,val in conditions.items():
                            node=elem('m',k=key,v=val)
                            if inner is not None:inner.append(node)
                            else:outer=node
                            inner=node
                        inner.extend(branch);instructions.append(outer)
                    else:instructions.extend(branch)
                entry['notes'].append('Native labels: local name, Android font metrics/collision rules; no MapLibre letter spacing/halo blur/text wrapping.')
                if layout.get('text-transform'):entry['notes'].append('Case transform omitted to preserve original names.')
                if layout.get('text-anchor')=='left':entry['notes'].append('Capital star/text centered by native labeling instead of left anchor.')
            payload=b''.join(ET.tostring(e) for e in instructions)
            if not payload: continue
            if groups and groups[-1][2]==payload and groups[-1][1]==vtm_zoom-1:
                groups[-1]=(groups[-1][0],vtm_zoom,payload,instructions)
            else: groups.append((vtm_zoom,vtm_zoom,payload,instructions))
        for low,high,_,instructions in groups:
            rule=elem('m',zoom_min=low,zoom_max=high);rule.extend(instructions);parent.append(rule)
        if len(parent):root.append(parent)
    ET.indent(root, space='    ')
    xml=b'<?xml version="1.0" encoding="UTF-8"?>\n'+ET.tostring(root,encoding='utf-8')+b'\n'
    return xml,report,needed


def render_sprites(needed,check):
    from PIL import Image
    atlas=Image.open(ASSETS/'styles/bright/sprite.png').convert('RGBA')
    import io
    expected=set()
    for name,scale,rotate,opacity in sorted(needed):
        meta=SPRITES[name];x,y,w,h=(meta[k] for k in ('x','y','width','height'))
        image=atlas.crop((x,y,x+w,y+h))
        if rotate:image=image.rotate(-rotate,expand=True)
        if scale!=1:image=image.resize((max(1,round(image.width*scale)),max(1,round(image.height*scale))),Image.Resampling.LANCZOS)
        if opacity!=1:image.putalpha(image.getchannel('A').point(lambda v:round(v*opacity)))
        stream=io.BytesIO();image.save(stream,format='PNG');data=stream.getvalue()
        target=ASSETS/sprite_path(name,scale,rotate,opacity);expected.add(target)
        if check:
            if not target.exists() or target.read_bytes()!=data:raise SystemExit(f'Stale sprite: {target}')
        else:target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
    for target in (ASSETS/'styles/bright/icons').glob('*.png'):
        if target not in expected:
            if check:raise SystemExit(f'Unused sprite: {target}')
            target.unlink()


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    style=json.loads(SOURCE.read_text());xml,report,needed=compile_style(style)
    for target,data in [(OUTPUT,xml),(REPORT,(json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())]:
        if args.check:
            if not target.exists() or target.read_bytes()!=data:raise SystemExit(f'Stale generated mapping: {target}')
        else:target.write_bytes(data)
    render_sprites(needed,args.check)
    print(f'Bright: {len(report["layers"])} source layers, {len(needed)} sprite variants; mapping '+('matches source.' if args.check else 'generated.'))

if __name__=='__main__':main()
