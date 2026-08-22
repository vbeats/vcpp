from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from scripts.frame import encode_frame, parse_frame
from scripts.protocols import decode_body, encode_body


class YkcVersionTests(unittest.TestCase):
    def parse(self, version: str, frame_type: int, fields: dict):
        body = encode_body("ykc", version, frame_type, fields)
        return parse_frame(encode_frame("ykc", version, frame_type, body, sequence=3), "ykc", version, decode_body)

    def test_login_sample_fields(self):
        sample = "6822000000012026050100000100020F3232303432320000010000000000000000000000563A"
        result = parse_frame(bytes.fromhex(sample), "ykc", "V1.5", decode_body)
        values = {field["key"]: field["value"] for field in result.fields}
        self.assertEqual(values["device_no"], "20260501000001")
        self.assertEqual(values["gun_count"], 2)
        self.assertTrue(result.crc["match"])

    def test_transaction_command_and_meter_widths(self):
        for version, frame_type, width in (("V1.4", 0x3F, 4), ("V1.5", 0x3B, 5), ("V1.6", 0x3B, 5), ("V1.7", 0x3D, 5), ("V1.8.1", 0x3D, 5)):
            result = self.parse(version, frame_type, {"device_no": "55031412782305", "meter_start": 12.3456, "meter_end": 20.0})
            values = {field["key"]: field for field in result.fields}
            self.assertEqual(result.frame_type, frame_type)
            self.assertEqual(values["meter_start"]["length"], width)
            self.assertTrue(result.crc["match"])

    def test_v21_login_and_transaction(self):
        login = self.parse("V2.1", 0x01, {"device_no": "55031412782305", "phone": "13800138000"})
        login_values = {field["key"]: field["value"] for field in login.fields}
        self.assertEqual(login_values["phone"], "13800138000")
        self.assertEqual(login.spec.kind, "v2.1")
        trade = self.parse("V2.1", 0x3D, {"device_no": "55031412782305", "rate_count": 1, "rate_1_单价": 1.2})
        trade_values = {field["key"]: field["value"] for field in trade.fields}
        self.assertEqual(trade_values["rate_count"], 1)
        self.assertTrue(trade.crc["match"])

    def test_billing_models_legacy_and_v21(self):
        legacy_body = encode_body("ykc", "V1.5", 0x0A, {"device_no": "55031412782305", "jian_electric_rate": 0.8})
        legacy = parse_frame(encode_frame("ykc", "V1.5", 0x0A, legacy_body), "ykc", "V1.5", decode_body)
        legacy_values = {field["key"]: field["value"] for field in legacy.fields}
        self.assertEqual(legacy_values["jian_electric_rate"], 0.8)
        self.assertIn("period_rate_48", legacy_values)

        v21_body = encode_body("ykc", "V2.1", 0x0A, {"device_no": "55031412782305", "rate_count": 2, "electric_rate_2": 1.2})
        v21 = parse_frame(encode_frame("ykc", "V2.1", 0x0A, v21_body), "ykc", "V2.1", decode_body)
        v21_values = {field["key"]: field["value"] for field in v21.fields}
        self.assertEqual(v21_values["rate_count"], 2)
        self.assertEqual(v21_values["electric_rate_2"], 1.2)
        self.assertEqual(v21.unknown_body_hex, "")

    def test_encrypted_body_is_not_decoded(self):
        body = bytes.fromhex("AABBCCDD")
        result = parse_frame(encode_frame("ykc", "V1.7", 0x3D, body, encrypt_flag=1), "ykc", "V1.7", decode_body)
        self.assertEqual(result.fields, [])
        self.assertEqual(result.unknown_body_hex, "AABBCCDD")
        self.assertIn("消息体标记为密文", result.warnings[0])

    def test_unknown_type_preserves_body(self):
        body = bytes.fromhex("AABBCC")
        result = parse_frame(encode_frame("ykc", "V1.7", 0xEE, body), "ykc", "V1.7", decode_body)
        self.assertEqual(result.unknown_body_hex, "AABBCC")
        self.assertIn("未注册帧类型", result.warnings[0])


if __name__ == "__main__":
    unittest.main()
