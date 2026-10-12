"""Generate the exact local Lucide 0.468.0 geometry; no runtime dependency."""
from pathlib import Path
import concurrent.futures, urllib.request, xml.etree.ElementTree as ET, re, sys
VERSION = '0.468.0'
ICON_REFS = {'cloud-check': '70562c1ee1c4fdcf736fe97bc893fb8511927934', 'save-check': '70562c1ee1c4fdcf736fe97bc893fb8511927934'}
ICONS = ['bike', 'log-in', 'log-out', 'file', 'ellipsis', 'ellipsis-vertical', 'eye', 'cloud-upload', 'cloud-alert', 'smartphone', 'trash-2', 'pencil', 'download', 'x', 'arrow-left', 'calendar-days', 'clock', 'chart-no-axes-combined', 'mountain', 'chevron-down', 'circle-help', 'loader-circle', 'bluetooth', 'unplug', 'map', 'save', 'folder-open', 'rotate-ccw', 'plus', 'link', 'info', 'radio', 'map-pinned', 'layout-dashboard', 'route', 'settings']
ICONS.extend(['user-round', 'activity', 'sunrise', 'sunset', 'lightbulb', 'locate-fixed', 'compass', 'maximize', 'eye-closed', 'sun', 'moon', 'gauge', 'zap', 'heart', 'flame', 'repeat-2', 'minus', 'cloud', 'check', 'cloud-check', 'save-check'])
if sys.argv[1:]:
    assert all(name in ICONS for name in sys.argv[1:]), 'Unknown Lucide icon'
    ICONS = sys.argv[1:]
ROOT = Path(__file__).resolve().parents[1]
BASE = f'https://raw.githubusercontent.com/lucide-icons/lucide/{VERSION}/'
def fetch(name):
    ref = ICON_REFS.get(name, VERSION)
    return name, urllib.request.urlopen(f'https://raw.githubusercontent.com/lucide-icons/lucide/{ref}/icons/{name}.svg', timeout=30).read()
def path(node):
    tag = node.tag.split('}')[-1]; a = node.attrib
    if tag == 'path': return a['d']
    if tag in ('polyline', 'polygon'):
        values = re.split(r'[\s,]+', a['points'].strip())
        return 'M'+ ' L'.join(','.join(values[i:i+2]) for i in range(0,len(values),2)) + (' Z' if tag == 'polygon' else '')
    if tag == 'line': return f"M{a['x1']},{a['y1']} L{a['x2']},{a['y2']}"
    if tag == 'circle':
        x,y,r = (float(a[k]) for k in ('cx','cy','r'))
        return f'M{x-r},{y} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0'
    if tag == 'rect':
        x,y,w,h = (float(a.get(k,0)) for k in ('x','y','width','height'))
        r = float(a.get('rx',0))
        return f'M{x+r},{y} H{x+w-r} A{r},{r} 0 0,1 {x+w},{y+r} V{y+h-r} A{r},{r} 0 0,1 {x+w-r},{y+h} H{x+r} A{r},{r} 0 0,1 {x},{y+h-r} V{y+r} A{r},{r} 0 0,1 {x+r},{y} Z'
    raise ValueError(tag)
for name,data in concurrent.futures.ThreadPoolExecutor(max_workers=6).map(fetch, ICONS):
    doc=ET.fromstring(data)
    parts=['<!-- Lucide '+ICON_REFS.get(name, VERSION)+' / '+name+'; ISC + Feather MIT, see assets/Lucide-LICENSE.txt -->', '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">']
    for node in doc:
        p=path(node).replace('&','&amp;').replace('"','&quot;')
        parts.append(f'    <path android:pathData="{p}" android:fillColor="@android:color/transparent" android:strokeColor="?android:attr/textColorPrimary" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" />')
    parts.append('</vector>')
    (ROOT/'app/src/main/res/drawable'/('ic_lucide_'+name.replace('-','_')+'.xml')).write_text('\n'.join(parts)+'\n')
license=urllib.request.urlopen(BASE+'LICENSE', timeout=30).read()
(ROOT/'app/src/main/assets/Lucide-LICENSE.txt').write_bytes(license)
