from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from scripts.frame import FrameError, encode_frame, normalize_hex, parse_envelope


class FrameTests(unittest.TestCase):
    SAMPLE = "6822000000012026050100000100020F3232303432320000010000000000000000000000563A"

    def test_compact_and_spaced_hex_are_equal(self):
        spaced = " ".join(self.SAMPLE[index:index + 2] for index in range(0, len(self.SAMPLE), 2))
        self.assertEqual(normalize_hex(self.SAMPLE), normalize_hex(spaced))

    def test_length_and_envelope(self):
        parsed = parse_envelope(bytes.fromhex(self.SAMPLE), "ykc", "V1.5")
        self.assertEqual(parsed.spec.kind, "legacy")
        self.assertEqual(parsed.frame_type, 0x01)
        self.assertEqual(parsed.length, 34)
        self.assertTrue(parsed.crc["match"])

    def test_v21_has_two_byte_length_and_send_time(self):
        frame = encode_frame("ykc", "V2.1", 0x01, bytes(130), sequence=7, send_time="2026-05-01 12:34:56")
        parsed = parse_envelope(frame, "ykc", "V2.1")
        self.assertEqual(parsed.spec.kind, "v2.1")
        self.assertEqual(parsed.sequence, 7)
        self.assertEqual(parsed.send_time, "2026-05-01 12:34:56")
        self.assertEqual(parsed.length, 141)
        self.assertTrue(parsed.crc["match"])

    def test_extra_bytes_are_rejected(self):
        with self.assertRaises(FrameError):
            parse_envelope(bytes.fromhex(self.SAMPLE) + b"\x00", "ykc", "V1.5")

    def test_cp56_year_outside_supported_range_is_rejected(self):
        from scripts.codecs import cp56_encode
        with self.assertRaises(ValueError):
            cp56_encode("2100-01-01 00:00:00")

    def test_truncated_frame_is_rejected(self):
        with self.assertRaises(FrameError):
            parse_envelope(bytes.fromhex(self.SAMPLE[:-4]), "ykc", "V1.5")


if __name__ == "__main__":
    unittest.main()
