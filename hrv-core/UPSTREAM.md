Source: https://github.com/Titanyus/B-Cycle-HRV
Commit: 895de63a98828cc7a7ece813c618e2eb48321daa (0.3.2)
Imported 2026-10-20: core source/tests and 62 reference fixtures.

Host adaptation: low RR-duration coverage (<95%) is a warning, not a rejection by
itself. Excess duration, minimum usable data, artefact and interruption checks
remain active. No rescaling of RR or invented beats. Evaluation retains warnings
separately from rejection reasons. Policy: bcycle-hrv-v2-low-coverage-warning.
