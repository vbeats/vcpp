from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from scripts.crc import crc16_modbus, crc_wire_bytes
from scripts.frame import FrameError, encode_frame, normalize_hex, parse_envelope


class CrcTests(unittest.TestCase):
    def test_modbus_vector_and_wire_order(self):
        self.assertEqual(crc16_modbus(b"123456789"), 0x4B37)
        self.assertEqual(crc_wire_bytes(b"123456789"), bytes.fromhex("374B"))

    def test_invalid_hex_is_rejected(self):
        with self.assertRaises(FrameError):
            normalize_hex("680")
        with self.assertRaises(FrameError):
            normalize_hex("68 ZZ")

    def test_crc_mismatch_is_reported(self):
        frame = bytearray(encode_frame("ykc", "V1.5", 0x01, bytes(30)))
        frame[-1] ^= 0xFF
        parsed = parse_envelope(bytes(frame), "ykc", "V1.5")
        self.assertFalse(parsed.crc["match"])
        self.assertIn("CRC 校验不匹配", parsed.warnings)


if __name__ == "__main__":
    unittest.main()
