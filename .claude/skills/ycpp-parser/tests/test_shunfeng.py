from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from scripts.frame import encode_frame, parse_frame
from scripts.protocols import decode_body, encode_body


class ShunfengTests(unittest.TestCase):
    def test_0b_sample(self):
        sample = "680B0200000B55031412782305DD25"
        result = parse_frame(bytes.fromhex(sample), "shunfeng", "V1.5", decode_body)
        self.assertEqual(result.frame_type, 0x0B)
        self.assertEqual(result.fields[0]["value"], "55031412782305")
        self.assertFalse(result.crc["match"])
        self.assertIn("CRC 校验不匹配", result.warnings)
    def test_0c_has_eight_rates_and_48_periods(self):
        body = encode_body("shunfeng", "V1.5", 0x0C, {"device_no": "55031412782305", "billing_model": "0100"})
        result = parse_frame(encode_frame("shunfeng", "V1.5", 0x0C, body), "shunfeng", "V1.5", decode_body)
        keys = {field["key"] for field in result.fields}
        self.assertIn("electric_rate_8", keys)
        self.assertIn("period_rate_48", keys)
        self.assertEqual(result.unknown_body_hex, "")
        self.assertTrue(result.crc["match"])


if __name__ == "__main__":
    unittest.main()
