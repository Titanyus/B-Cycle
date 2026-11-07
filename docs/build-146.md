# B|CYCLE 0.8.101 – Build146

- [APK direkt herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/main/downloads/B-Cycle-0.8.101-release-146.apk)
- [SHA-256](../downloads/B-Cycle-0.8.101-release-146.apk.sha256)
- [Erfolgreicher Actions-Build](https://github.com/Titanyus/B-Cycle/actions/runs/42352289278)
- Quellstand: `31df1e662118fc647bc778b934b2872983880d25`
- Version0.8.101, versionCode171, Android13+.
- Größe: 4387299 Bytes (4,39MB).
- SHA-256: `b91de0ff6202c68d25bae2f5770b48c9043e22e8f7dfa86253edc813fce20c7b`.
- Zertifikat SHA-256: `94d34c46ede28d12e6d37efd8a7d953a75b621596ec2e5b68f61899b8ea7499b`.

Release ausschließlich aus GitHub Actions. CI-Prüfsumme, dauerhafte Update-Signatur, Version und Manifest ohne Debug-/Test-Flags geprüft. ZIP-/16KB-Ausrichtung und ARM64-ELF-LOAD-Segmente geprüft; vier VTM-ABIs und28Shader vorhanden. Original-Bright-JSON, generiertes XML und113Sprites bytegleich zu Quellen. Terrain-/Cycling-Attribution erhalten. R8-Mapping für GLMatrix/MVT, Trainingsleser/Speicher sowie neue Fahrtfeedback-/GL-Kontextpfade geprüft. Headless-Testshadow nicht im APK.

[Änderungen](release-0.8.101.md): Zusammenfassung mit Fahrtnamen und Werten ohne Icons, autospeichernde Notiz/RPE, Detailkopf mit Notizeditor und RPE, HRV-Vortagsbewertung weiterhin getrennt. Datei-/Backupübertragung und Aktualisierung verknüpfter Cloudfahrten; Intervals natives icu_rpe, Strava/Dreeve Beschreibung mit RPE-Text. Details-Vollbild verwendet dieselbe Karteninstanz, gemeinsame GL-Ressourcen abgesichert.

21 gezielte Tests grün (`ride-feedback-map-tests.log`): Autospeicherung/0 vs. unbewertet/normaler Text, FIT mit RPE und Unicode-Memo/GPX-Erweiterungen/Originalbytes, Beschreibungsfelder aller drei Dienste am Testserver, Backup, Zusammenfassung, Vortagssummen, Queue-/Kontowechsel, Kartenidentität und aufgezeichnete EGL-/GL-Aufrufe. Zusammenfassungsvorschau kontrolliert. Typografie/UI-Katalog/Bright/Diff grün. Kein Gesamtlauf und keine lokale APK. Optischer Kartenvergleich auf Geräte-GPU und persönliche Cloud-Kontoprüfung offen. [Feedback/Speichergrenzen](ride-feedback.md), [Kartenbefund/Prüfgrenze](vtm-detail-fullscreen-contexts.md).

## Direkter Downloadnachweis

Direkten veröffentlichten Raw-Download tatsächlich abgerufen: 4387299 Bytes, bytegenau identisch zum geprüften Actions-Artefakt; SHA-256 b91de0ff6202c68d25bae2f5770b48c9043e22e8f7dfa86253edc813fce20c7b ebenfalls identisch.
