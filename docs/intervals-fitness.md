# Fitness unter Statistiken

Aufträge 04.11.2026 nach Build 130: Fitness/Ermüdung/Form von intervals.icu abrufen und als letzten Abschnitt unter Statistiken anzeigen. Unabhängig von lokalen gefahrenen Strecken und Fahrradprofil; bei leerer Streckenstatistik bleibt der Abschnitt verfügbar.

## Offizielle Schnittstelle und Bedeutung

[Intervals OpenAPI](https://intervals.icu/api/v1/docs), am 04.11.2026 gelesen: `GET /api/v1/athlete/{id}/wellness.json?oldest=YYYY-MM-DD&newest=YYYY-MM-DD&fields=id,ctl,atl`. Beide Datumsgrenzen inklusive, lokale Kalendertage. Die Wellness-Felder `ctl` und `atl` sind Zahlen (float); es existiert dort kein eigenes Form-Feld. Fitness = CTL, Ermüdung = ATL, absolute Form = CTL−ATL. `fatigue` ist dagegen eine subjektive Wellnessangabe, `ctlLoad`/`atlLoad` sind Belastungsangaben; diese werden nicht als Fitnesskurven interpretiert. Keine neue CTL-/ATL-Berechnung aus Aktivitäten.

Originalwerte ohne Rundung speichern, Anzeige auf eine Nachkommastelle. Fehlendes/null CTL oder ATL bleibt fehlend; absolute Form nur bei beiden vorhandenen Werten. Numerische Null bleibt gültig. Ungültige Typen, negative/nichtendliche Lastwerte, doppelte/außerhalb liegende Tage oder fehlerhafte Antworten werden abgewiesen. Keine Gesundheitsdaten, Antwortkörper oder Zugangsdaten im Log. Kein Zugriff auf ein privates Konto während der Entwicklung; Tests nur mit synthetischen Daten.

Screenshot als Vorlage für zwei übereinander liegende Kurvenbereiche: oben Fitness blau/Ermüdung magenta, darunter absolute Form gelb samt Null-Linie. Tagesauswahl synchronisiert Datum und drei Werte. Es wird ausdrücklich die absolute Form gezeigt; eine etwaige prozentuale Form-Einstellung des Intervals-Webkontos wird derzeit nicht abgefragt. Keine erfundenen Risiko-/Trainingszonen aus dem Screenshot übernehmen. Eine rechnerische Prozent-Property ist für spätere explizite Darstellung vorbereitet, CTL=0 liefert dabei keinen Wert.

## Bedienung und Abruf

Letzter Statistikabschnitt „Fitness · intervals.icu“, initial eine Woche bis heute. Auswahl Woche/Monat/3 Monate = rollende 7/30/90 Kalendertage einschließlich Endtag. Horizontales Ziehen mit nativer Trägheit verschiebt den Zeitraum bis zu früheren Tagen; nach rechts maximal bis heute. Antippen wählt einen Tag. „Heute“ kehrt zurück; „Aktualisieren“ erzwingt einen Abruf. Accessibility vor/zurück verschiebt um eine sichtbare Spanne. Vertikales Scrollen der Statistik bleibt erhalten.

Gemeinsame Tagesachse, lückengetrennte monotone Kurven ohne künstliche Extrema, einzelne Werte als Punkt. Keine Glättung der gespeicherten Daten. Darstellung in Flat/Glow mit zentraler Typografie/Schaltern; native View/Canvas ohne weitere Bibliothek. Diagramm und Tageswerte aktualisieren sich bei Auswahl; HTTP/Dateizugriff niemals in onDraw oder pro Dragframe.

Vorhandene Intervals-Aktivierung, Athleten-ID, verschlüsselter API-Schlüssel und WLAN-Vorgabe verwenden. Abruf ausschließlich GET, keine Uploads/Bestätigungsbelege oder HRV-Daten ändern. Kompakte Cloud-IN-Zeile nennt Fitness/Ermüdung/Form. Ohne Konfiguration verständlicher Hinweis; bei Netzwerk-/Authentifizierungsfehlern vorhandene Fehlerzuordnung, bei fehlerhaften Fitnessantworten eigene Lesemeldung statt „Upload nicht bestätigt“.

Pro View ein Worker. Abruf nach Scrollende 250 ms verzögern, je eine Viewportbreite vor/nach dem sichtbaren Bereich puffern. Ganze sichtbare Bereiche inklusive Puffer maximal 366 Tage, in 31-Tage-Batches. Alte Generationen bei Zeitraum-/Konto-/Einstellungswechsel oder Schließen nicht mehr anzeigen. Kein Hintergrundjob oder dauerhaftes Polling. Offene HTTP-Anfrage kann noch bis zur vorhandenen Client-Zeitgrenze laufen; ihr Ergebnis wird bei Abbruch verworfen.

## Lokaler Cache

Privater, erneut ladbarer Cache unter cacheDir/intervals-fitness, je Athleten-ID getrennte SHA-256-Datei, Schema 1. Keine API-Schlüssel im Cache, kein neues Backupformat. AtomicFile und gemeinsame Sperre, höchstens 1098 Tageseinträge/1 MiB pro Konto. Beim Kürzen angefragten Bereich bevorzugen, danach zuletzt aktualisierte Daten; frühere Zeiträume können später erneut abgerufen werden.

Nur vollständig erfolgreiche Bereiche atomar veröffentlichen. Fehlende API-Tage als explizite leere Tageswerte speichern, nicht mit Null auffüllen. Teilfehler/Abbruch erhalten den vorherigen vollständigen Stand. Überlappende erfolgreiche Aktualisierung ersetzt auch inzwischen fehlende Werte, statt alte Werte unbegrenzt festzuhalten. Beschädigten/unvollständigen Cache neu laden. Alter unter sechs Stunden: Cache direkt wiederverwenden, sonst Cache zuerst zeigen und aktualisieren; Datum des gespeicherten Stands sichtbar. Bei Netzfehlern vorhandene Kurven behalten.

## Gezielte Prüfung

IntervalsFitnessTest: echte MockWebServer-GETs mit Auth/Feldprojektion/Datumsgrenzen, CTL/ATL-Präzision und Verwechslungsschutz, Lücken/0, ungültige Antworten/401; 90-Tage-Batching, Wiederlesen/Teilzeiträume, Offline-/Abbruch-/Teilfehlerschutz und Kontotrennung; Statistikabschnitt auch ohne lokale Fahrten zuletzt, Diagrammpanning/Tagesauswahl und tatsächliches Canvas-Bild in beiden Designs. Zwei gezielte API-/Cachefälle und ein nativer UI-Fall, kein Gesamtlauf. Rendernachweise unter app/build/reports/ui/fitness-flat.png und fitness-glow.png. Live-Abgleich mit dem tatsächlichen Nutzerkonto bleibt Geräteprüfung; neue APK noch nicht beauftragt.
