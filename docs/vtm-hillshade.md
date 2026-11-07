# Optionale Online-Schummerung

Auftrag07.11.2026: Esri World Hillshade als dezente Reliefhilfe integrieren; Quellen/Lizenzen nach Erweitert. Keine neue APK beauftragt.

## Bedienung und Persistenz

„Schummerung“ im bestehenden Vektor-Ebenenmenü auf der Karte und unter Fahrradprofil → Karte → Kartenebenen. Anfangs aus, pro Profil gespeichert. Neue MapVectorLayer-ID HILLSHADE nur angehängt; bestehende IDs unverändert. Profilkopie/-wechsel, Einstellungssicherung und Designer-JSON-Sichtbarkeit nutzen denselben zentralen Enum-/Key-Pfad. Rasterkarten unverändert.

Reihenfolge: Grundflächen → Schummerung → optionale Höhenlinien → Straßen/Gebäude/Radwegehervorhebung → Radrouten → Beschriftungen → Tracks/GPS. Zweiter Grundkartenpass nur bei Schummerung oder Höhenlinien, gleicher geteilter Vektor-HTTP-Cache wie bisher. Beim Abschalten beider Ebenen wieder nur ein Pass. Karte bleibt flach und im selben GL-Kontext; keine zusätzliche MapView, Shader oder Dem-Berechnung.

## Bilder und Ressourcen

`HillshadeSource` lädt Esri-Bildkacheln256×256 mit **Z/Y/X** über HTTPS. Quelle: https://server.arcgisonline.com/arcgis/rest/services/Elevation/World_Hillshade/MapServer/tile/{z}/{y}/{x} . Nativ Zoom6–16, darüber vorhandene Texturen vergrößern. Dienstabdeckung/Detailgrad regional unterschiedlich; keine gleichmäßige Höhenauflösung oder garantierte Datenverfügbarkeit behaupten.

Ein VTM-Bildworker. Dekodierung und Schattenaufbereitung ausschließlich auf dem Worker, keine Höhen-/Hangberechnung auf UI-/GL-Thread. Weiße Pixel transparent, dunkle Pixel zu neutralem Schwarz mit Alpha: luminance=(77R+150G+29B+128)>>8, alpha=min(90,floor((255−luminance)×0.6)), vorhandenes Alpha berücksichtigen. Dadurch keine weißen JPEG-Flächen über Bright und kein Weißschleier über Naturfarben. Die maximale Schwarzdeckung beträgt etwa35%, typische helle Kacheln deutlich weniger. Straßen/Gebäude/Namen werden danach ungedämpft gezeichnet.

Eigenständiger HTTP-Client **ohne Plattencache**, keine Verbindung zum Raster-/DEM-Diskcache; begrenzte sichtbare VTM-Bild-/Texturkacheln (Cacheziel32), ein Worker. Kein Regionsdownload, kein Offline-Export, keine Nachbearbeitung von Fahrten aus Schummerungsbildern. Kein neuer Cache in Sicherungen oder Datenexporten.

Maximal1MiB pro Antwort, erwartete Dimension vor Bitmap-Allokation prüfen, begrenzte Connect-/Read-/Call-Timeouts. HTTP-/Bildfehler lassen nur den Relief-Tile fehlen, keine undurchsichtige Ersatzkachel. Cancellationgeneration und Call.cancel bei Pause/Dispose, veraltete Bildantworten verwerfen. Temporäre Bitmaps bei Fehlern recyceln, danach VTM-Sink/Texturlebenszyklus zuständig.

## Quellen und Nutzungsgrenze

MapCredits.hillshade zentral registriert: aktive Ebene im bestehenden Kartenquellen-Popup erreichbar; Kurzzeile bleibt kompakt. Erweitert → Quellen enthält Anbieter-/Lizenzlinks sowie vollständige Dienst-Credits aus `assets/Hillshade-ATTRIBUTION.txt`. Keine neue Quellen-/Lizenzseite auf dem Dashboard.

Offizielle Nutzungsangabe vom07.11.2026: Esri Master License Agreement; der normale World-Hillshade-Dienst ist nicht für den Kachelexport zur Offline-Nutzung vorgesehen. Das ist kein pauschales Verbot jedes technischen Anzeigecaches. Unsere Implementierung nutzt nur Online-Abruf und vorübergehenden Anzeigespeicher. Keine Aussage, dass damit beliebige kommerzielle Nutzung, Offline-Vorrat oder Weiterverteilung lizenziert wäre. Für Offline verweist Esri auf ein getrenntes forExport-Angebot für ArcGIS-Anwendungen.

https://www.arcgis.com/sharing/rest/content/items/1b243539f4514b6ba35e7d995890db1d?f=json
https://goto.arcgis.com/termsofuse/viewtermsofuse

Ohne Netz fehlen gegebenenfalls die Schatten; übrige Karten-/DEM-Quellen und deren bisherige Caches bleiben unabhängig.

## Gezielt geprüft

HillshadeTest: echte JPEG-Dekodierung/Transparenz/neutraler Schatten, Z/Y/X-GET ohne Fahrdaten, keine HTTP-Diskcache-Wiederverwendung trotz max-age, HTTP-/Bildfehler ohne opaque Ersatz, Inflight-Abbruch und Folgeabruf, Dispose ohne Request, native Ebenenreihenfolge mit/ohne Höhenlinien und Entfernen/Einpass-Rückkehr. MapVectorLayersTest: zentrale Profil-/Sicherungsrundreise einschließlich neuer Option und Defaultaus. MapPresentationSettingsTest: aktive Esri-Quelle, kompakte Quellenanzeige und vollständige Credits/Lizenz unter Erweitert. Kein Gesamtlauf oder lokaler APK-Build. GPU-/Geländeoptik auf realem Gerät noch zu prüfen.
