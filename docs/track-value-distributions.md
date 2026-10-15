# Streckendetails: zeitgewichtete Verteilungskurven

Unter den bisherigen Streckenverlaufsdiagrammen folgen zwei native, ungefähr quadratische Diagramme (jeweils volle verfügbare Inhaltsbreite, untereinander): Leistung und Puls. Die bestehenden Strecken-Zonenbalken bleiben unverändert distanzgewichtet.

## Daten

Die neue Kurve zeigt den **Zeitanteil pro Messwertbereich**, nicht den zeitlichen Verlauf und nicht den Streckenanteil. Derselbe Streaming-Detail-Lader und dieselben gültigen Intervalle wie bei den Zonenbalken liefern die Daten vor jeder Diagrammverdichtung. Es gibt keine neue Aufzeichnung und keine Änderungen an FIT, Sensoren oder Performance Engine.

Ein gültiger vorheriger Messwert gilt für das folgende Intervall von höchstens fünf Sekunden. Echte 0 W zählen mit. Fehlende Werte, Timer-Pausen, neue Segmente und längere Lücken werden nicht mit Null gefüllt oder überbrückt. Zeitverteilungen funktionieren auch ohne GPS oder verlässliche Distanzdaten. Leistung und Puls werden unabhängig ausgewertet; pro Kurve sind gültige Messintervalle und passende Profilzonen erforderlich. Fehlende Kurven entfallen.

Histogramme sind unabhängig von der Fahrtdauer begrenzt (Watt-/BPM-Buckets); im Ergebnis bleiben höchstens 128 Anzeigebuckets je Kurve. Glättung nur an dieser Darstellung: dezenter Fünf-Punkt-Filter in zwei Durchläufen, keine negativen Werte, Gesamtdauer bleibt erhalten. Die native Kurve verwendet quadratische, innerhalb ihrer Kontrollpunkte bleibende Übergänge. Keine künstliche Glockenkurve und kein Rückschluss von der Profilzone auf Messwerte.

## Darstellung

Bekannte sieben Coggan- und fünf Karvonen-Zonen aus dem aktuellen Fahrerprofil, mit den vorhandenen semantischen Zonenfarben als dezente Hintergrundflächen. Am oberen Rand ausschließlich die Zahlen 1 … 7 beziehungsweise 1 … 5. Keine zusätzlichen Prozentangaben oder AVG-Marker. Seitliche Bezeichnung und X-Achsentexte verwenden die vorhandene Details-Typografie und Theme-Farben.

Die X-Achse deckt alle beobachteten Werte einschließlich des letzten Samples ab. Zusätzlich sind sämtliche Profilzonen sichtbar: Leistung ab 0 W mindestens bis 175 % FTP (Platz für offene Zone 7), Puls mindestens vom unteren Profilbereich bis zum Profil-Maximalpuls, bei Bedarf nach unten/oben erweitert. Achsengrenzen auf 5 Einheiten gerundet. Puls außerhalb der Profilzonen bleibt sichtbar, erhält aber keine falsche Zonenfarbe.

Weiße Hauptlinie, beide Leuchtsäume und transparente Verlaufsfüllung verwenden die vorhandenen `LiveGraphStyle`-Stärken/Deckkräfte. Pfade und Shader werden beim Größenwechsel vorbereitet; beim Scrollen keine erneute Auswertung oder Histogrammverarbeitung.

Vorschau aus echten nativen Views mit reproduzierbaren **synthetischen Testdaten**, BLACK OPS: [Diagramme](previews/track-distributions.png).

Validierung: 37 gezielte Tests bestanden (Zeitgewichtung, Lücken/Nullwerte/Pausen, Skalierung und Zonenfarben, FIT-Wiedereinlesen, bestehende Details/Best Efforts/Zonenbalken, quadratische Hochformat-Layouts in allen Presets). Release-Kotlin-Kompilierung erfolgreich.
