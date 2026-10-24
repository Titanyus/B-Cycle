#!/usr/bin/env python3
"""Produce the offline input designer with the same palettes and licensed font as the UI atlas."""
import base64
import html
import importlib.util
import json
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('ui_catalog',ROOT/'scripts/build-ui-catalog.py')
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
result=(ROOT/'docs/previews/input-designer.template.html').read_text()
result=result.replace('__THEMES__',json.dumps(module.manifest()['themes'],ensure_ascii=False).replace('</','<\\/'))
result=result.replace('__CHROMATIC_ENGINE__',(ROOT/'docs/previews/chromatic-color.js').read_text())
result=result.replace('__FONT__',base64.b64encode((ROOT/'app/src/main/res/font/league_gothic.ttf').read_bytes()).decode())
result=result.replace('__LICENSE__',html.escape((ROOT/'app/src/main/assets/League-Gothic-OFL.txt').read_text()))
target=ROOT/'docs/previews/input-designer.html'
if '--check' in sys.argv:
    assert target.exists() and target.read_text()==result,'Input designer is stale; run scripts/build-input-designer.py'
    print('Input designer matches app palettes and font.')
else:
    target.write_text(result)
    print(target.relative_to(ROOT))
