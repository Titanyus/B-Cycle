# Indoor-Dashboard und Schalter-Accordions – 19.10.2026

## Indoor

DashboardTemplate.forBike begrenzt die effektive Ansicht bei Indoor auf kartenlose Vorlagen. DashboardPresetPicker zeigt in seiner Layoutauswahl nur diese Vorlagen, derzeit FULL_DATA. Benannte Seiten, Feldbelegung und gespeicherte Kartenlayouts bleiben erhalten; ein vorhandenes Kartenlayout bekommt nur zur Darstellung den kartenlosen Rückfall. Wechsel zurück zu E-Bike stellt das gespeicherte Layout wieder dar. Eine bewusste neue Layoutauswahl bleibt wie bisher gespeichert.

Die untere Optionsleiste bleibt bei Indoor sichtbar, auch wenn der Karten-Callback ausgeblendete Controls meldet. Seit 0.8.8 gilt dies für alle Profile: Während der Aufnahme ersetzt RecordingButtonIndicator das Stoppsymbol im bestehenden linken Button durch eine weiche Überblendung zwischen Stop und rotem Punkt: 1,5 Sekunden halten, 0,5 Sekunden überblenden, dann Gegenrichtung. Ein kompletter Zyklus dauert vier Sekunden. Eine feine FOREGROUND-Kontur hält den roten Punkt auf rotem PRIMARY lesbar. Nur Darstellung; Klickaktion STOP, Beschriftung, Accessibility-Beschreibung und Aufnahme-/FIT-Logik bleiben erhalten.

Native ValueAnimator, keine Bibliothek. Animation bei Activity-Stopp, Aufnahmeende, ersetztem Symbol und zerstörter Activity abbrechen. Androids deaktivierte Animationen respektieren (statischer roter Punkt). Verdeckte Hauptansicht bei nächstem Render ohne Animation; bei Rückkehr neu beginnen.

## Einheitliche Schalter-Accordions

BicycleComponents.switchAccordion ist die gemeinsame Geometrie für Sensoren, Dienste/Sicherung/lokale Speicherung, Benachrichtigungen und Bosch-Fahrmodus: links eigenständige Beschriftung samt Icon, dann Chevron, ganz rechts äußerer Aktivierungsschalter. Text/Icon und Chevron öffnen/schließen nur Details. Allein die Switch-Touchfläche ändert Aktivierung. Vorhandene Switch-Instanz, Tag, Listener, Checked-Zustand, Sperren und Speicherung erhalten; eigener zugänglicher Name über contentDescription. Einträge ohne Aktivierungsschalter behalten ihre Aktion. Keine Änderung an einfachen Schaltern oder Trainingshilfe-Segmenten. Sensortrenner innerhalb der Liste erhalten 8 dp oben/unten (controlGap); der Abstand zum folgenden Abschnitt bleibt 32 dp und damit deutlich größer.

## Fahrtidentität

Streckenliste: Herkunfts-/Radtyp-Icon, Profilfarbstreifen und FIT/GPX bekommen im Kopf ihre natürliche Breite unabhängig vom kleinen Kartenthumbnail. Format einzeilig. Titel an der tatsächlichen Textkante des Datums ausrichten; Karte und Kennzahlen behalten ihre Breiten und Bedienung.

Streckendetails: bekannter Profilfarbstreifen 5 dp vor der Icon-/Formatgruppe, gleiche volle Höhe von 52 dp, 8 dp Abstand. Name/Datum bleiben insgesamt zentriert mit symmetrischen Reserven. Ohne verlässliche Profilzuordnung kein erfundener Farbstreifen, weiterhin zwei Textzeilen Platz.

Gezielte Tests: Indoor-Auswahl und Erhalt von Outdoor-Vorlieben, dauerhaft sichtbare Leiste, Aufnahme-/Lifecycle-Animation und STOP-Aktion, Überblendungsphasen, Formatzeile/Titelausrichtung bei schmalen Hochformaten, Farbe/Höhe im Detailkopf, unabhängige Aktivierung und Aufklappen bei Sensoren/Diensten/Benachrichtigungen.
