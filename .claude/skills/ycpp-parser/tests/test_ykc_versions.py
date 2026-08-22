from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from scripts.frame import encode_frame, parse_frame
from scripts.protocols import decode_body, encode_body, frame_type_for
from scripts.registry import get_definition


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

    def test_v21_gps_uses_documented_offsets(self):
        result = self.parse("V2.1", 0x01, {"longitude": 120.123456, "latitude": 30.654321})
        values = {field["key"]: field for field in result.fields}
        self.assertEqual(values["longitude"]["value"], 120.123456)
        self.assertEqual(values["longitude"]["raw_hex"], (300123456).to_bytes(4, "little").hex().upper())
        self.assertEqual(values["latitude"]["value"], 30.654321)
        self.assertEqual(values["latitude"]["raw_hex"], (120654321).to_bytes(4, "little").hex().upper())

    def test_v21_missing_gps_is_encoded_as_unavailable(self):
        result = self.parse("V2.1", 0x01, {})
        values = {field["key"]: field for field in result.fields}
        self.assertIsNone(values["longitude"]["value"])
        self.assertEqual(values["longitude"]["raw_hex"], "00000000")
        self.assertIsNone(values["latitude"]["value"])
        self.assertEqual(values["latitude"]["raw_hex"], "00000000")

    def test_raw_body_override_still_gets_valid_envelope_crc(self):
        body = encode_body("ykc", "V1.7", 0x42, {"raw_body_hex": "AABBCC"})
        self.assertEqual(body, bytes.fromhex("AABBCC"))
        result = parse_frame(encode_frame("ykc", "V1.7", 0x42, body), "ykc", "V1.7", decode_body)
        self.assertTrue(result.crc["match"])
        self.assertTrue(result.warnings)

    def test_documented_catalog_is_completely_generatable(self):
        expected = {
            "V1.4": "01 02 03 04 05 06 09 0A 12 13 15 17 19 1B 1D 21 23 25 31 32 33 34 35 36 3F 40 41 42 43 44 45 46 47 48 51 52 55 56 57 58 61 62 63 91 92 93 94",
            "V1.5": "01 02 03 04 05 06 09 0A 12 13 15 17 19 1B 1D 21 23 25 31 32 33 34 35 36 3B 40 41 42 43 44 45 46 47 48 51 52 55 56 57 58 61 62 63 91 92 93 94 F0 F1",
            "V1.6": "01 02 03 04 05 06 09 0A 12 13 15 17 19 1B 1D 21 23 25 31 32 33 34 35 36 3B 40 41 42 43 44 45 46 47 48 51 52 55 56 57 58 61 62 63 91 92 93 94 9B 9C 9E A1 A2 A3 A4",
            "V1.7": "01 02 03 04 05 06 09 0A 12 13 15 17 19 1B 1D 21 23 25 31 32 33 34 35 36 3D 40 41 42 43 44 45 46 47 48 51 52 55 56 57 58 61 62 63 91 92 93 94 A1 A2 A3 A4",
            "V1.8.1": "01 02 03 04 05 06 09 0A 12 13 15 17 19 1B 1D 21 23 25 35 36 3D 40 41 42 43 44 45 46 47 48 51 52 55 56 57 58 59 60 61 62 63 91 92 93 94 A1 A2 A3 A4 A5 A6 A7 A8",
            "V2.1": "01 02 03 04 05 06 09 0A 12 13 15 17 19 1B 1D 21 23 25 35 36 3D 40 41 42 43 44 45 46 47 48 49 4A 4B 4C 4D 4E 4F 50 51 52 55 56 57 58 59 5A 5B 5C 5D 5E 5F 60 61 62 63 91 92 93 94 95 96 97 98 A1 A2 A3 A4 A5 A6 A7 A8 A9 AA",
        }
        for version, types in expected.items():
            definition = get_definition("ykc", version)
            expected_types = {int(item, 16) for item in types.split()}
            self.assertEqual(expected_types, set(definition.frame_names), version)
            self.assertEqual(expected_types, set(definition.decoders), version)
            self.assertEqual(expected_types, set(definition.encoders), version)
            for frame_type in expected_types:
                body = encode_body("ykc", version, frame_type, {})
                result = parse_frame(encode_frame("ykc", version, frame_type, body), "ykc", version, decode_body)
                self.assertTrue(result.crc["match"], f"{version}/0x{frame_type:02X}")
                self.assertEqual(result.frame_name, definition.frame_names[frame_type], f"{version}/0x{frame_type:02X}")
                self.assertEqual(result.direction, definition.directions[frame_type], f"{version}/0x{frame_type:02X}")
                self.assertEqual(result.unknown_body_hex, "", f"{version}/0x{frame_type:02X}")

    def test_variable_and_repeated_frames_round_trip(self):
        qr = self.parse("V2.1", 0x5B, {"device_no": "55031412782305", "qr_content": "https://example.test/charge"})
        qr_values = {field["key"]: field["value"] for field in qr.fields}
        self.assertEqual(qr_values["qr_length"], len("https://example.test/charge"))
        self.assertEqual(qr_values["qr_content"], "https://example.test/charge")
        cards = self.parse("V1.7", 0x44, {"cards": [{"logical_card": "1234", "physical_card": 7}]})
        card_values = {field["key"]: field["value"] for field in cards.fields}
        self.assertEqual(card_values["card_count"], 1)
        self.assertEqual(card_values["cards_1_physical_card"], 7)

    def test_v21_login_ack_and_billing_setting_are_variable(self):
        login_ack = self.parse("V2.1", 0x02, {"device_no": "55031412782305", "latest_key": "PUBLICKEY"})
        values = {field["key"]: field["value"] for field in login_ack.fields}
        self.assertEqual(values["key_length"], 9)
        self.assertEqual(values["latest_key"], "PUBLICKEY")
        billing = self.parse("V2.1", 0x58, {"rate_count": 2, "electric_rate_2": 1.2})
        billing_values = {field["key"]: field["value"] for field in billing.fields}
        self.assertEqual(billing_values["rate_count"], 2)
        self.assertEqual(billing_values["electric_rate_2"], 1.2)

    def test_natural_language_routes_new_frame_types(self):
        self.assertEqual(frame_type_for("ykc", "V2.1", "生成云快充2.1设备故障复位上送"), 0x4B)
        self.assertEqual(frame_type_for("ykc", "V1.5", "生成云快充1.5二维码前缀下发"), 0xF0)

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
