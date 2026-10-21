# B|CYCLE 0.8.21

Optikkorrekturen bei Fahrtnamen, segmentierten Auswahlen und Einstellungsseiten. Berechnungen, Auswahlverhalten, Speicherung und Navigation bleiben erhalten.

- Fahrtnamensbaustein heißt „Fahrtzeit“; weiterhin dieselbe Bewegungsdauer.
- Icon über kleinem Text als gemeinsam zentrierter Block mit 2 dp Abstand.
- Segmentierte Einfach-/Mehrfachauswahlen erhalten global den gemeinsamen umlaufenden Rahmen wie die Trainingshilfe, einschließlich Fahrtnamen und Graph-Zeitfenstern.
- Eingabefelder und Auswahlgruppen mit 8 dp Abstand zum Nachbarn; auch nach Ersetzung der Layoutparameter.
- Normale erste Seiteninhalte einheitlich 16 dp unter dem Haupt-Header, führende Sticky-Header bündig. Verschachtelte und nachträglich eingefügte Formulare berücksichtigen.

11 gezielte UI-Tests erfolgreich, einschließlich nativer Pixelprüfung, sieben tatsächlicher Einstellungsseiten, dynamischer Formulare und bestehender Mehrfachauswahl/Profilspeicherung. Nativ gerenderte Fahrtnamensauswahl visuell kontrolliert. Kein vollständiger Gesamttestlauf.

[Globale UI-Konventionen](ui-conventions-audit.md) · [Fahrtnamen](ride-naming.md) · [Übergabe](CONTINUE-HERE.md).
