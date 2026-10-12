"""Regression tests for false-green contract checks; no app or device operations."""
import tempfile
import unittest
from pathlib import Path
from check_contract_consistency import check


class ContractConsistencyTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="bazi-contract-check-test-")
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.write("AGENTS.md", "# Long-term rules\n")
        self.write("docs/DATA_CONTRACT.md", "# 数据\n当前版本：12\n## 交换、恢复\n")
        self.write("docs/TEST_STRATEGY.md", "# Tests\n")
        self.write("core/data/src/main/kotlin/com/nanzhufeng/nanfengbazi/data/db/NanfengBaziDatabase.kt", "const val SCHEMA_VERSION = 12\n")
        self.write("docs/CONTRACT_INDEX.md", "# Index\n| 合同/信息 | 正文 | 使用范围 | 类型 |\n| --- | --- | --- | --- |\n| Safety | [Rules](../AGENTS.md) | Safety | 现行 |\n| Data | [Data](DATA_CONTRACT.md) | Data | 现行 |\n| Tests | [Tests](TEST_STRATEGY.md) | Tests | 现行 |\n| History | [Old](history.md) | Evidence | 历史 |\n")
        self.write("docs/history.md", "```sh\n./gradlew connectedAndroidTest\n```\n")

    def write(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def issues(self):
        return {error["issue"] for error in check(self.root)["errors"]}

    def register(self, target, role="现行"):
        path = self.root / "docs/CONTRACT_INDEX.md"
        path.write_text(path.read_text() + f"| Added | [Contract]({target}) | Added | {role} |\n")

    def test_valid_baseline_and_inactive_history(self):
        self.assertEqual(check(self.root)["result"], "PASS")

    def test_schema_drift(self):
        self.write("docs/DATA_CONTRACT.md", "当前版本：13\n")
        self.assertIn("Schema version missing or differs from implementation", self.issues())

    def test_new_registry_target_is_mandatory(self):
        self.register("missing.md")
        self.assertIn("missing active contract", self.issues())
        self.write("docs/missing.md", "# New contract\n")
        self.assertEqual(check(self.root)["result"], "PASS")

    def test_unknown_role_cannot_silently_skip_a_contract(self):
        self.register("missing.md", "现形")
        self.assertIn("invalid contract registry row", self.issues())

    def test_unicode_anchor_is_checked(self):
        self.write("docs/TEST_STRATEGY.md", "[valid](DATA_CONTRACT.md#交换恢复)\n")
        self.assertEqual(check(self.root)["result"], "PASS")
        self.write("docs/TEST_STRATEGY.md", "[bad](DATA_CONTRACT.md#不存在)\n")
        self.assertIn("missing Markdown anchor", self.issues())

    def test_connected_command_forms(self):
        examples = [r".\gradlew.bat connectedDebugAndroidTest", "`gradlew connectedAndroidTest`", "`./gradlew :app:connectedDebugAndroidTest`", "```sh\n./gradlew \\\n  test \\\n  :app:connectedDebugAndroidTest\n```"]
        for command in examples:
            with self.subTest(command=command):
                self.write("docs/TEST_STRATEGY.md", command)
                self.assertIn("connected test invocation", self.issues())

    def test_skill_file_and_ui_metadata_are_required(self):
        self.register("../.agents/skills/example/SKILL.md")
        self.assertIn("missing active contract", self.issues())
        self.write(".agents/skills/example/SKILL.md", "# Example\n")
        self.assertIn("missing or incomplete Skill UI metadata", self.issues())
        self.write(".agents/skills/example/agents/openai.yaml", 'interface:\n  display_name: ""\n  short_description: " "\n  default_prompt: "Run"\n')
        self.assertIn("missing or incomplete Skill UI metadata", self.issues())
        self.write(".agents/skills/example/agents/openai.yaml", 'interface:\n  display_name: "Example"\n  short_description: "Test"\n  default_prompt: "Run"\n')
        self.assertEqual(check(self.root)["result"], "PASS")

    def test_local_links_cannot_escape_checkout(self):
        self.write("docs/TEST_STRATEGY.md", "[outside](../../outside.md)\n")
        self.assertIn("local link escapes project", self.issues())

    def test_primary_auto_launch_and_stale_network_claim(self):
        self.write("docs/TEST_STRATEGY.md", "OPPO默认允许自动打开\nManifest均无联网权限\n")
        self.assertIn("automatic primary-device launch", self.issues())
        self.assertIn("stale no-network Manifest claim", self.issues())


if __name__ == "__main__":
    unittest.main()
