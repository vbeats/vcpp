from __future__ import annotations

import json
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLI = ROOT / "scripts" / "cli.py"


class GeneratorTests(unittest.TestCase):
    def run_cli(self, *args):
        return subprocess.run([sys.executable, str(CLI), *args], capture_output=True, text=True, encoding="utf-8")

    def test_generate_and_reparse_trade(self):
        completed = self.run_cli("generate", "--request", "生成云快充1.7版本的交易记录", "--fields-json", json.dumps({"device_no": "55031412782305", "gun_no": 1}), "--format", "json")
        self.assertEqual(completed.returncode, 0, completed.stderr)
        result = json.loads(completed.stdout)
        self.assertTrue(result["generated"])
        self.assertEqual(result["version"], "V1.7")
        self.assertTrue(result["crc"]["match"])

    def test_parse_cli_uses_registered_decoder(self):
        sample = "6822000000012026050100000100020F3232303432320000010000000000000000000000563A"
        completed = self.run_cli("parse", "--protocol", "ykc", "--version", "V1.5", "--hex", sample, "--format", "json")
        self.assertEqual(completed.returncode, 0, completed.stderr)
        result = json.loads(completed.stdout)
        self.assertEqual(result["frame"]["name"], "登录请求")
        self.assertEqual(result["fields"][0]["value"], "20260501000001")

    def test_missing_version_is_structured_error(self):
        completed = self.run_cli("parse", "--protocol", "ykc", "--hex", "68", "--format", "json")
        self.assertEqual(completed.returncode, 2)
        result = json.loads(completed.stdout)
        self.assertEqual(result["error"], "version_required")


if __name__ == "__main__":
    unittest.main()
