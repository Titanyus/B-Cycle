# Aktueller Stand

Seit 0.8.2 stehen drei Pulsmodelle zur Wahl. Die folgenden V1-Angaben dokumentieren den ursprünglichen Karvonen-Stand; maßgeblich für Methode, Schwellenpuls, aktivierte Felder und Sicherungen ist [Pulsmodelle](heart-rate-methods.md).

# Fahrerprofil und persönliche Karvonen-Zonen V1

## Integration und Architektur

Der zuvor funktionslose sechste Platz im Einstellungsraster ist jetzt „Fahrerprofil“ mit dem offiziellen lokalen Lucide-Icon `user-round` (0.468.0). Andere Menüpunkte behalten ihre Position. Die neue Bezeichnung kommt aus den zentralen Android-Textressourcen.

`RiderProfile` ist das zentrale optionale Datenmodell: Geburtsdatum (`LocalDate`), Größe in cm, Gewicht in kg, Ruhepuls, Maximalpuls und persönliche FTP in W. `RiderProfileStore` verwendet die bestehenden `bike`-SharedPreferences, eine einzelne versionierte JSON-Einstellung `riderProfile.v1`, ohne zusätzliche Preferences-Datei, Datenbank oder Abhängigkeit. `RideRuntime.riderProfiles` stellt die gemeinsame Schnittstelle bereit. Das Alter wird aus dem aktuellen lokalen Datum berechnet; Alter und Zonen werden nicht redundant gespeichert.

Der Store validiert vor dem Schreiben und bestätigt den synchronen Abschluss des kleinen einzelnen Preference-Eintrags. Ungültige Eingaben ersetzen keine gespeicherten Daten. Fehlende/defekte oder unbekannt versionierte Daten ergeben ein leeres Profil; bei einzelnen beschädigten Feldern werden nur diese verworfen. Alle sechs Angaben dürfen leer bleiben. Keine Beispielwerte werden vorausgefüllt. Datumsauswahl wird wie andere gültige Änderungen unmittelbar übernommen.

Das bestehende `EnergyProfile` ist ein optionales Berechnungs-Eingabemodell ohne eigene Persistenz. `RiderProfile.energyProfile(today)` bietet einen Adapter für spätere Verbraucher. Die bestehende Performance-/Energie-Berechnung wurde nicht geändert oder neu aktiviert. Persönliche FTP wird ausschließlich manuell gespeichert, niemals aus einer Strecken-FTP übernommen. Persönliche Coggan-Leistungszonen und der Dashboard-Zonen-Glow sind unter [Trainingszonen](training-zones-glow.md) beschrieben; es gibt keine neue W/kg- oder historische Streckenzonenanalyse.

## Eingaben, Validierung und Entwurf

`RiderProfileValidation` definiert breite Plausibilitätsbereiche: Größe 50–260 cm, Gewicht 20–400 kg, Ruhepuls 20–140 bpm, Maximalpuls 60–250 bpm, FTP 1–2500 W; Geburtsdatum nicht in der Zukunft und höchstens 150 Jahre zurück. Maximalpuls muss bei vorhandenen beiden Pulswerten über dem Ruhepuls liegen. Alter verändert niemals Maximalpuls.

Ganzzahlen müssen vollständig parsebar sein. Das Gewicht berücksichtigt das Dezimalzeichen der Gerätesprache und lokale Dezimalziffern. Gruppentrennzeichen, Exponenten, nachgestellte Dezimalzeichen, Teilzahlen, nicht endliche Werte und ungültige Zeichen werden verworfen. Fehler erscheinen lokalisiert am jeweiligen Feld. Datumsanzeige und Plural der Altersangabe berücksichtigen die Locale.

`RiderProfileView` hält einen unabhängigen Entwurf. Änderungen an Ruhe-/Maximalpuls aktualisieren sofort die Vorschau. Leere oder fehlerhafte Pulsangaben liefern einen kurzen passenden Hinweis statt berechneter Standardzonen. Andere fehlerhafte Eingaben verhindern die Puls-Vorschau nicht. Gültige Änderungen werden unmittelbar gespeichert. Unvollständige oder ungültige Eingaben ersetzen keine gültigen gespeicherten Werte. Header- und Android-Zurück führen zur vorherigen Einstellungsansicht; es gibt keine Speichern-/Abbrechen-Navigation. Der Date-Picker verwendet dieselbe bestehende Navigation für kompakte Unterdialoge.

## Karvonen und Rundung

`Karvonen` ist eine UI-unabhängige zentrale Berechnung:

- Herzfrequenzreserve = Maximalpuls − Ruhepuls.
- Zielpuls = Ruhepuls + Reserve × Intensität.
- Z1 50–60 %, Z2 60–70 %, Z3 70–80 %, Z4 80–90 %, Z5 90–100 %.
- Grenzwerte für ganzzahlige Messwerte werden mathematisch aufgerundet. Die Implementierung verwendet exakte ganzzahlige Prozentrechnung, um Gleitkomma-Rundungsartefakte zu vermeiden.
- Intervalle sind unten inklusive/oben exklusiv. Z5 enthält zusätzlich den eingegebenen Maximalpuls; intern wird hierfür die obere exklusive Grenze auf Maximalpuls + 1 gesetzt.
- Sichtbare Bereiche zeigen beide **inklusive** Integer-Grenzen, zum Beispiel bei 55/185 bpm: 120–132, 133–145, 146–158, 159–171, 172–185 bpm. Dadurch behauptet die Anzeige keine überlappenden Grenzen.
- Unterhalb Z1 und oberhalb Maximalpuls gibt `zoneFor` keine Zone zurück. Bei extrem kleiner Reserve können einzelne Integer-Zonen leer sein; diese zeigen einen Strich, statt einen falschen Pulsbereich zu erfinden. Alle fünf Definitionen bleiben erhalten.

`KarvonenResult` enthält die tatsächlich verwendeten beiden Pulsparameter sowie `modelId = karvonen-hrr-v1`. Damit können spätere Analysen ihre Berechnungsgrundlage nachvollziehen. Aktuell entstehen keine historischen Zonen-Snapshots, und keine FIT-Datei wird verändert.

## Gestaltung und Datenschutz

Der Screen verwendet `BicycleDialogBuilder`, `BicycleFullscreen`, `DialogNavigation`, `BicycleComponents`, zentrale Abstände/Headerhöhe, native Formulare, einen vertikalen Inhalts-Scrollbereich und die gemeinsame Zurück-Navigation im Header ohne Speichern-/Abbrechen-Footer. Systemfont für Formulare/Labels, Bebas Neue für Pulsbereiche, vertikal mittige kleine bpm-Einheit im bestehenden `BicycleDesign.dashboardUnitColor`. Alle normalen Texte/Icons verwenden Theme-Farben. Fünf kleine semantische Intensitätsmarkierungen sind unabhängig vom Farbtheme; keine farbigen Formular-Icons. Das bisherige Activity-Dummy-Icon wird als Asset nicht gelöscht.

Keine Cloud-/WebDAV-Synchronisierung, kein FIT-Export persönlicher Angaben, keine neuen Logs oder Telemetrie. Bereits vorhandene Android-Backup- und Device-Transfer-Ausschlüsse bleiben wirksam. Es werden keine Sensor-, GPS-, Aufzeichnungs-, Performance-, Karten- oder Streckenaktionen verändert.

## Dateien und Tests

Neue Dateien: `RiderProfile.kt`, `Karvonen.kt`, `RiderProfileView.kt`, `ic_lucide_user_round.xml`; Tests `RiderProfileTest.kt`, `KarvonenTest.kt`, `RiderProfileViewTest.kt`.

Integration: `SettingsMenuView.kt`, `MainActivity.kt`, `RideRuntime.kt`, `BicycleIcons.kt`, `BicycleDesign.kt`, `BicycleDialog.kt`, `strings.xml`, `scripts/import_lucide.py`. Bestehende Einstellungs-/Designmigrationstests wurden auf sechs funktionale Plätze und die ausdrücklich vorgesehenen Pulsbereichsschriften/Einheiten erweitert, nicht deaktiviert.

Die neuen Tests prüfen vollständige/partielle/leere Persistenz, beschädigte Daten, Abbruch-/Zurück-Verhalten, Alter/Datum/Locale, strenge Gewichtseingabe, breite Plausibilität, tatsächliche Trennung von persönlicher und berechneter FTP, bekannte/fractionale Zonengrenzen, eindeutige Zuordnung aller Integer-bpm in vielen gültigen Profilen, fehlende/ungültige Parameter, Live-Vorschau ohne Persistenz, Menüintegration, Date-Picker einschließlich Abbruch/Löschen, zentralen Header, festen Footer, native Scrollstruktur, schmale/kurze Displays, alle Preset-Themes, Theme-Geometrie, Einheiten, lokalisierte Ressourcen und zugängliche Beschriftungen/Touch-Flächen.

Abschlussprüfung am 12.10.2026: **280 Tests bestanden**, 0 Fehler, 0 Fehlschläge, 0 übersprungen; vollständige Suite und Release-Build erfolgreich in **2m**. Release-Lint-Vital erfolgreich. Bekannte Produktionswarnung `onBackPressed()` bleibt; native Back-Tests verwenden dieselbe Android-API. Kein zusätzlicher vollständiger Lint-Lauf, kein Gerätetest. Der Quellstand ist nach APK 0.7.5 entstanden. Eine praktische Prüfung von Date-Picker, Tastatur und Eingaben am Telefon steht aus.

Anschließende APK-Versionierung auf 0.7.6/44: 19 gezielte Profil-/Karvonen-/FIT-Tests erneut erfolgreich und Release-Build in 53s. Signierte Test-APK veröffentlicht, Signatur und Version geprüft; kein erneuter vollständiger Suite-Lauf nach der reinen Versionsänderung. Details: [Meilenstein 0.7.6](milestone-076.md).

Nach Build 61: optionale Geschlechtsauswahl Mann/Frau im Fahrerprofil, automatisch gespeichert und in der bestehenden Profil-Einstellungssicherung enthalten. Ältere Profile ohne `sex` behalten ihre Werte und gelten als nicht ausgewählt. Nur bei Frau kann die Frage zum Menstruationszyklus unter HRV-Messungen → Tagesbefinden aktiviert werden; bei Mann oder fehlender Auswahl ist sie gesperrt. Änderungen löschen keine historischen Antworten. Keine Änderungen an bestehenden Energie-, Zonen- oder HRV-Berechnungen daraus ableiten. Details: docs/hrv-questionnaire.md.
