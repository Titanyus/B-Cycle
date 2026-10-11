# Meilenstein B|Cycle 0.6.20

Tag: `v0.6.20`. Vollständiger Quellstand einschließlich Tests, Ressourcen,
Gradle-Wrapper und Konfiguration, Dokumentation sowie signierter Test-APK mit SHA-256.
Keystore, lokale SDK-/Cacheverzeichnisse, private Daten und Build-Zwischenergebnisse
sind nicht Bestandteil des Meilensteins.

`:app:testDebugUnitTest :app:assembleRelease`: erfolgreich, 211 Tests,
0 Fehler, 0 Fehlschläge, 0 übersprungene Tests; zusammen 1 Minute.
Zusätzlich Karten-/Manager-/WebDAV-UI-Tests nach dem Iconwechsel erfolgreich.
Signatur mit bisherigem Testzertifikat geprüft; VersionCode 36, VersionName 0.6.20.
Bekannte Compilerwarnung: veraltetes onBackPressed(), kein neuer kompletter Lint-Lauf.
Hardwaretest des neuen MCSP-Lichtstatus steht weiterhin aus.

Kartenicons: locate-fixed, compass, maximize; negative Augen verwenden eye-closed
statt der generischen Schräglinie. Andere negative Statussymbole behalten ihre
Darstellung. Funktionen und Touchflächen unverändert, bisherige Ressourcen erhalten.

Dieser Stand ist die Vergleichsbasis für zukünftige Aufräumarbeiten: getrennte,
rücknehmbare Änderungen, relevante Tests und abschließender Hardwarevergleich.
