# Streckenaktionen – Schritt 1

Die nachträgliche Nutzerkorrektur ersetzt das ursprünglich vorgesehene Header-Dropdown: **fünf feste Icon-Buttons im Footer**, kein Drei-Punkte-Menü und kein Schließen-Button. Reihenfolge: Auf Karte ansehen, Umbenennen, Cloud-Upload, Speichern, Löschen. Der Header bleibt gleich hoch, Zurück führt zum erhaltenen Streckenmanager. Der Scrollinhalt nutzt die Fläche zwischen Header und Footer. Alle Icons und Texte verwenden FOREGROUND, Erfolgszustände bekommen kein Grün und kein ACCENT.

## Wiederverwendete Aktionen

- Karte: vorhandene interaktive Detail-Vollbildkarte, ausgewählte Track-ID/Analyse; kein Wechsel der Dashboard-Streckenauswahl. Nach Schließen dieselben Details.
- Umbenennen: der vorhandene Dialog samt Vorausfüllung, 120-Zeichen-Grenze und Leerprüfung ist in eine gemeinsame Funktion gezogen. Nach Abschluss aktualisieren sich Detailheader und Manager über `MapState`.
- Cloud: bestehendes `MapState.uploadRoute`/`RouteUploads` mit bisheriger WebDAV-Konfiguration. Nur selbst gefahrene FIT-Fahrten dürfen hochgeladen werden. Bei GPX/importierten Fahrten bleibt der Cloud-Button sichtbar und deaktiviert. Bestehende Queue verhindert doppelte Übertragungen derselben Datei/Zielgeneration.
- Speichern: dieselbe manuelle Aktion wie bislang „Nach Downloads exportieren“, über `MapState.exportRoute` und `DownloadFiles`. Dateiname, Format und manuelles Ziel Downloads/B-Cycle bleiben erhalten. Der automatische Export bei STOPP nutzt weiterhin den konfigurierten Zielordner. Das bisherige Manager-Icon zum Prüfen der internen Originaldatei wird nicht als echter abgeschlossener externer Speichervorgang interpretiert.
- Löschen: derselbe kompakte Bestätigungsdialog. Abbruch bleibt in den Details. Erst nach tatsächlich abgeschlossenem Löschen schließen sich die Details und kehren zum Manager zurück. Downloads/Importquellen werden weiterhin nicht mitgelöscht.

Alle Aktionen lösen anhand der ID den aktuellen Eintrag auf; keine Zuordnung nach Titel oder Listenposition. Busy-Zustand sperrt versehentliche Mehrfachaktionen. Manager-Menü, Auge, Cloud-/Speichericons und Listenlayout bleiben bestehen. Schritt 2 wird nicht automatisch umgesetzt.

## Nachweisbarer Status

`RouteUploads` bleibt die persistente Cloud-Quelle: Wartend, laufend, Fehler und Abbruch verwenden das normale Upload-Icon. Nur der nach erfolgreichem Transfer bestätigte Zustand derselben Datei und desselben Ziels verwendet `cloud-check`. Nach Rückkehr des Upload-Aufrufs wird Abbruch/Zielwechsel erneut geprüft, bevor Erfolg gespeichert werden darf. Ein gestarteter Transfer gilt niemals als bestätigter Erfolg.

`RouteLocalSaves` ergänzt getrennte persistente Nachweise für abgeschlossene externe Speichervorgänge. Ein Nachweis entsteht erst nach erfolgreichem Schreiben und Prüfung der unveränderten Originaldatei. Fehler und Abbruch bestätigen nichts. Ein erneuter Speicherversuch ersetzt den bisherigen Versuch; die bloße Existenz der internen Originaldatei erzeugt kein Häkchen. Auch der erfolgreiche automatische Export bei STOPP kann einen Nachweis erzeugen, unabhängig vom Upload.

Nachweise enthalten SHA-256, Länge, Änderungszeit und für lokale Saves das tatsächlich verwendete Ziel. Beim Öffnen der Details wird die betreffende Originaldatei auf dem bestehenden Worker gehasht; Footer-Abfragen verwenden diesen geprüften Stempel. Geänderte Inhalte, auch bei gleicher Länge und Änderungszeit, übernehmen den vorherigen Erfolg nicht ungeprüft. Normales Umbenennen ändert den Dateiinhalt nicht und lässt gültige Nachweise bestehen. Bei Löschen wird der lokale Nachweis entfernt. Die Status können nach erneutem Öffnen wieder angezeigt werden und sind voneinander unabhängig.

Der Footer aktualisiert seine Icons aus Statusrevisionen, ohne ihn bei jedem Tick neu aufzubauen. Zugängliche Beschreibungen melden bestätigten Erfolg. Die bisherigen Upload-/Speicheraktionen bleiben ausführbar. Ein gespeicherter Nachweis bestätigt den früher abgeschlossenen Speichervorgang; er überwacht nicht nachträgliches Löschen externer Dateien durch andere Apps.

## Originalicons und Ressourcen

Lucide 0.468.0 enthält die beiden gewünschten Erfolgsicons nicht. Nach dem Nutzerhinweis wurden die **offiziellen neueren Originale** übernommen, ohne eine Laufzeitbibliothek zu installieren oder andere Icons zu ersetzen:

- Quellrepository `lucide-icons/lucide`, Commit `70562c1ee1c4fdcf736fe97bc893fb8511927934`.
- Original-SVGs `icons/cloud-check.svg` und `icons/save-check.svg`, lokal als Android-Vektoren.
- `scripts/import_lucide.py` bindet ausschließlich diese beiden Namen an den neuen Commit; vorhandene Icons bleiben auf ihrem bisherigen Stand.
- Zugehörige Lizenz: `assets/Lucide-status-icons-LICENSE.txt`. Die alten Assets/Lizenzen bleiben erhalten.

Neue sichtbare Bezeichnungen, Bestätigungstexte, Abbruch-/Änderungsmeldungen und Accessibility-Texte liegen in `strings.xml`.

## Navigation und Tests

Beim gleichzeitigen Schließen von Löschbestätigung und Detailfenster konnte ein verspäteter Dialog-Callback den bereits geschlossenen Parent wieder öffnen. `DialogNavigation` stellt nun nur noch einen weiterhin angehängten, lediglich versteckten Parent wieder her; verspätete Show-Callbacks geschlossener Dialoge werden ignoriert. Eine gezielte Regression prüft die Reihenfolge der beiden Dismiss-Ereignisse. Die bestehenden verschachtelten Dialogtests bleiben erhalten.

Neue Tests prüfen persistente und unabhängige Erfolgsstatus, fehlgeschlagene und abgebrochene Vorgänge, geänderte Dateien, Queue-Deduplizierung, Statusänderungen im Footer nach tatsächlichem Upload, Speicherung über den bisherigen Download-Provider, erneutes Öffnen mit erhaltenen Icons, alle fünf festen Buttons, weiterhin vorhandene Manageraktionen, Rename-Header, Karten-Vollbild, Löschabbruch und bestätigte Rückkehr zum Manager. Native Vektor-Pixelvergleiche prüfen das Speicher-Erfolgsicon.

Vollständige Testsuite und Release-Build erfolgreich am 12.10.2026 in **1m 24s**: **265 Tests, 0 Fehler, 0 Fehlschläge, 0 übersprungen**. Release-Lint-Vital erfolgreich; die bekannte Produktionswarnung `onBackPressed()` bleibt. Kein zusätzlicher vollständiger Lint-Lauf. Die reale Nextcloud-/Dateiprovider-Interaktion ist zusätzlich am Gerät zu prüfen; automatisierte Tests verwenden vorhandene native Testprovider und Upload-Testcallbacks. Keine automatische Entfernung der Managerbedienelemente und keine neue signierte Veröffentlichung vor weiterer Anforderung.


Die anschließende Listenvereinfachung ist in [Schritt 2](route-manager-step2.md) dokumentiert; die alten Manageraktionen sind jetzt entfernt.

## Gemeinsamer Aktionsfooter (14.10.2026)

Die fünf Aktionen besitzen jetzt zusätzlich kleine sichtbare Beschriftungen unter
den Icons: Anzeigen, Umbenennen, Cloudupload, Speichern, Löschen. Der Streckenmanager
verwendet denselben Footer mit Importicon und „Strecke importieren“ links.
`BicycleFooterAction` bündelt Icon/Label zu einer Touchfläche; Statusicons und
Accessibility-Erfolgsbeschreibungen bleiben unabhängig von der kurzen Beschriftung.
Der feste Fullscreen-Footer wird aus dem Scrollinhalt herausgelöst. Keine zusätzliche
Schließen-Schaltfläche, keine Änderung an den fünf Aktionen oder ihren Services.

## Footer und Detailkopf nach Build114

Streckendetails und Fahrradprofile verwenden wie der Streckenmanager das Panel-Symbol links neben Info zum Ein-/Ausblenden des Footers. Standard bei jedem Öffnen ausgeblendet; Kontextaktionen der Fahrradprofile bleiben erhalten. BicycleFooterVisibility bündelt die seitenlokale Sichtbarkeit einschließlich temporärer Einblendung bei Langdruck-Auswahl im Streckenmanager. Keine Footeränderung am Fahrerprofil.

Fahrrad-/Datumsangaben und gegebenenfalls nachgeladene Wetterdaten liegen gemeinsam in einem TERTIARY-Container (`analysis_detail_head`). Themebindung aktualisiert die gesamte Fläche. Abstände, übrige Statistikgruppen und Diagrammhintergründe unverändert.

Korrektur nach Build115: Die TERTIARY-Fläche des Detailkopfs wird auch in die bestehenden seitlichen und oberen Seitenabstände gezeichnet, bis an beide Seitenränder und direkt unter den Hauptheader. Die Inhaltspositionen bleiben erhalten; der Kopf erhält unten zusätzlich8dp Innenabstand. Die gemeinsame Dialog-Body-Fabrik kann dafür einen spezialisierten LinearLayout übernehmen; andere Aufrufer verwenden unverändert den Standardbody.

Fahrttitel nach Build115 immer linksbündig, auch nach Header-Rehousing, Umbenennung und Größenwechsel. Links ein Back-Button-Inset, rechts Platz für Footer-Umschalter und Info. RideTitleView behält Scroll-/Fade-/Animationsphasen und Endkürzung bei deaktivierter Animation.
