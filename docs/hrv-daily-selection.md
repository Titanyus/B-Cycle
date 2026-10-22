# HRV-Tagesauswahl

Explizite Nutzerentscheidung: pro lokalem Kalendertag nur die letzte Messung anzeigen und als Tagesmessung für künftige Auswertungen verwenden. Wiederholungsmessungen bleiben möglich. Alle früheren Ergebnisse, Rohdaten und Fragebögen bleiben unverändert gespeichert; keine Migration oder neue Abhängigkeit.

`HrvDailySelection` gruppiert kleine `HrvHistorySummary`-Zeilen nach dem lokalen Datum in der bei Messbeginn gespeicherten Zeitzone. Legacy-Einträge ohne gültige Zeitzone verwenden die Gerätezeitzone. CSV-Importe behalten ihren expliziten Quelltag. Ein Zeitzonenwechsel des Geräts verschiebt keine bereits datierten Messungen. Innerhalb eines Tages entscheidet der spätere UTC-Messbeginn. Bei exakt gleicher Zeit hat eine native Messung Vorrang; sonst entscheidet die stabile ID deterministisch.

Die Auswahl erfolgt vor einer Qualitätsprüfung: auch eine neuere abgelehnte/abgebrochene Messung ersetzt die vorherige Tagesauswahl. Sie wird dadurch nicht zu einer akzeptablen Messung. Kein stiller Rückgriff auf ältere erfolgreiche Werte. Tagesbefinden bleibt bei seiner Originalmessung und wird nicht automatisch auf eine Wiederholung übertragen.

Importierte reine Kontexttage verdrängen keine Messung. Bei CSV-Messungen ohne Uhrzeit lässt sich die Reihenfolge gegenüber bekannten Messzeiten nicht feststellen; bekannte Zeiten haben daher Vorrang. Gibt es nur einen solchen CSV-Eintrag, bleibt er sichtbar. Diese Ausnahmen vermeiden erfundene Reihenfolgen aus dem Mitternachtsplatzhalter.

`HrvStore.daily()` und die Controller-Historie liefern die Tagesauswahl. `readDaily` löst auch einen veralteten Historienaufruf auf die aktuelle Messung desselben Tages auf. Nach Import einer neueren Messung wird eine gegebenenfalls überholte native Ergebnisanzeige zurückgesetzt. `list`, `ids`, `read`, portable Sicherung und Wiederherstellung behalten den vollständigen Bestand. Für spätere Trenddiagramme zuerst täglich auswählen, danach getrennt über Messqualität/Protokoll entscheiden.

Der Diagnoseexport enthält weiterhin eine JSON je archiviertem Ergebnis. Das additive Feld `dailySelected` außerhalb des unveränderten `result` markiert die aktuelle Tagesauswahl; es ist ausdrücklich keine Qualitätsfreigabe. Vorhandene Ergebnisdateien und Sicherungsformate bleiben kompatibel. Kein neuer Score oder Trendgraph in diesem Schritt.

Gezielte Regressionen: letzte statt beste Messung, Zeitzonen/Mitternacht/Sommerzeit, native und importierte Einträge, fehlende Uhrzeit/Kontexttage, deterministische Gleichstände, vollständiger unveränderter Archivbestand und Exportmarker. BLE-/HRV-Berechnung und Oberflächengestaltung bleiben unverändert.
