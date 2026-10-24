#!/usr/bin/env python3
"""Build the offline UI reference from Kotlin tokens, current palettes, icons and licensed fonts."""
import argparse
import ast
import base64
import hashlib
import html
import json
import operator
import pathlib
import re
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
APP = ROOT / 'app/src/main'
OUT = ROOT / 'docs/previews/ui-catalog.html'
TEMPLATE = ROOT / 'docs/previews/ui-catalog.template.html'


def manifest():
    path = APP / 'java/de/bcycle/app/BicycleDesign.kt'
    values, tokens = {}, {}
    operations = {ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul, ast.Div: operator.truediv}

    def evaluate(node):
        if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
            return node.value
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, ast.USub):
            return -evaluate(node.operand)
        if isinstance(node, ast.Name):
            return values[node.id]
        if isinstance(node, ast.BinOp) and type(node.op) in operations:
            return operations[type(node.op)](evaluate(node.left), evaluate(node.right))
        raise ValueError('Not a numeric design token')

    for number, line in enumerate(path.read_text().splitlines(), 1):
        match = re.search(r'const val (\w+)\s*=\s*(.+)', line)
        if not match:
            continue
        name, expression = match.groups()
        expression = expression.split('//', 1)[0]
        try:
            value = evaluate(ast.parse(re.sub(r'(?<=\d)[fFL]\b', '', expression.strip()), mode='eval').body)
        except (ValueError, KeyError, SyntaxError):
            continue
        values[name] = value
        tokens[name] = {'value': value, 'source': f'app/src/main/java/de/bcycle/app/BicycleDesign.kt:{number}'}
    required = ['controlGap', 'choiceTextGap', 'pageContentGap', 'sectionContentGap', 'sectionEndGap', 'horizontalPadding',
                'inputHintAlpha', 'fieldLabelGap', 'fieldFeedbackGap', 'sliderScaleLabelAlpha', 'sliderScaleLabelGap', 'sliderLabelGap', 'sliderQuestionGap', 'sectionDividerGap', 'headerHeight', 'touchSize', 'bodySp', 'titleSp', 'headingSp',
                'settingsMetricSp', 'controlBorder', 'controlBorderAlpha', 'inputRadius', 'switchWidth',
                'switchHeight', 'switchThumbDiameter', 'footerHeight', 'iconSize', 'sectionHeaderMinHeight']
    assert all(name in tokens for name in required), 'Design token missing; update the catalog'
    theme_path = APP / 'java/de/bcycle/app/BicycleTheme.kt'
    themes = []
    for row in re.findall(r'family\("([^"]+)","([^"]+)","([^"]+)","([^"]+)","([^"]+)","([^"]+)","([^"]+)"\)', theme_path.read_text()):
        key, name, primary, secondary, tertiary, dark_s, dark_t = row
        for dark in (False, True):
            themes.append(dict(id=f'{key}-{"dark" if dark else "color"}', name=name + (' DARK' if dark else ''),
                               primary='#' + primary, secondary='#' + (dark_s if dark else secondary),
                               tertiary='#' + (dark_t if dark else tertiary), foreground='#FFFFFF', accent='#' + primary))
    assert len(themes) == 14
    icons = {}
    ns = '{http://schemas.android.com/apk/res/android}'
    for name in ['heart', 'chevron_down', 'chevron_left', 'chevron_right', 'calendar_days', 'check', 'settings', 'route', 'bike', 'play', 'square', 'eye', 'trash_2', 'plus', 'info']:
        path = APP / f'res/drawable/ic_lucide_{name}.xml'
        if not path.exists():
            continue
        root = ET.parse(path).getroot()
        paths = []
        for child in root.iter('path'):
            paths.append('<path d="' + html.escape(child.get(ns + 'pathData'), quote=True) + '"/>')
        icons[name] = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + ''.join(paths) + '</svg>'
    tracked = [APP / 'java/de/bcycle/app' / file for file in ['BicycleDesign.kt', 'BicycleTheme.kt', 'BicycleContentSpacing.kt', 'BicycleForm.kt', 'HrvQuestionnaireView.kt']]
    tracked.append(APP / 'res/font/league_gothic.ttf')
    fingerprint = hashlib.sha256(b''.join(p.read_bytes() for p in tracked)).hexdigest()[:16]
    return dict(tokens=tokens, themes=themes, icons=icons, fingerprint=fingerprint,
                exceptions=[dict(area='Schriftfamilien', value='Zentrale Rollen in BicycleDesign', detail='League Gothic für schmale Eingaben, Titel und Messwerte. System-/kompakte Systemschrift und 900 Black bleiben benannte Beschriftungsrollen.'),
                            dict(area='Großschreibung', value='Fenstertitel und Sticky-Kategorieheader erzwungen', detail='Eingaben und gespeicherte Namen werden nicht in Großbuchstaben umgewandelt.'),
                            dict(area='Gruppenabstand / Nähe von Label und Control', value='Kein durchgängiger Token', detail='Slider-Fragen verwenden 32 dp Gruppenabstand, 0 dp oberhalb des Sliders und -8 dp zur Skala. Der allgemeine Formular-Abstandsentwurf bleibt separat.'),
                            dict(area='Abschnittstrenner', value=f'{values["sectionDividerGap"]} dp je Seite', detail='Dokumentation auf den bestehenden Codewert abgeglichen; kein App-Abstand geändert.')])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    result = TEMPLATE.read_text()
    replacements = {
        '__DESIGN_JSON__': json.dumps(manifest(), ensure_ascii=False).replace('</', '<\\/'),
        '__CHROME_ENGINE__': (ROOT/'docs/previews/chromatic-color.js').read_text(),
        '__INPUT_DESIGN__': (ROOT/'docs/designs/input-design.json').read_text(),
        '__BEBAS_FONT__': base64.b64encode((ROOT / 'docs/previews/fonts/BebasNeue-Regular.ttf').read_bytes()).decode(),
        '__LEAGUE_FONT__': base64.b64encode((APP / 'res/font/league_gothic.ttf').read_bytes()).decode(),
        '__LEAGUE_LICENSE__': html.escape((APP / 'assets/League-Gothic-OFL.txt').read_text()),
        '__CONDENSED_FONT__': base64.b64encode((ROOT / 'docs/previews/fonts/RobotoCondensed-latin.woff').read_bytes()).decode(),
        '__FONT_LICENSE__': html.escape((ROOT / 'docs/previews/fonts/RobotoCondensed-OFL.txt').read_text()),
        '__BEBAS_LICENSE__': html.escape((ROOT / 'docs/previews/fonts/Bebas-Neue-OFL.txt').read_text()),
        '__ICON_LICENSE__': html.escape((APP / 'assets/Lucide-LICENSE.txt').read_text()),
    }
    for key, value in replacements.items():
        result = result.replace(key, value)
    if args.check:
        assert OUT.exists() and OUT.read_text() == result, 'UI catalog is stale: python3 scripts/build-ui-catalog.py'
        print('UI catalog matches the current app tokens and source fingerprint.')
    else:
        OUT.write_text(result)
        print(OUT.relative_to(ROOT))


if __name__ == '__main__':
    main()
