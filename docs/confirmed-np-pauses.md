# Bestätigte E-Bike-Pausen ausschließlich für NP

Nutzerregel29.10.2026: eine Aufnahme läuft bis zum manuellen STOP. Lange Rad-Abschaltungen dürfen als bestätigte Pause die NP-Datenabdeckung nicht entwerten. Aufnahmedauer, Journal, FIT-Sitzung, Rohwerte und automatische Wiederverbindung bleiben erhalten. Ein Defekt des für die Bosch-Motorsteuerung erforderlichen Sensors ist vom Ausfall der BLE-Übertragung zur App zu unterscheiden.

## Nachweis

ConfirmedBikePause verwendet ausschließlich bereits vorhandene Messungen: E-Bike-Profil, keine Radverbindung, keine verfügbare gewählte Leistungsquelle und mindestens10s kontinuierlicher GPS-Stillstand unter0,7m/s. Der GPS-Fix muss höchstens5s alt und mit höchstens25m Genauigkeit akzeptiert sein (GpsRide nimmt bereits nur höchstens20m an). Es wird kein GPS zusätzlich gestartet. Neue Radverbindung oder verfügbare Leistung beendet den Zustand, ebenso Bewegung, ungenaue/fehlende/veraltete GPS-Belege oder eine Verarbeitungslücke. Ohne GPS-/Stillstandsnachweis bleibt der Zeitraum unbekannt. Einzelne fehlende Wattwerte bei bestehender Radverbindung werden nicht zur Pause erklärt. Normales Rollen/Abfahren wird nicht entfernt, gültige Nullen bleiben Nullen.

Die10s vor Bestätigung werden konservativ nicht rückwirkend umklassifiziert. Die eigentliche Aufnahme wird niemals angehalten. Andere Fahrradtypen/Indoor erhalten dadurch keine neue automatische Pause.

## Berechnung und Datenweg

Die interne BCYCLE_NP_PAUSED-Markierung wird als benannte bool-Spalte im bestehenden BCY7-Journal gespeichert. PerformanceEngine.acceptSample verarbeitet dieselben Ereignisse und Pausenbelege live sowie beim FIT-Encoding. PerformanceChannel.NP_PAUSE ist hinten angefügt, Modellversion5; ältere Journale bleiben lesbar. Neue Signale ändern keine bereits gespeicherten Originale.

Nur unbekannte Leistung in bestätigter NP-Pause wird von NP-Zeit und möglicher30s-Fensterzahl ausgeschlossen. Es entstehen keine Nullwatt-Messungen. Fenster werden an Pause/Weiterfahrt gebrochen, niemals über die Pause verbunden. Echte neue Leistungsdaten dürfen durch eine Pausenmarkierung nicht verdeckt werden. Jeder positive Beleg gilt höchstens5s, Folgebelege werden mit vorhandenen Aufnahmeticks erneuert; unaufgezeichnete Zeitlücken werden nicht pauschal als Pause behauptet.

PerformanceQuality.npPausedMs/npPowerCoverage sind von der bisherigen Gesamtaufnahme-Qualität getrennt. NP verwendet weiterhin90% Daten- und Fensterabdeckung, jetzt bezogen auf NP-Auswertungszeit. Arbeit/Kalorien/Ratio behalten ihre bisherige95%-Gesamtaufnahme-Regel. FTP wird nicht über eine Leistungslücke verkettet; EF behält die vollständige Paarung der ausgewerteten Puls-/Leistungsintervalle.

FIT: die bool-Markierung wird bei ausgewähltem NORMALIZED_POWER und in der privaten vollständigen Auswertungsdatei mitgeführt. NP_DATA_COVERAGE und NP_PAUSED_TIME ergänzen die bestehenden Qualitätsangaben. Weder FIT-Timerereignisse noch total_elapsed_time/total_timer_time werden durch diese NP-Regel verändert. RouteAnalysisRead nutzt die ausdrücklich gespeicherten Belege; ohne Marker keine neue Pauseheuristik für alte oder fremde FIT-Dateien. Erneuter Export erhält Marker nur bei gewähltem NP. Keine neue Einstellung, keine Änderung anderer Sensor-/Feldwahl.

## Prüfung

Neue Tests: ConfirmedBikePauseTest (frische/genaue/kontinuierliche Belege, Bewegung/Signalverlust/Reset, lange bestätigte Pause, unveränderte Gesamtdauer, getrennte30s-Fenster, unbekannter Ausfall, gültige Wattwerte, auslaufende Belege); ConfirmedPauseFitTest (Live=Journal, Standard-NP-Rundung auf1W, eine FIT-Sitzung mit kompletter Dauer, tatsächliche Nachberechnung nach Entfernen nur des NP-Summaryfelds, Marker bei erneutem Export/Feldwahl); zusätzlicher RecordingFlowTest für reale Runtimeanbindung, ohne sichtbare Activity, Ende des Pausenzustands ohne GPS, Aufnahme weiter aktiv.

Bestehende PerformanceEngine-/Journal-/FIT-, GPS-, Detail-, Neuexport- und45min-Aufnahmeprüfungen wurden gezielt ausgeführt. Keine Gesamtsuite oder physischer Langzeittest. Private Nutzer-FIT bleibt außerhalb des Repositorys.


Veröffentlichung29.10.2026: Änderungen nachBuild104 sind in **APK0.8.64/Build105** enthalten. [Download und Buildnachweis](build-105.md). Frühere Angaben zu fehlender APK sind Umsetzungshistorie.
