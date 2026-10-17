# Benachrichtigungen auf dem Dashboard

Seit dem Funktionsstand für APK 0.7.27 implementiert. Einstellungen → **Benachrichtigungen**,
direkt nach Design & UI. Native Kotlin-Views, eigener Fullscreen-Screen mit Header-Chevron,
sofortiger Speicherung, gemeinsamen Sticky-Kategorien und ohne leeren Footer.

## Einrichtung

- „Benachrichtigungen anzeigen“ aktivieren. Bei fehlendem Zugriff öffnet sich Androids
  Detailseite für Benachrichtigungszugriff, mit Fallback auf die allgemeine Systemseite.
- Zugriff in Android ausdrücklich freigeben. Nach Rückkehr tatsächliche Berechtigung prüfen.
  Erteilte Berechtigung bedeutet keine pauschale Auswahl aller Apps.
- Einzelne Apps auswählen. Die Liste umfasst für BICYCLE sichtbare installierte Apps,
  beobachtete Benachrichtigungsquellen und bereits ausgewählte Paketkennungen, soweit die App noch installiert und regulär nutzbar ist. Android-Systemdienste ohne Launcher-Einstieg werden auch in der vollständigen Liste ausgeblendet; vorinstallierte Apps mit Launcher sowie bekannte Messenger bleiben sichtbar. Bestehende Auswahlen werden durch die Filterung nicht gelöscht.
  Android liefert keine vollständige Liste aller künftig sendenden Apps. Die Manifest-
  Sichtbarkeit beschränkt sich auf Apps mit Launcher-Einstieg; kein QUERY_ALL_PACKAGES.
- „Nur gängige Messenger anzeigen“ filtert über Paketkennungen (WhatsApp, Signal,
  Telegram, Element usw.). Ausgeblendete Auswahlen bleiben erhalten, nichts wird automatisch
  ausgewählt. Eigene BICYCLE-Benachrichtigungen werden immer ignoriert.
- Anzeigedauer per Slider **5–180 Sekunden**, 1-Sekunden-Schritte, Standard **60 Sekunden**.
- Drei Positionskacheln mit schematischen Vorschauen im Stil der Dashboard-Layouts:
  **Unten** (Standard), **Mitte**, **Oben**. Auswahl sofort persistent.

## Nachrichtendarstellung

In der Dashboard-Activity, kein Android-Systemoverlay und keine Anzeige über anderen Apps.
Das abgerundete Fenster verwendet **SECONDARY** als deckenden Hintergrund und einen
**3-dp-ACCENT-Rahmen**. Farbige Original-App-Icons, sofern über Android verfügbar;
sonst neutrales Glocken-Symbol. Absender oben links direkt neben dem Icon, Nachricht
vollständig darunter, soweit von der sendenden App bereitgestellt. Keine neuen Sounds,
Vibrationen, Antworten oder automatischen Appwechsel. Das Originalbanner von Android
wird nicht unterdrückt und die Quellbenachrichtigung nicht gelöscht.

- Unten: am nutzbaren Bildschirmende; inhaltshoch, maximal bis zur Oberkante der Karte.
  Dashboard-Datenfelder werden nicht verdeckt; die untere Bedienleiste kann überdeckt sein.
  Auf Dashboard 10 ohne Karte gilt stattdessen die maximale Höhe von 320 dp.
- Mitte: zur nutzbaren Bildschirmfläche zentriert; inhaltshoch, maximal **320 dp**.
- Oben: unterhalb der Android-Status-/Aussparungsfläche angedockt; maximal **320 dp**.
- 320 dp sind eine ungefähre 5-cm-Darstellungsgröße, kein Versprechen physisch vermessener
  Zentimeter auf jedem Android-Gerät. Immer zusätzlich auf die verfügbare Höhe begrenzen.
- Überlanger Inhalt scrollt innerhalb des Fensters. Während einer Touch-/Scrollgeste
  pausiert der Timeout; danach beginnt die eingestellte Dauer erneut.
- Kleines X schließt die aktuelle Nachricht. Folgemeldungen erscheinen nacheinander,
  höchstens acht wartende Nachrichten; bei Überlauf die älteste wartende verwerfen.
- Nur im sichtbaren Dashboard außerhalb des Editors. Einstellungsdialoge werden nicht
  überdeckt; in dieser Situation eingehende Meldungen werden nicht für später archiviert.
- Appwechsel, Deaktivierung, Quellenauswahländerung oder Diensttrennung verwerfen die
  sichtbaren und wartenden Nachrichten. Zurückkehren spielt keine alten Nachrichten ab.

## Daten und Architektur

`MessageNotificationService` ist ein `NotificationListenerService`, durch Androids
`BIND_NOTIFICATION_LISTENER_SERVICE` geschützt. Kein Accessibility-Service und keine
fremden Chatdatenbanken. Android-Zugriff, Hauptschalter und einzelne Quellenauswahl
müssen gleichzeitig aktiv sein. Kein Auslesen aktiver Altbenachrichtigungen beim Start.
App-Start darf bei bereits erteiltem Zugriff eine erneute Systembindung anfordern.

`MessageNotifications` extrahiert die neueste MessagingStyle-Nachricht mit Absender
oder EXTRA_BIG_TEXT / EXTRA_TEXT_LINES / EXTRA_TEXT. Eigene MessagingStyle-Antworten,
Gruppenzusammenfassungen, laufende Statusmeldungen und leere Inhalte ignorieren.
Identische Aktualisierungen pro Android-Benachrichtigungsschlüssel deduplizieren;
gleicher Text mit neuem MessagingStyle-Zeitstempel gilt als neue Nachricht. App-/Android-
seitig gekürzte oder verborgene Texte lassen sich nicht nachladen. Obergrenze 32.768
Zeichen Text und 512 Zeichen Absender pro Meldung, höchstens 256 reine Hash-Merkmale
und 128 beobachtete Quellpakete im Arbeitsspeicher.

**Keine Nachrichten, Benachrichtigungsschlüssel oder Inhalts-Hashes dauerhaft speichern,
in Diagnosen schreiben, an Server senden oder in FIT übernehmen.** Dauerhaft bleiben
nur Aktivierung, Filter, Paket-Auswahl, Position und Anzeigedauer. Vorschauen verwenden
abstrakte Layoutzeichnungen, keine tatsächlichen Nachrichten. Keine neuen Abhängigkeiten.

## Verifikation

MessageNotificationsTest: vollständiger von Android bereitgestellter Text, MessagingStyle-
Absender, eigene Antworten, Gruppenzusammenfassungen, erforderliche Freigabe/Auswahl,
Deduplizierung, persistente reine Konfiguration, Fensterhöhe, Warteschlange, Ausblendung
und Verwerfen. MessageNotificationUiTest: Auswahl-/Filter-/Slider-/Positionsbedienung,
Android-Freigabeintent, drei reale Dashboard-Anker und Erhalt offener Einstellungen beim
Appwechsel. Bestehende Menüreihenfolge-Tests um den neuen Eintrag erweitert.

Echter Android-Benachrichtigungszugriff und Zustellung aus WhatsApp/Element auf dem
Nutzergerät müssen mit der Test-APK geprüft werden; Frameworktests ersetzen das nicht.

## Einheitliche Abschnittsnavigation

Apps verwendet BicycleForm.section im zentralen
BicycleStickyScrollView. Überschriften docken beim Scrollen oben an, werden vom
folgenden Abschnitt abgelöst und übernehmen die gemeinsamen Abstände/Theme-Regeln.

## Kompaktere Position und Anzeigedauer

Umgesetzt am 17.10.2026 nach Build 30:

- Die Header **Position** und **Anzeigedauer** entfernen. Positionsauswahl und Zeitfunktion bleiben erhalten.
- **Anzeigedauer** links als Beschriftung, rechts daneben den Slider in derselben Zeile.
- Beschriftung vertikal genau auf die Sliderlinie zentrieren, unabhängig vom darüberstehenden Zahlenwert.
- Den gewählten Sekundenwert oberhalb des Sliders anzeigen.

Ein zweispaltiges Grid hält den Sekundenwert über dem Slider und die Beschriftung unabhängig davon mittig zur Sliderlinie. Positionsauswahl, App-Auswahl und sofortige Speicherung bleiben erhalten.
