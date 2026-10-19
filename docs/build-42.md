# B|CYCLE 0.8.7 – optimierte APK

Build 42 vom 19.10.2026, versionCode 77.

**[APK herunterladen](https://raw.githubusercontent.com/Titanyus/B-Cycle/v0.8.7/downloads/B-Cycle-0.8.7.apk)**

## Änderungen

- Indoor bietet nur kartenlose Layoutvorlagen an, derzeit FULL_DATA. Vorhandene Kartenlayouts bleiben unverändert gespeichert und bekommen im Indoor-Modus lediglich den kartenlosen Darstellungsrückfall.
- Untere Optionsleiste bei Indoor immer sichtbar. Während Aufnahme wechselt das Symbol im linken Button etwa alle zwei Sekunden weich zwischen Stop und rotem Aufnahmepunkt. Die vorhandene STOP-Aktion bleibt erhalten; Animation bei Aufnahmeende/Activity-Stopp abbrechen.
- Streckenliste: Titel an der Datumstextkante ausgerichtet, Herkunft/Profilfarbe/FIT/GPX im Kopf unabhängig vom kleinen Thumbnail. Format einzeilig.
- Streckendetails: bekannter Profilfarbstreifen vor Radtyp-Icon und FIT/GPX, über deren gesamte Höhe. Keine erfundene Farbe bei unbekannter Zuordnung.
- Schalter-Accordions zentral: Beschriftung/Icon links und Chevron öffnen Details, äußerer Switch ganz rechts aktiviert allein. Sensoren, Dienste/Sicherung/lokale Speicherung, Benachrichtigungen und Bosch-Fahrmodus vereinheitlicht.
- Sensortrenner innerhalb der Liste wieder kompakter als der Abstand zum nächsten Abschnitt.
- Konventionen und Indoor-Hilfe ergänzt; [Details](indoor-dashboard-and-switch-accordions.md).

## Prüfung

Gezielte Tests für Indoor-Vorlagen und Erhalt gespeicherter Outdoor-Layouts, kartenlose Ansicht/Optionsleiste, Aufnahmehinweis und STOP-Aktion, Lifecycle-Stopp/Neustart, weiche Phasen, einzeiliges Format und Titelausrichtung auf schmalen Hochformaten, Profilfarbe im Detailkopf sowie unabhängige Schalter/Accordion-Bedienung bei Sensoren, Diensten und Benachrichtigungen bestanden. Bestehende Karten-/Listen-/Detailbedienung gezielt geprüft. Kein vollständiger Testlauf und kein physischer Gerätetest.

[GitHub Actions erfolgreich](https://github.com/Titanyus/B-Cycle/actions/runs/39206642727), Quellstand a0055923ed03b0f4c73ca2332b76706b465be2b4. Release mit R8 und Ressourcenverkleinerung; kein lokaler APK-Build. Artefakt-Prüfsumme geprüft, mit bisherigem Testschlüssel nachsigniert; Zertifikat, Version 0.8.7/77, nicht-debuggable Manifest und 16-KiB-Zip-Ausrichtung geprüft.

- Größe: 1,858,724 Bytes.
- SHA-256: `ae998fab2848e9bff641294e2be8bab1b885d55dbf96a1f3e5e1fe9bd865bb1a`.
- Zertifikat SHA-256: `1f9f9ea0db9df10a47a4da05f93d5dd265062b474421e98d0819cbf4ac7e555a`.
- [Prüfsumme](../downloads/B-Cycle-0.8.7.apk.sha256) · [R8-Mapping](../downloads/B-Cycle-0.8.7-mapping.txt.gz).
