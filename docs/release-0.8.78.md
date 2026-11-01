# B|CYCLE0.8.78

- Aufnahme-Anzeige animiert nur während der beiden500-ms-Überblendungen; dazwischen keine eigenen Animationsframes. Rhythmus und Aussehen erhalten. Trainingshilfe bleibt weich überblendet.
- HRV: Kurzempfehlung in eigener Zeile, restlicher Text zusammenhängend. Messdiagramm10% höher mit12dp zusätzlichem Kreisabstand.
- Vollhohe, zentrierte Kennzahlkacheln; Weitere öffnet ein rechtsbündiges schwebendes Dropdown mit drei gleich breiten Zusatzkacheln. Auswahl schließt es, Weitere bleibt markiert.
- Einstellungen: HRV & Tagesbefinden direkt nach Fahrradprofile. Erststartangebot als einfacher Schalter vor Vibrieren, ohne Erklärung.
- Fahrradprofile/Karten: redundanten GPX-Anzeigeschalter entfernt; Dashboardsteuerung erhalten. Sensoren nur bei Zuordnung oder Suchfund; Ausschalten/Trennen behalten sie sichtbar, Entfernen blendet sie aus. GPS bleibt immer sichtbar.

Gezielt geprüft: fünf betroffene HRV-Fälle, Menüanordnung und Sensorlebenszyklus über freigeschaltete Profiltypen. Typografie/Atlas/Diff erfolgreich. Kein neuer Gesamtlauf und kein physischer Gerätetest.

[CPU-/GPU-Abschätzung und Vorschlag für einen Akkuschonmodus](dashboard-energy-analysis.md). Gezielt geprüft: Animationskurve, Ruhephasen, Abbruch/Neustart und Indoor-/Outdoor-Lebenszyklus.

Noch kein Akkuschonmodus und kein Wechsel zu VTM implementiert.

[Buildnachweis und Download](build-119.md).
