#!/usr/bin/env python3
"""Keep typeface families in one place; this check needs only the Python standard library."""
import pathlib
import re

root = pathlib.Path(__file__).resolve().parent.parent
sources = root / 'app/src/main/java/de/bcycle/app'
violations = []
for path in sources.glob('*.kt'):
    if path.name == 'BicycleDesign.kt':
        continue
    for number, line in enumerate(path.read_text().splitlines(), 1):
        if re.search(r'getFont\(R\.font\.|Typeface\.create\(|Typeface\.DEFAULT', line):
            violations.append(f'{path.relative_to(root)}:{number}: {line.strip()}')
assert not violations, 'Use a central BicycleDesign typeface role:\n' + '\n'.join(violations)
assert (root / 'app/src/main/res/font/league_gothic.ttf').is_file()
assert (root / 'app/src/main/assets/League-Gothic-OFL.txt').is_file()
assert not (root / 'app/src/main/res/font/bebas_neue.ttf').exists()
print('Typeface families are central; League Gothic and its license are present.')
