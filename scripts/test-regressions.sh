#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

python3 scripts/check-ui-typography.py
python3 scripts/build-ui-catalog.py --check

# Deliberately bounded: compatibility, backups, HRV storage, metadata/status and settings UI.
# Optional Gradle flags such as --offline are forwarded. No release or APK task.
classes=(
  RecordingFieldsTest RecordingStoreBaselineTest RecordingResultTest
  HrvStoreTest HrvHistorySummaryTest HrvQuestionnaireTest HrvSettingsTest HrvEntryTest HrvControllerTest TypographyRolesTest
  RideProfileTypeTest RideMetadataSnapshotTest ActivityStatusIndexTest ActivityServiceTest
  RideDirectoryBackupTest SettingsDesignMigrationTest SettingsSectionsTest
)
filters=()
for name in "${classes[@]}"; do filters+=(--tests "de.bcycle.app.$name"); done
rm -f app/build/test-results/testDebugUnitTest/TEST-*.xml hrv-core/build/test-results/test/TEST-*.xml
./gradlew "$@" --no-daemon :hrv-core:test :app:testDebugUnitTest "${filters[@]}" --console=plain

# A misspelled filter must not silently leave part of the promised checks untested.
python3 - "${classes[@]}" <<'PY'
import pathlib, sys, xml.etree.ElementTree as ET
total = 0
for name in sys.argv[1:]:
    path = pathlib.Path(f'app/build/test-results/testDebugUnitTest/TEST-de.bcycle.app.{name}.xml')
    row = ET.parse(path).getroot()
    assert int(row.get('tests', '0')) > 0, f'No tests executed: {name}'
    assert all(int(row.get(key, '0')) == 0 for key in ('failures', 'errors', 'skipped')), name
    total += int(row.get('tests'))
core = list(pathlib.Path('hrv-core/build/test-results/test').glob('TEST-*.xml'))
assert core, 'HRV core tests did not run'
for path in core:
    row = ET.parse(path).getroot()
    assert int(row.get('tests', '0')) > 0, path
    assert all(int(row.get(key, '0')) == 0 for key in ('failures', 'errors', 'skipped')), path
    total += int(row.get('tests'))
print(f'{total} regression tests verified; no APK built.')
PY
