# Eigene Dashboard-Statuszeile

„Dashboard im Vollbild“ bleibt an erster Stelle von Design & UI, nun als vorhandene Schalter-/Accordion-Kombination mit Chevron links vom Schalter. Schalter aktiviert weiter ausschließlich das Ausblenden der Android-Statusleiste auf dem Dashboard; Einstellungs-/HRV-Fenster und Android-Navigationsleiste behalten ihre bisherigen Regeln. Die Zeile gehört zum Dashboard, nicht zu einem neuen Systemoverlay oder Hintergrunddienst.

## Konfiguration und Vorschau

Drei Slots links, drei rechts, kompakt in einer gemeinsamen Zeile. Ein FOREGROUND-Punkt trennt die Seiten mittig; „Rechts“ steht rechtsbündig. Alle zunächst leer; kein automatisches Belegen. Antippen öffnet ein am Slot verankertes Dropdown statt eines Dialogs. Zwei gleich hohe Dreierreihen ohne Baseline-Ausrichtung und mit gemeinsamem Abstand bilden das3×2-Raster. Optionen ebenfalls neutral, nur Symbol/Uhrzeit/VPN; Außenklick/Zurück schließen ohne Änderung. Langer Druck sortiert innerhalb derselben Seite wie bei den Profil-/Fahrtnamenkacheln. BicycleSegmentButton-Standalone-Karten mit gemeinsamen Konturen wie die Fahrtnamensbestandteile, schmalerem Innenpadding und gewichteter Breite für alle sechs Slots. Symbol bzw. Uhrzeit/VPN statt sichtbarer Bezeichnungen. Keine Auswahlfüllung, kein Auswahlrahmen/Glow/Schatten bei belegten Slots; nativer Checked-Zustand und gespeicherte Belegung bleiben dennoch unverändert. Andere Auswahlkarten behalten ihr bisheriges Highlight. Accessibility behält die Namen. Jede Quelle höchstens einmal; Zuweisung an anderen Slot leert ihren bisherigen Platz. Über die Auswahl sind alle Quellen auf beiden Seiten möglich.

Leere Slots verschwinden in der eigentlichen Statuszeile. Verbleibende linke Elemente linksbündig, rechte rechtsbündig. Unter den sechs Feldern zeigt die obere Hälfte eines gezeichneten Handy-Mockups dieselbe Statusdarstellung mit Beispielwerten und freigehaltener Kamera-Mitte. Die bisherigen Erklärungstexte entfallen. Kein neues Bitmapasset für das Mockup nötig. Die reale Darstellung verwendet Android-Insets und tatsächliche Kameraausschnitte, keine feste Notchgröße. Auch seitliche Kameralöcher werden aus den jeweiligen freien Halbflächen ausgeschnitten. Schmale Bereiche skalieren die Gruppe gleichmäßig; bei unlesbar wenig Platz wird sie ausgelassen statt über die Kamera zu zeichnen. Ohne Belegung bleibt das bisherige Dashboard unverändert.

- Uhrzeit: Android-Zeitformat/Zeitzone, ohne Sekunden; aktuelle Zeit als Text.
- Smartphone-Akku: gefülltes Akkusymbol plus Prozentwert direkt dahinter, z.B.72%. Echte0% erhalten, unbekannter Stand als Strich statt erfundener0%. Vorhandenen PhoneBattery-Wert aus RideRuntime verwenden, kein zweiter Akkuempfänger.
- Messenger: Icon der zuerst gewählten App unter Benachrichtigungen. Die Auswahlreihenfolge wird neu ausdrücklich als packageOrder gespeichert; ältere ungeordnete Auswahlen erhalten eine stabile alphabetische Ersatzreihenfolge. Ist kein Appicon auflösbar, gemeinsames Benachrichtigungsicon als Ersatz. Icon nur bei aktiven passenden Meldungen; Zähler eindeutiger aktiver Android-Meldungen, maximal99+. Bei mehreren gewählten Apps zählen alle passenden Meldungen unter diesem ersten Icon. Kein Anspruch auf exakte ungelesene Nachrichten. Bestehender Aktivierungsschalter, App-Auswahl und Notification-Listener-Zugriff gelten weiter; keine neue Berechtigungsabfrage durch die Statusauswahl.
- VPN: ausdrücklich Text „VPN“, nur wenn Android eine für die App sichtbare VPN-Transportverbindung meldet. Kein Server-/Internet-/Handshake-Test und keine eigene WireGuard-Anbindung. Bestehendes ACCESS_NETWORK_STATE genügt.
- Mobilfunk: Android-Signallevel0–4 der Standard-Daten-SIM als vier Empfangsbalken. Keine Netztyp-/Geschwindigkeits-/WLAN-Anzeige, keine SIM-Auswahl. Keine Daten oder fehlender Zugriff werden als unbekannt dargestellt. Flugmodus entfernt alte Balken; alte SIM-Callbacks werden verworfen. Keine zusätzliche gefährliche Berechtigung angefordert. Physische Signalqualität und herstellerspezifische Mehr-SIM-Anzeigen bleiben am Telefon zu prüfen.

## Lebenszyklus und Daten

DashboardStatusMonitor registriert nur gewählte Quellen bei aktivem Vordergrund-Dashboard/Vollbild, baut bei Konfigurationswechsel ab und stoppt bei onStop. VPN über NetworkCallback ohne VALIDATED-Anforderung; vorhandene VPN-Netze einmal beim Start lesen, danach Ereignisse. Zeit über Systembroadcasts, Mobilfunk über TelephonyCallback; keine periodischen Netz-/Funkabfragen oder Serverkontakte. Reale Registrierung/Callbacks übernehmen Android. Aufzeichnungs-, GPS-, Karten- und FIT-Verarbeitung unverändert.

MessageStatusNotifications hält ausschließlich aktive Notification-Keys und Paketnamen im Prozessspeicher. Listener-Verbindung liest vorhandene Meldungen, neue Meldungen ersetzen denselben Key, Entfernen löscht ihn. Zusammenfassungen/fortlaufende Meldungen/eigene Antworten folgen dem vorhandenen Decoderfilter. Keine Nachrichtentexte oder Keys sichern/loggen; bei Listenerverlust Speicher leeren. Bestehende Meldungs-Popups und deren Deduplizierung unverändert.

Belegung wie der bestehende Vollbildschalter im Fahrrad-Designprofil, einschließlich Profilkopie und geschützter Einstellungssicherung. Sechs stabile Enum-Namen; unbekannte Werte, doppelte nichtleere Quellen oder falsche Slotzahl beim Restore ablehnen. Live-VPN/Empfang/Benachrichtigungswerte nie sichern. Messengerreihenfolge gehört mit App-Auswahl zu den gesicherten Benachrichtigungseinstellungen.

## Prüfung

39 unterschiedliche gezielte Fälle grün: DashboardStatusTest7, DashboardFullscreenTest4, MessageNotificationsTest4, MapProviderSettingsTest2, ProfileSectionConventionTest1, SettingsBackupTest17, SettingsBackupSupplementTest4. Neue Prüfungen für Belegung/Leerzustand/Eindeutigkeit/Sortieren, Profil-/Backup-Roundtrip, Messenger-Auswahlreihenfolge samt Restore, aktive/entfernte Benachrichtigungen, VPN-Callbacks und verspätete Ereignisse nach Stop, Android-Mobilfunklevel/Flugmodus, tatsächliche Slot-Auswahl aus Design & UI, Kameraausschnitte sowie Native-Flat/Glow-Rendering und Symbolzentrierung.

[Konfigurationsvorschau Glow](previews/dashboard-status-settings-glow.png) · [Flat](previews/dashboard-status-settings-flat.png) · [Beispiel Statuszeile](previews/dashboard-status-glow.png). Synthetische Daten; die Glocke ist der Ersatz für ein in der Testumgebung nicht installiertes Messengericon. Kein echter Mobilfunk-/WireGuard-/Notchtest am Nutzertelefon, keine Gesamtsuite und keine neue APK beauftragt.


Veröffentlichung28.10.2026: Diese Änderungen sind in **APK0.8.56/Build97** enthalten. [Buildnachweis und Download](build-97.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


## Kompakte Weiterentwicklung nach Build97

Die oben beschriebene einzeilige/neutrale Slotansicht, Akku-Prozentwerte, das3×2-Dropdown und Handy-Mockup sind **noch nicht in APK0.8.56/Build97** enthalten. Keine neue APK beauftragt. Zehn gezielte unterschiedliche Fälle grün: neun DashboardStatusTest-Fälle (bestehende sieben plus Geometrie-/Prozentfall) und RideNamingReorderTest. Native Prüfungen in Flat/Glow und320/360/480px Hochformat, alle Slots sichtbar/gleich hoch, mittiger Punkt, neutrale Darstellung bei erhaltenem Checked-Zustand, Popup-Raster und Abbruch ohne Änderung, Prozent0/72/100/unbekannt. Normales Fahrtnamen-Highlight/Drag unverändert. Native Voransichten geprüft; keine neue Gesamtsuite oder Telefonprüfung. [Dropdown-Vorschau](previews/dashboard-status-options.png).


Veröffentlichung28.10.2026: Auch die zuletzt nach Build97 ergänzten Änderungen sind in **APK0.8.57/Build98** enthalten. [Buildnachweis und Download](build-98.md). Frühere Aussagen zu fehlender neuer APK sind Umsetzungshistorie.


Nach Build98: Statusgruppen links/rechts mindestens15dp zum sicheren Bildschirmrand. Zusätzlich meldet MainActivity die öffentlichen WindowInsets.privacyIndicatorBounds an denselben Sicherheitsbereichsrenderer wie Kameraausschnitte. Der vom System reservierte Indikatorbereich wird mit vorhandenem8dp-Sicherheitsabstand ausgespart, sofern er die Statuszeile schneidet. Leere/fehlende Angaben reservieren keinen Zusatzplatz; keine pauschalen25dp und keine Geräte-/Herstellerliste. Bounds sind kein zuverlässiger Sichtbarkeitsstatus und können dauerhaft reserviert oder vom Hersteller unvollständig gemeldet werden. Keine AppOps-Abfragen, neuen Berechtigungen oder Polling. Poco-Verhalten erst auf echtem Gerät verifizieren.
