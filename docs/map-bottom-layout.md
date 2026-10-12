# Untere Kartenwerte und Zoom-Leiste

Korrektur in 0.7.2: Eine dynamische Regel in `MapPanel.onMeasure` hatte die zuvor geänderten Anfangsabstände wieder überschrieben. Vor allem die Zoom-Leiste reservierte weiterhin die Höhe der Kartenwerte und blieb dadurch zu weit oben.

Kartenwerte und Zoom-Leiste verwenden jetzt auch beim Vermessen denselben unteren Abstand von 12 dp. Die Unterkante von Minus liegt auf der Unterkante der Kartenwerte; Plus bleibt darüber. Die Kartenquellenangabe reserviert bei sichtbarer Zoom-Leiste deren rechte Spalte. Ein kleiner innerer Abstand der Werte schützt die Textzeile vor Überschneidung mit der Quellenangabe. GPS-Status und Steuerung kurzer Kartenausschnitte bleiben berücksichtigt.

Die vorhandenen Kartentests prüfen den gemeinsamen Abstand auf Dashboard-Seiten und in Vollbild, tatsächliche Textgrenzen oberhalb der Quellenangabe, freie Zoom-Spalte und die Bedienung einschließlich Nachtmodus und Ausblenden.

## Gedrehte Attribution

Der Quellenhinweis steht anschließend um 90° nach oben gedreht an der linken unteren Dashboard-Kartenkante. `MapCreditView` misst Text und Touchfläche passend zur Drehung; auf kurzen Kartenflächen bricht der vollständige Text um, statt abgeschnitten zu werden. Schriftgröße, Lesbarkeit, Inhalt und Öffnen der Quellen-/Lizenzseite bleiben erhalten. Der Hinweis wird weiterhin nicht automatisch ausgeblendet. Kartenwerte und GPS-Status reservieren nur den schmalen linken Streifen; der gemeinsame untere Abstand mit Minus bleibt 12 dp. Die Detailkarte bleibt unverändert.
