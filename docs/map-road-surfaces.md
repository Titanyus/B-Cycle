# Belagsdarstellung nach Build138

Nutzerregel: Bei Straßen und Wegen bedeutet gestrichelt ausdrücklich unbefestigt. Befestigte und unbekannte Beläge sind durchgezogen. Ausschließlich vorhandene surface-Angaben verwenden, nicht aus Wegeart/tracktype auf den Belag schließen. Radwegehervorhebung bei bekannt unbefestigtem Weg selbst deutlich gestrichelt; kein seitlicher Versatz beauftragt. Separat geladene Radrouten haben keine Belagsdaten und bleiben durchgezogen (unbekannt).

## Umsetzung

MapRoadSurface erkennt explizit unpaved/unsealed/gravel/fine_gravel/compacted/ground/dirt/earth/grass/sand/mud/woodchips/pebblestone/rock/stones. Fehlende, unbekannte oder gemischte Angaben bleiben solid. OpenFreeMap liefert meist normalisiertes paved/unpaved; der native MVT-Decoder erhält surface bereits. VtmBrightTheme ergänzt für transportation ein abgeleitetes bcycle:surface=unpaved/other.

Nach Tracktrennung und Designer-Farb-/Breitenanwendung erzeugt MapStyleXml.roadSurfaces zwei native Varianten je Straßen-/Wege-Linienanweisung. Original-IDs/Farben/Breiten/Alpha erhalten; bei unpaved ein einheitliches6/4-Strichmuster mit offenem Abstand, bei other alle bisherigen Strichmuster entfernen. Gilt für Straße/Pfad/Feldweg, Tunnel/Brücke, Umrandung und farbige Radwegehervorhebung. Gleiches Muster und deterministischer Anfang für Füllung/Umrandung/Overlay halten die Lücken gemeinsam offen. Varianten bleiben unter denselben IDs im nativen Live-Farbsystem, damit Feldweg-/Overlayfarben und Transparenz sofort für beide Belagsfälle gelten. Keine neuen Shader oder Änderungen der Referenz-Bright-JSON/XML/Sprites.

Bahnlinien, Fähren, Seilbahnen, Gewässer, Grenzen und Höhenlinien behalten ihre eigenen nicht als Straßenbelag verstandenen Muster. Der Designer bietet bei Straßenzielen keine freie Linienmusterwahl mehr, sondern den kompakten Hinweis „Gestrichelt: unbefestigt. Durchgezogen: befestigt oder unbekannt.“ Farbe/Alpha/Breite bleiben editierbar. Alte manuelle Straßenmuster können im Datensatz bestehen bleiben, werden von der Belagsregel überschrieben.

JSON enthält die feste Regel als roadSurfaceStyle="unpaved-dashed" zusätzlich zu den bisherigen Daten. Alte Exporte ohne das Feld bleiben gültig; unbekannte Regelwerte werden zurückgewiesen. Die feste Belagsregel hat Vorrang vor alten objektbezogenen Straßenmustern. Kein neuer Schalter und keine neue Importfunktion.

## Prüfung

Sechs gezielte Fälle grün: RoadSurfaceStyleTest für sieben Wege-/Straßenklassen, normale/Tunnel-/Brückenlagen und mehrere bekannte/fehlende Beläge; echte native Dash-Texturen/solid Varianten, gleiches Overlaymuster/gleiche Phase, Farben/Live-Update/JSON; zwei Editor-/Profil-/Backup-/Exportfälle; bisherige native Live-Farb-/Reset-/Texturprüfung; Straßenhierarchie/Zoom-/Filter-/Schema-Referenz und Radwegeübersicht. Logs /workspace/.tools/road-surface-style-tests.log (fünf erfolgreich, verbleibende veraltete Solid-Offset-Annahme korrigiert) und road-surface-reference-final-test.log (erfolgreich). Typografie/UI-Katalog/Bright-Generator/Diff grün. Kein Gesamtlauf, keine neue APK. Optische Geräteprüfung der Strichwirkung offen.

## Folgeauftrag nach Build139

Strichlänge12 / Lücke8 vor Geräteskalierung, auf ausdrücklichen Wunsch kartenweit für alle gestrichelten Linien, einschließlich Grenzen/Gewässer und Straße/Radwegehervorhebung. Reine Punktmuster bleiben Punkte; Strich-Punkt erhält12er Striche,8er Lücken und seine Punkte. Strukturelle Bahn-Querstriche bleiben Bahnsymbole. Anwendung nach Designer-Breiten/Farben, Original-Bright-Assets unverändert. MapDash.DASHED und DASH_DOT entsprechend angepasst.

JSON ergänzt feste Metadaten dashPattern=[12,8] und placeTextHalo={widthScale:2,opacity:1}; alte Dateien ohne diese Angaben bleiben gültig. MapPaintPolicyTest prüft Source-Linienmuster, native Städte-/Länder-/Kontinent-Halos, Padding und Metadaten. Nach Korrektur der gültigen Länder-/Kontinent-Testmerkmale erfolgreich in map-halo-global-dash-final-test.log.
