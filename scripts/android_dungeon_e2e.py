"""Verify goal editing, the Second Gate, and recovery in the disposable APK."""
import argparse
from datetime import datetime
import os
from pathlib import Path
import time

from android_e2e import Device


def verify_goal_journey(device):
    device.deny_notification_prompt()
    device.click("SKIP")
    device.click("Bypass Ceremony")
    device.find("Your day")
    device.click("Study")
    device.click("New goal")
    device.field("Enemy Name (e.g., Mathematics Exam)", "Systems QA")
    device.field("Required Study Duration (Minutes)", "1")
    device.click("SUMMON", scroll=True)
    device.click("BEGIN DEFY QUEST", scroll=True)
    device.click("BEGIN BATTLE", scroll=True)
    print("Waiting for the one-minute goal to finish normally", flush=True)
    deadline = time.monotonic() + 100
    while time.monotonic() < deadline:
        labels = device.labels()
        if "DONE" in labels or "SESSION NOTE (OPTIONAL)" in labels:
            break
        for notice in ("ASCEND FORWARD", "CONTINUE QUEST"):
            if notice in labels:
                device.click(notice)
                break
    else:
        raise AssertionError("The focus session never displayed its summary")
    device.click("DONE", scroll=True)
    device.click("Study")
    device.click("Open Second Gate", scroll=True)
    xml = device.tree()
    if 'text="Additional minutes"' not in xml:
        raise AssertionError("The Second Gate did not ask for additional minutes")
    device.field("Additional minutes", "2")
    device.click("Open gate")
    device.find("Second Gate", scroll=True)
    device.evidence("nested")
    device.click("Edit Systems QA · Second Gate", scroll=True)
    device.field("Target minutes", "3")
    device.click("Save")
    device.find("REQUIRED: 3 Mins Focus", scroll=True)
    device.click("Move Systems QA to Deleted goals", scroll=True, down=False)
    device.click("Move to Deleted goals")
    device.click("Deleted goals (1)", scroll=True, down=False)
    device.find("1m 0s studied · 1 min target")
    device.evidence("history")
    device.click("Restore")
    device.find("No deleted goals. Goals you remove from Study will appear here.")
    device.click("Done")
    device.find("SYSTEMS QA", scroll=True)
    device.find("REQUIRED: 3 Mins Focus", scroll=True)
    device.evidence("restored")

    state = device.snapshot("persisted")
    original, = [goal for goal in state["bosses"] if goal["name"] == "Systems QA"]
    gate, = [goal for goal in state["bosses"] if goal["secondGateOfId"] == original["id"]]
    if (original["requiredMinutes"], original["timeSpentSeconds"], original["isCompleted"],
            original["archivedAtMillis"]) != (1, 60, 1, None):
        raise AssertionError("Restoring the original changed its target, time, or completion")
    if (gate["requiredMinutes"], gate["timeSpentSeconds"], gate["archivedAtMillis"]) != (3, 0, None):
        raise AssertionError("The edited gate did not survive archive and restore")
    session, = state["study_sessions"]
    if (session["bossId"], session["durationSeconds"]) != (original["id"], 60):
        raise AssertionError("The original session was lost or credited to the wrong goal")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api", type=int, default=36, choices=[36, 37])
    parser.add_argument("--sdk", default=str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android/Sdk"))
    parser.add_argument("--reset-e2e-data", action="store_true", required=True,
                        help="Clear only the verified disposable E2E package before the scenario")
    args = parser.parse_args()
    repo = Path(__file__).resolve().parent.parent
    report = repo / "app/build/reports/dungeon-e2e" / f"api{args.api}-{datetime.now():%Y%m%d-%H%M%S}"
    device = Device(args.sdk, args.api, report)
    device.install(repo / "app/build/outputs/apk/e2e/app-e2e.apk", reset=args.reset_e2e_data)
    device.launch()
    print(f"Evidence: {report}", flush=True)
    verify_goal_journey(device)
    print("PASS: linked goal, edit, archive, restore, and exact persisted study time", flush=True)


if __name__ == "__main__":
    main()
