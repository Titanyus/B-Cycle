# Vorbereitung: Benachrichtigungen

Dieser Abschnitt beschreibt den nächsten Funktionsschritt; er ist noch nicht
Bestandteil der UI-Vereinheitlichung in APK 0.7.15.

## Einstellungsbereich

Eigener Screen „Benachrichtigungen“ im Haupt-Einstellungsmenü, mit der bestehenden
Fullscreen-/Chevron-/Sofortspeicher-Konvention und zentralen Schrift-/Schalterrollen.

- „Benachrichtigungen anzeigen“ aktiviert/deaktiviert die Funktion.
- Fehlenden Android-Benachrichtigungszugriff über den systemeigenen
  Benachrichtigungszugriff-Dialog anfordern. Nach Rückkehr tatsächliche Freigabe prüfen.
  Bei späterem Entzug den effektiven Zugriff anzeigen, nicht einen aktiven Empfang behaupten.
- Auswahl installierter Apps sowie beobachteter Benachrichtigungsquellen. Android
  liefert keine vollständige Liste aller Apps, die künftig Nachrichten senden werden.
- Messenger-Filter blendet andere Apps nur aus. Vorhandene Auswahlen bleiben gespeichert.
- Jeder Messenger separat und persistent auswählbar; keine App automatisch auswählen.
- Übliche Messenger über stabile Paketkennungen vorfiltern, nicht über lokalisierte Namen.

## Technische Grundlage

NotificationListenerService mit Androids BIND_NOTIFICATION_LISTENER_SERVICE;
kein Accessibility-Service zum Lesen anderer Apps. Appzugriff und einzelne
Appauswahlen unabhängig speichern. Keine Nachrichten dauerhaft speichern oder in
FIT übernehmen. Nur ausgewählte Quellen an die spätere Darstellung weiterreichen.
Benachrichtigungsaktualisierungen anhand ihrer Schlüssel deduplizieren;
Gruppenzusammenfassungen berücksichtigen. Privat ausgeblendete Inhalte bleiben
gegebenenfalls unlesbar. Geräte-/Appnamen sind dynamische Nutzdaten.

## Noch festzulegen

Ziel: eintreffende Nachrichten länger und wesentlich vollständiger als das kurze
Android-Banner anzeigen. Erweiterte Benachrichtigungstexte (EXTRA_BIG_TEXT) und
MessagingStyle-Nachrichten verwenden, soweit die jeweilige App sie bereitstellt.
BICYCLE liest keine fremden Chatdatenbanken und lädt keine zusätzlichen Inhalte
nach. Nachrichtentexte nur vorübergehend im Arbeitsspeicher halten und nach der
Anzeige verwerfen; auch nicht in Diagnosen oder FIT protokollieren. Dauerhaft
bleiben nur Aktivierung, Filter und ausgewählte App-Paketkennungen.

Position, Anzeigedauer und Verhalten bei Folgemeldungen bleiben festzulegen.
Keine Vorlesefunktion festgelegt.
