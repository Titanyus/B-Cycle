# Gemischte Live-Graphfenster: konsistente Gesamtachse10.11.2026

Nutzerfehler: ein30-s-Graph und ein Gesamtverlauf auf demselben Dashboard. Der vollständige Graph schrumpft sekündlich, seine weiterführende Linie erscheint aber erst später.

Ursache bestätigt: LiveGraphProvider veröffentlicht kurze Fenster im Normalmodus1s, bereitet den Gesamtverlauf bewusst nur alle10s neu auf. GraphHistoryBuffer übernahm zwischen diesen Gesamtaktualisierungen die bisherige series, kombinierte sie aber mit aktueller distanceM/elapsedMs/timeAxis. DashboardGraphDrawable projizierte dadurch ältere Punkte auf die längere neue Achse. Der bisherige reine Sparmodus-Guard vermied einen Teil der unnötigen Rebuilds, schützte aber nicht neue Drawables oder Layout-/Fensterwechsel.

GraphFrame trägt jetzt den zu seiner vollständigen Kurve gehörenden Distanz-/Zeit-/Achsentyp-Snapshot (fullDistanceM/fullElapsedMs/fullTimeAxis). Bei Wiederverwendung des Gesamtverlaufs werden diese Werte gemeinsam übernommen, bei vollständiger Aufbereitung gemeinsam erneuert. Vollständige Drawables verwenden diese passenden Achsenwerte auch bei erstmaligem Binden oder Größenwechsel. Derselbe Gesamtstand benötigt in beiden Modi keinen neuen Pfadaufbau durch einen reinen Kurzfensterframe. Kurze Fenster nutzen weiterhin die aktuellen Zeitwerte.

Keine Änderung der Intervalle: kurze Fenster1s im Normalmodus, mindestens5s im Sparmodus; vollständiger Verlauf10s. Keine zusätzlichen Worker, Timer, Sensorabos, Datenreduktion oder Änderung von Glättung/Rohdaten/FIT. Die Kurve wird nicht künstlich bis zum neuesten Zeitpunkt verlängert; Achse und tatsächlich vorhandene Kurve wechseln synchron.

Gezielt geprüft, vier Methoden grün:

- Neuer nativer Mischfensterfall in LiveGraphUiTest: echte Providerframes mit30s-Herzfrequenz und vollständiger Leistung; vier Kombinationen aus Normal-/Sparmodus und Distanz-/Zeitachse. Frisch erzeugte vollständige Drawables sind zwischen den10-s-Aktualisierungen pixelidentisch, während das kurze Fenster weiterzeichnet. Nach10s enthält der Gesamtverlauf die echte Fortsetzung und ein neues Bild.
- Bestehender Providerfall:10-s-Takt, Abschlussflush und nächste Fahrt/Reset.
- Bestehender Sichtbarkeitsfall: vollständige Historie trotz unsichtbarer Graphen und frische Rückkehr in beiden Modi.
- Bestehender Sparmodusfall: statische Darstellung und kein unnötiger Gesamtframewechsel. Synthetischen Zwischenframe auf copy angepasst, damit er wie der tatsächliche Provider seinen Gesamtachsensnapshot mitführt.

Log mixed-graph-snapshot-before-test.log reproduziert den gemeldeten Fehler vor der Korrektur exakt als nativen Bildunterschied. mixed-graph-snapshot-tests.log mit vier erfolgreichen Methoden danach. Typografie/UI-Katalog/Bright/Diff geprüft. Kein Gesamtlauf/Profiling, keine APK beauftragt. Bediengefühl am Gerät bleibt Abschlusskontrolle.
