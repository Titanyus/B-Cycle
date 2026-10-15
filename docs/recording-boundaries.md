# Aufnahmegrenzen für App-Fahrtzähler

Vor START ist die Fahrtdauer 0. GPS darf Position, Geschwindigkeit, Höhe und
Fahrtrichtung für die Live-Anzeige liefern, sammelt aber noch keine Fahrtstrecke,
Bewegungs-/Standzeit, Höchstgeschwindigkeit oder Höhenmeter.

RideRuntime aktiviert GpsRide.collecting erst beim Aufnahmestart und deaktiviert
es nach der letzten Abschlussprobe. Die Fahrtdauer stammt immer aus der
Aufnahmeuhr, auch vor der ersten Aufnahme und ohne GPS. Nach STOPP bleiben die
Fahrtzähler eingefroren, bis START sie für die nächste Fahrt zurücksetzt.

Der GPS-Filter verwendet seine räumliche Kontinuität weiterhin für Live-Geschwindigkeit
und Steigung; diese internen Filteranker sind keine zusätzliche Aufzeichnung.
Aktuelle Position, Höhe und Geschwindigkeit bleiben unabhängig von der Aufnahme.

App-eigene Kadenz-/Pulsdurchschnitte, maximaler Puls und die Fahrtstrecke aus dem
Rad-Kilometerzähler werden ausschließlich während der Aufnahme aktualisiert.
Rohwerte vom Sensor bleiben erhalten. Vom Bosch-Gerät selbst gelieferte Kennzahlen
werden dadurch nicht umgerechnet oder inhaltlich verändert.

FIT-Journal, Karten-Fahrttrack, Graphhistorie und Performance Engine hatten bereits
eine Aufnahmebedingung und verwenden weiterhin denselben Aufnahme-/Abschlussweg.
Keine neuen Hintergrundtimer, keine zweite Sensorverbindung oder Aufzeichnung.

Verifikation: 43 gezielte Tests bestanden, inklusive realem Runtime-/Service-/FIT-
Abschlussweg auf API 33/35/36, GPS-Preview vor dem ersten START, Stillhalten aller
GPS-Tripwerte nach STOPP, Kadenz-/Puls-/Radstrecken-Grenzen und Karten-Livebetrieb.
Keine neue APK gebaut; der Fix ist eine Quellcodeänderung nach Version 0.7.15.
