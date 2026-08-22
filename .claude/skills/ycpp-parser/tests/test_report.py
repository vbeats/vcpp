from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from scripts.frame import encode_frame
from scripts.protocols import decode_body
from scripts.report import to_report
from scripts.frame import parse_frame


class ReportTests(unittest.TestCase):
    def test_emoji_only_changes_decoration(self):
        frame = encode_frame("ykc", "V1.5", 0x01, bytes(30))
        result = parse_frame(frame, "ykc", "V1.5", decode_body)
        plain = to_report(result.as_dict())
        decorated = to_report(result.as_dict(), emoji=True)
        self.assertIn("【云快充 V1.5】", plain)
        self.assertIn("⬆️", decorated)
        self.assertEqual(plain.replace("【云快充 V1.5】", ""), decorated.replace("【云快充 V1.5】 ⬆️", ""))


if __name__ == "__main__":
    unittest.main()
