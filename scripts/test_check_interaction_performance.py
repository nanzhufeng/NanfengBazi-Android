import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET
from check_interaction_performance import EmulatorUi


class PerformanceTargetSafetyTest(unittest.TestCase):
    def test_primary_phone_is_rejected_before_any_adb_call(self):
        with patch.object(EmulatorUi, "adb") as adb:
            with self.assertRaises(ValueError):
                EmulatorUi("3B157F009E800000")
            adb.assert_not_called()

    def test_emulator_name_without_qemu_identity_is_rejected(self):
        with patch.object(EmulatorUi, "adb", return_value="0") as adb:
            with self.assertRaises(ValueError):
                EmulatorUi("emulator-5600")
            self.assertEqual(1, adb.call_count)

    def test_failed_dump_never_reads_stale_ui_for_a_click(self):
        with patch.object(EmulatorUi, "adb", side_effect=["1", "ERROR: null root"]) as adb:
            ui = EmulatorUi("emulator-5600")
            with self.assertRaises(RuntimeError):
                ui.snapshot()
            self.assertEqual(2, adb.call_count)


    def test_no_rendered_frames_have_no_latency_percentiles(self):
        raw = "Total frames rendered: 0\n50th percentile: 4950ms\n90th percentile: 4950ms\n95th percentile: 4950ms"
        with patch.object(EmulatorUi, "adb", side_effect=["1", "", raw]), \
             patch.object(EmulatorUi, "snapshot", return_value=ET.fromstring("<hierarchy/>")):
            result = EmulatorUi("emulator-5600").sample("idle", lambda: None)
        self.assertEqual(0, result["frames"])
        self.assertIsNone(result["median_ms"])
        self.assertIsNone(result["p90_ms"])
        self.assertIsNone(result["p95_ms"])


if __name__ == "__main__":
    unittest.main()
