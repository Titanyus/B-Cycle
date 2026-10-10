# Read-only Bosch MCSP subset in B-Cycle 0.1.7

This is a new experimental route for live speed and rider power. It is separate
from the official LDI service in the supplied Bosch PDF. The user's 0.1.6 log
already lists this service, its notify characteristic, and its write-without-response
characteristic. That establishes GATT availability, not successful telemetry access.

## Sources and attribution

Protocol facts, field mapping and this adapted summary are based on **redundo.app**,
*Bosch Smart System — verified BLE field map*, under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/):

- [Transport](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/TRANSPORT.md)
- [Decoder card](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/DECODER-CARD.md)
- [Request/response grammar](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/BLE-ACCESS.md)
- [MobileApp startup handshake](https://github.com/radiohound/bosch-smart-system-ble/blob/97def2bbbfae0d373e67d9cd3911c1dae35f4323/docs/STARTUP-HANDSHAKE.md)

Adaptations: a two-measurement subset, a native Kotlin implementation, bounded
reassembly, serialized requests, and filtering of diagnostic output. This does
not imply endorsement by redundo.app or Bosch. The source's hardware findings
primarily concern a BDU3740 / firmware 20.x with LED Remote and Kiox 300; the user's
System Controller is a different configuration.

The startup response framing was cross-checked against Remko Weijnen's
[BLE-MCSP transport](https://github.com/rweijnen/bosch-bes3-reader/blob/cd9a5510ae1091c467d8d5a586a23723c17a9249/web/transport-webble-mcsp.js)
and message-type definitions (MIT license). Its acknowledgement format was
independently implemented below; its license notice is included at the end.
Published rider-power fixtures, including an empty zero-valued push, were also
cross-checked against [RobbyPee's examples](https://github.com/RobbyPee/Bosch-Smart-System-Ebike-Garmin-Android).
No source code from that GPL project is included.

## GATT and startup

The phone uses the ordinary Android GATT-client path and its existing OS bond.
It can attach to an existing bike link. Flow may remain connected.
There is no accessory advertisement or local GATT server.

| Purpose | UUID |
| --- | --- |
| MCSP service | `00000010-eaa2-11e9-81b4-2a2ae2dbcce4` |
| Bike → phone, notify | `00000011-eaa2-11e9-81b4-2a2ae2dbcce4` |
| Phone → bike, write without response | `00000012-eaa2-11e9-81b4-2a2ae2dbcce4` |

Request MTU 247, discover services, enable notify locally, then write CCCD `0100`.
Only after the descriptor-write callback succeeds does the characteristic-write
queue start. Subscribe to `0x182D` and `0x185B`, then issue READ requests for both.
The manual reread button repeats the two READ requests. No periodic polling or
background service is added.

The host identity is `0x4080`, verified in the redundo.app request examples.
The full-response destination must match that host; a response to Flow's distinct
`0x4180` identity is ignored. Unsolicited measurement pushes have no destination.

If the bike READs MobileApp field `0x40AA`, answer with protobuf field 3 = true
(`18 01`, staged startup supported). If the bike WRITEs startup stage to local
field `0x40A9`, acknowledge the received stage and sequence. Accept stages 0–10;
do not wait for stage 9, because a bike already running may not send boot stages.
Do not acknowledge arbitrary MobileApp fields or send requests to bike settings.

Example, requester `0x2080`, READ sequence 5:

```text
RX 30 05 20 80 C0 AA 05
TX 30 07 40 AA A0 80 15 18 01

RX 30 07 20 80 C0 A9 27 08 09
TX 30 05 40 A9 A0 80 37
```

Every outgoing GATT write contains only a READ, SUBSCRIBE or one of these two
local startup replies. There are no WRITE/RPC commands to motor, lock, pairing,
configuration, navigation or firmware addresses.

## Framing and measurements

Each MCSP segment starts with two bytes:

```text
header = channel << 5 | endOfMessage << 4 | length >> 8
lengthLow = length & 255
```

Walk headers and lengths; do not split on byte `30`. Several segments can be
packed into a notification. Reassemble channel-1 continuations until their
end bit is set. Ignore transport commands and stored-log channels.
Allow at most 514 bytes per notification at the parser boundary, additionally
check the negotiated MTU in the Android connection, and bound an assembled
message to 4096 bytes. A malformed packet clears partial reassembly without
partially replacing displayed values.

| Bus field | Measurement | Conversion |
| --- | --- | --- |
| `0x182D` / push `98 2D` | Displayed bike speed | raw / 100 km/h |
| `0x1808` / push `98 08` | Bike speed, accepted if auto-pushed | raw / 100 km/h |
| `0x185B` / push `98 5B` | Rider power | raw watts |

For a push, the source's top bit is set and its two-byte address is followed
directly by a protobuf scalar wrapper. An addressed push with a missing
non-optional field 1 represents its default zero. For a full READ/SUBSCRIBE
response, skip the source, destination and type/sequence envelope before
decoding. A cleared success bit is a refusal; log the status code without
decoding it as data. An empty response is acknowledgement without a sample,
which differs from an empty, addressed zero-valued push.

Only READ and SUBSCRIBE for the two requested measurement addresses can be
generated. Request sequence numbers are four bits. There is one Android GATT
write in flight, a 300 ms gap after its callback, a five-second callback timeout,
and a maximum backlog of 16 writes. Startup replies precede queued measurements.
Stop cancels queued work; retired-client callbacks cannot revive it.

Only recognized measurement values refresh the displayed packet age. READ
snapshots and live pushes are distinguished in status. A speed update still does
not prove that the last power value is fresh; this test does not infer zeros from
silence.

## Diagnostics and limits

Count all selected-channel notifications, bytes, complete messages, ignored
messages, control frames and errors. Retain only numeric speed/power samples and
the known startup/request/status events. Do not retain raw MCSP notifications:
they can combine live values with component identifiers or stored-log traffic.
The UI shows reception counts even when there are no recognized measurements.

The exact Locus implementation is unknown. Assist mode alone does not establish
which channel Locus uses: redundo.app also reports it as undocumented LDI field 8.
The new MCSP route is justified by the user's repeatedly empty LDI responses,
the presence of MCSP in their service discovery, and public hardware-tested
protocol references. B-Cycle itself still needs a hardware test. If neither
values nor useful responses arrive, a capture of the working Locus BLE traffic
would provide stronger evidence than another speculative startup command.

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
