# Read-only Bosch MCSP in B-Cycle

B-Cycle now uses only the hardware-tested MCSP path. The user confirmed speed and
rider power during a real ride with a System Controller v20.9.0 and Mini Remote,
a Xiaomi Poco F7 Ultra / Android 16, alongside Bosch Flow. Earlier stationary tests
with a freely spinning rear wheel displayed zero rider watts. The official PDF's
LDI service repeatedly produced empty reads; that comparison path is removed.
The user subsequently confirmed the automatic connection and all additional fields
in 0.2.0. The attached diagnostic contained all 18 numeric fields, 721 notifications
and no decoding errors. The dashboard, GPS and 0.4.0 recording changes preserve this BLE path.

## Sources and attribution

Protocol facts, mapping and this adapted summary are based on **redundo.app**,
*Bosch Smart System — verified BLE field map*, under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/):

- [Transport](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/TRANSPORT.md)
- [Decoder card](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/DECODER-CARD.md)
- [Request grammar](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/BLE-ACCESS.md)
- [Startup handshake](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/STARTUP-HANDSHAKE.md)

Names, readability/subscribability and the mode-name/list schemas were cross-checked
against Remko Weijnen's [Bosch BES3 address registry](https://github.com/rweijnen/bosch-bes3-reader/blob/cd9a5510ae1091c467d8d5a586a23723c17a9249/src/address-registry.json)
(CC BY 4.0) and [protocol definitions](https://github.com/rweijnen/bosch-bes3-reader/blob/cd9a5510ae1091c467d8d5a586a23723c17a9249/src/protocol.js)
(MIT, notice below). Startup replies were checked against its
[BLE transport](https://github.com/rweijnen/bosch-bes3-reader/blob/cd9a5510ae1091c467d8d5a586a23723c17a9249/web/transport-webble-mcsp.js).
Published rider-power fixtures, including empty zero-valued pushes, were checked
against [RobbyPee's examples](https://github.com/RobbyPee/Bosch-Smart-System-Ebike-Garmin-Android).
No GPL source code is included. The Kotlin implementation is independent.

Adaptations: a bounded read-only telemetry subset, native Android UI, foreground
retry policy and filtering diagnostics. No endorsement by the authors or Bosch
is implied. The redundo.app measurements primarily concern a BDU3740 / firmware
20.x with LED Remote and Kiox 300; delivery of all fields is not guaranteed on
other configurations. Fields with uncertain meanings/scales are not displayed.

## GATT, startup and requests

Use the normal Android GATT-client path and existing OS bond. No accessory
advertisement or local GATT server. Flow can keep its connection.

| Purpose | UUID |
| --- | --- |
| Service | `00000010-eaa2-11e9-81b4-2a2ae2dbcce4` |
| Bike → phone notify | `00000011-eaa2-11e9-81b4-2a2ae2dbcce4` |
| Phone → bike write without response | `00000012-eaa2-11e9-81b4-2a2ae2dbcce4` |

Request MTU 247, discover services, enable notify locally, then write CCCD `0100`.
Only after the successful descriptor callback start the write queue. Subscribe to
18 numeric fields and the mode-name/range fields below, then request snapshots
for all 20. Speed and rider power remain first. Manual reread repeats snapshots;
there is no periodic polling. Requests are serialized with a 300 ms callback gap,
a five-second write-callback timeout and a bounded 64-write backlog (40 initial
requests plus room for startup replies). Requests take about 12 seconds to finish;
notifications are accepted throughout. Refused optional reads do not stop reception.
The sources disagree about whether range READ is accepted on every bike; it is
subscribed as well, and refusal is logged without inventing a value.

Host identity `0x4080` follows published examples. Full responses must address this
host; Flow's `0x4180` responses are discarded. Unsolicited pushes have no destination.
Message types READ=0 / reply=1, SUBSCRIBE=6 / reply=7, sequence is four bits.

If the bike READs local MobileApp field `0x40AA`, answer field 3=true (`18 01`).
If it WRITEs startup stage `0x40A9`, acknowledge stages 0–10 with matching sequence.
Do not wait for boot stage 9 on an already-running bike or acknowledge other app
fields. Exact published examples:

```text
RX 30 05 20 80 C0 AA 05
TX 30 07 40 AA A0 80 15 18 01
RX 30 07 20 80 C0 A9 27 08 09
TX 30 05 40 A9 A0 80 37
```

Outgoing writes contain only READ, SUBSCRIBE or these two local startup replies.
There are no motor, assist-mode, lock, pairing, configuration or firmware writes/RPCs.

## Framing and fields

```text
header = channel << 5 | endOfMessage << 4 | length >> 8
lengthLow = length & 255
```

Walk segment lengths; never split on literal byte `30`. Reassemble channel-1
continuations, skip transport and stored-log channels. Bounds: at most 514 bytes
per notification, also checked against negotiated MTU; 4096 bytes per reassembled
message. Malformed packets do not partially replace values and reset reassembly.

| Address (top bit cleared) | Displayed measurement | Conversion |
| --- | --- | --- |
| `182D` | Displayed speed | /100 km/h |
| `185B` | Rider power | W |
| `185A` | Rider cadence | /2 rpm |
| `1809` | Assist mode | index into bike-supplied names |
| `185D` | Motor power | W |
| `1814`, `1815` | Rider / motor torque | /20 Nm |
| `0088` | Battery state of charge | % |
| `1818` | Odometer | metres → /1000 km |
| `008B` | Internal battery-cell temperature | zigzag-decode, /10 °C |
| `0091` | Remaining rider-facing battery energy | /10 Wh |
| `009C` | Cumulative delivered battery energy | Wh, lifetime counter |
| `224A`, `224B` | Activity average / maximum rider power | W |
| `2248`, `2249` | Activity average / maximum cadence | /2 rpm |
| `2246` | Activity average speed | /100 km/h |
| `2254` | Activity rider share of propulsion energy | % |
| `180D` | Configured long mode names | repeated field-1 UTF-8 strings |
| `1857` | Range estimates per configured mode | repeated uint32 field 1, packed or unpacked, km |

Numeric values use uint32 protobuf field 1. Domain limits remain uint16 for
speed/power/torque/cadence, 0–100 for percentages and 0–255 for mode index; counters
support the full uint32 range. Temperature is a zigzag signed integer, not raw/10.
Unsolicited true speed `1808` is accepted as a fallback until displayed speed
`182D` is first received; afterwards it cannot overwrite displayed speed, including
an explicit displayed zero. Both addresses remain separately identifiable in logs.

A push sets the source's top bit and appends the scalar directly. Its missing
non-optional scalar defaults to zero. Full READ/SUBSCRIBE replies have a five-byte
source/destination/type envelope: check the destination success bit and skip the
envelope before parsing. Empty full responses acknowledge without a measurement.
An explicit zero-valued push is different. No silence is converted into zero.

Mode-name/range lists are limited to 16 entries; names to 128 UTF-8 bytes with no
control characters. Mode names are read, not hardcoded. A range list may omit OFF;
only align names when lengths match (or names have one extra leading OFF), otherwise
show numbered slots. List defaults are empty lists, not fabricated range values.

## Persistence, lifecycle and diagnostics

Selected device name/address, seven stable dashboard field IDs and the GPS enable
switch are stored in private SharedPreferences;
Android backup/device transfer is disabled. Selection connects immediately.
Opening/resuming the activity connects to the saved device. Foreground retries use
3/6/12/24/30-second backoff; successful setup resets it. No concurrent attempts.
Sensor enable/disable preferences persist across app restarts. Leaving the foreground
stops this app's GATT client unless an explicitly started recording is running.
During recording the foreground service shares the same sensor runtime as the
Activity and keeps it active until STOP. No boot receiver is used.
Retired clients are closed after their disconnect callback, with a bounded fallback.
Late callbacks cannot resume stopped requests or update a new session.

UI shows simple connection status and last recognized data-packet age; no protocol
selection or packet counters. Values unavailable in this connection appear as a
dash. Partial updates retain earlier values. Common packet age does not establish
freshness of each field; cadence can be missing depending on session timing, and
activity summaries may update sparsely and reset on Bosch's activity boundaries.
Bosch activity summaries are not B-Cycle trip counters and are not reset by the
app. B-Cycle's new bike trip-distance field sums positive odometer deltas; its
START action resets only that derived counter and its own GPS counters.
GPS has independent fields and processing; see [GPS notes](gps.md).

Diagnostics remain bounded in memory until explicit document-picker export. Count
notifications, bytes, complete/ignored messages, control frames, responses and
errors. Keep known numeric per-field session statistics, last 24 numeric samples,
and independently last 24 rider-power samples. Do not retain raw mixed MCSP packets,
Bluetooth addresses, device/mode names, component identifiers or stored logs.
Each new connection attempt starts a new diagnostic report; export freezes the
report before opening the picker; a started recording continues independently. No automatic upload or internet use.

## MIT license notice for the cross-checked protocol implementation

MIT License

Copyright (c) 2026 rweijnen

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Fahrradlicht (0.6.19)

Zusätzlich wird DriveUnit `BIKE_LIGHT`, Adresse **0x181C / 6172**, als optionale,
rein lesende Subscription und READ angefragt. Mapping: MCSP-Boolean **0 = aus,
1 = an**. Quelle: oben verlinkte BES3-Registry (`BIKE_LIGHT`, `kind: bool`,
readable/subscribable). Die Registry kennzeichnet diese Zuordnung noch nicht als
hardwarebestätigt; ein Test am tatsächlichen System Controller steht aus.
Keine WRITE-/Toggle-/Konfigurationsbefehle werden gesendet. Unbekannte Werte werden
protokolliert und verworfen, ohne die übrigen Messwerte desselben Pakets zu verlieren.

Die offizielle Bosch-PDF V1 vom 01.05.2026 nennt auf S. 18–19 dagegen das
**LDI-Protobuf**-Feld `bike_light = 17` mit Enum 0=invalid, 1=off, 2=on.
Das ist ein anderer Transport und ausdrücklich **nicht** die Kodierung von MCSP.
Der funktionierende MCSP-Verbindungsweg bleibt bestehen. Ein leerer Subscription-ACK
ist kein Zustand; ein expliziter leerer scalar-Push entspricht dem Booleanwert false.

„Fahrradlicht“ steht in der Feldauswahl unter E-Bike → Licht und im FIT-Abschnitt
von „Sensoren & Aufzeichnung“. Textanzeige An/Aus, bei fehlendem/ungültigem Zustand
„—“. Für Anzeige/Overlay und FIT wird der Status bei Verbindungsverlust oder nach
60 Sekunden ohne Update nicht weiter als aktuell verwendet (wie andere seltene
Status-/Akkuwerte). Nur ein bekanntes, frisches An zeigt die statische rote Lucide-
Glühbirne rechts neben dem mittleren Kartenfeld; Record-Punkt und Glühbirne funktionieren
auch ohne belegtes mittleres Feld. Kein Touchziel, keine Animation, kein weiterer Timer.

FIT enthält bei Auswahl das Developer Field `BIKE_LIGHT`, Einheit `bool`,
standardkonforme Float64-Werte 0/1 je Record, fehlend als FIT-invalid. Es gibt keine
Zuordnung zu einem erfundenen Garmin-Nativfeld. Garmins Decoder/Activity Validator
prüfen Licht an, aus und fehlend; MCSP-Tests prüfen Requests, ACK/Push und unbekannte
Werte neben anderen Messwerten; ein nativer UI-Test prüft Farbe/Position und das
Zusammenspiel mit dem Record-Punkt. Wetterfelder bleiben ausschließlich Anzeigegrößen.
