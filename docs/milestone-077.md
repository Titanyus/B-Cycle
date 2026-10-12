# B|Cycle 0.7.7 – Daten & Speicher und acht Dashboard-Vorlagen

Tag `v0.7.7` sichert Quellcode, Tests, Ressourcen, Dokumentation und die signierte Test-APK.
Ältere Stände und APKs bleiben erhalten.

Enthalten: [Daten & Speicher mit selektiver Sicherung](data-storage-backup.md) und
[zwei neue Dashboard-Vorlagen sowie feste graue Bezeichnungen](dashboard-presets.md).

VersionName 0.7.7, VersionCode 45, MinSdk 33, TargetSdk 35 separat geprüft.
40 gezielte Tests erneut bestanden: DashboardViewTest (12), DashboardLayoutTest (5),
SettingsMenuTest (4), BoschModeGlowTest (9), SettingsBackupTest (10).
Release-Build einschließlich Release-Lint-Vital erfolgreich; Test-/Build-Gesamtlauf 56s.
Zuvor bestanden 294 Tests der vollständigen Suite für Daten & Speicher. Keine neue
vollständige Suite, kein vollständiger Lint oder Gerätetest bei diesem APK-Build.
Die bekannte Produktionswarnung zum veralteten `onBackPressed()` bleibt bestehen.

APK: 1054393 Byte. SHA-256 `dba9a21fd52aeb5a78b6fa483376eb284bceea622cd23f0a1b216cf7ed9b6508`.
Signatur mit dem bisherigen Android-Testschlüssel geprüft und identisch; Updateinstallation möglich.
Keystore, private Messdateien, SDKs und Caches werden nicht veröffentlicht.

Am Telefon zu prüfen: neue Dashboard-Vorlagen und Thumbnails, einheitliche Feldbezeichnungen,
Daten-&-Speicher-Navigation, Export/Import ausgewählter Einstellungen und bestehende
Speicher-/WebDAV-Zugänge. Cloud-Passwörter sind bewusst nicht Teil der Sicherung.
