"""Black-box acceptance of the separately installed E2E app; stdlib only."""
import re
import argparse
import json
import os
import subprocess
import sqlite3
import time
from datetime import datetime
from pathlib import Path
import xml.etree.ElementTree as ET

E2E_PACKAGE = 'com.amkumirab.solostudying.e2e'
MAIN_ACTIVITY = 'com.amkumirab.solostudying.MainActivity'


def validate_target(api, serial, avd, actual_api):
    expected = {36: 'emulator-5580', 37: 'emulator-5582'}
    if (api not in expected or serial != expected.get(api) or avd != f'SoloStudying_QA_API{api}'
            or actual_api != str(api)):
        raise ValueError('Only the matching dedicated API 36/37 QA emulator is allowed')


def validate_apk(badging):
    match = re.search(r"^package: name='([^']+)'", badging, re.MULTILINE)
    if not match or match[1] != E2E_PACKAGE:
        raise ValueError('Only the isolated E2E APK is allowed')


def unique_node(xml, label):
    root = ET.fromstring(xml)
    nodes = list(root.iter('node'))
    matches = [n for n in nodes if n.get('content-desc') == label]
    if not matches:
        matches = [n for n in nodes if n.get('text') == label]
    if len(matches) != 1 or matches[0].get('enabled') != 'true':
        raise ValueError(f'Expected one enabled control: {label!r}; found {len(matches)}')
    parents = {child: parent for parent in root.iter() for child in parent}
    parent = parents.get(matches[0])
    while parent is not None:
        if parent.get('enabled') == 'false':
            raise ValueError(f'Control is inside a disabled container: {label!r}')
        parent = parents.get(parent)
    return matches[0]


def tap_point(bounds, width, height):
    match = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
    if not match:
        raise ValueError(f'Invalid bounds: {bounds}')
    left, top, right, bottom = map(int, match.groups())
    if not (0 <= left < right <= width and 0 <= top < bottom <= height):
        raise ValueError(f'Control is not fully visible: {bounds}')
    return ((left + right) // 2, (top + bottom) // 2)


def verify_completion(state):
    goals = [b for b in state['bosses'] if b['name'] == 'E2E Physics']
    if len(goals) != 1 or len(state['study_sessions']) != 1 or len(state['user_profile']) != 1:
        raise AssertionError('Expected one goal, one session and one profile')
    goal, session, profile = goals[0], state['study_sessions'][0], state['user_profile'][0]
    expected = [(goal, {'timeSpentSeconds': 120, 'isCompleted': 1}),
                (session, {'durationSeconds': 120, 'bossId': goal['id'], 'wasCompleted': 1,
                           'note': 'E2E persisted note', 'xpEarned': 150, 'goldEarned': 130}),
                (profile, {'totalStudyTimeSeconds': 120, 'totalSessionCount': 1,
                           'totalXpEarned': 150, 'gold': 230, 'level': 2})]
    for actual, fields in expected:
        for field, value in fields.items():
            if actual[field] != value:
                raise AssertionError(f'Persisted {field}: expected {value!r}, got {actual[field]!r}')


class Device:
    def __init__(self, sdk, api, report):
        self.sdk, self.api = Path(sdk), api
        self.serial = {36: 'emulator-5580', 37: 'emulator-5582'}[api]
        self.adb = self.sdk / 'platform-tools/adb.exe'
        avd = self.command('emu', 'avd', 'name').splitlines()[0].strip()
        actual = self.command('shell', 'getprop', 'ro.build.version.sdk').strip()
        validate_target(api, self.serial, avd, actual)
        if self.command('shell', 'getprop', 'sys.boot_completed').strip() != '1':
            raise ValueError('Wait for the dedicated emulator to finish booting')
        size = self.command('shell', 'wm', 'size')
        self.width, self.height = map(int, re.findall(r'(\d+)x(\d+)', size)[-1])
        self.report = Path(report)
        self.report.mkdir(parents=True, exist_ok=False)
        self.dump_count = 0

    def command(self, *args, timeout=35):
        result = subprocess.run([str(self.adb), '-s', self.serial, *args],
                                capture_output=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError(result.stderr.decode('utf-8', errors='replace'))
        return result.stdout.decode('utf-8', errors='replace')

    def install(self, apk, reset=False):
        aapt = self.sdk / 'build-tools/37.0.0/aapt2.exe'
        result = subprocess.run([str(aapt), 'dump', 'badging', str(apk)],
                                capture_output=True, check=True, timeout=30)
        validate_apk(result.stdout.decode('utf-8'))
        if 'Success' not in self.command('install', '-r', str(apk), timeout=60):
            raise RuntimeError('E2E APK installation did not succeed')
        if reset and 'Success' not in self.command('shell', 'pm', 'clear', E2E_PACKAGE):
            raise RuntimeError('Disposable E2E storage reset failed')

    def launch(self, cold=False):
        if cold:
            self.command('shell', 'am', 'force-stop', E2E_PACKAGE)
        self.command('shell', 'am', 'start', '-W', '-n', f'{E2E_PACKAGE}/{MAIN_ACTIVITY}')

    def tree(self):
        for _ in range(3):
            self.dump_count += 1
            remote = f'/sdcard/e2e-ui-{self.dump_count}.xml'
            result = self.command('shell', 'uiautomator', 'dump', remote)
            if 'dumped to' not in result:
                continue
            local = self.report / 'latest.xml'
            self.command('pull', remote, str(local))
            xml = local.read_text(encoding='utf-8')
            ET.fromstring(xml)
            return xml
        raise RuntimeError('No fresh accessibility hierarchy; stale dumps are not accepted')

    def tap(self, node):
        if node.get('package') not in (E2E_PACKAGE, 'com.android.permissioncontroller'):
            raise ValueError('Refusing to click outside the E2E app or permission dialog')
        x, y = tap_point(node.get('bounds', ''), self.width, self.height)
        self.command('shell', 'input', 'tap', str(x), str(y))

    def find(self, label, scroll=False, timeout=25, down=True):
        deadline, swipes = time.monotonic() + timeout, 0
        while time.monotonic() < deadline:
            xml = self.tree()
            try:
                node = unique_node(xml, label)
                tap_point(node.get('bounds', ''), self.width, self.height)
                return node
            except ValueError:
                if scroll and swipes < 8:
                    self.scroll(xml, down)
                    swipes += 1
                else:
                    time.sleep(0.3)
        raise AssertionError(f'Control not uniquely available: {label!r}')

    def click(self, label, scroll=False, down=True):
        self.tap(self.find(label, scroll, down=down))

    def scroll(self, xml, down=True):
        candidates = [n for n in ET.fromstring(xml).iter('node')
                      if n.get('scrollable') == 'true' and n.get('package') == E2E_PACKAGE]
        if not candidates:
            return
        node = max(candidates, key=lambda n: int(re.findall(r'\d+', n.get('bounds'))[3])
                   - int(re.findall(r'\d+', n.get('bounds'))[1]))
        x, y = tap_point(node.get('bounds'), self.width, self.height)
        _, top, _, bottom = map(int, re.findall(r'\d+', node.get('bounds')))
        start, end = bottom - 30, top + 30
        if not down:
            start, end = end, start
        self.command('shell', 'input', 'swipe', str(x), str(start), str(x), str(end), '350')

    def field(self, label, value):
        if not re.fullmatch(r'[A-Za-z0-9 ._-]+', value):
            raise ValueError('Fixture input must be plain ASCII text')
        self.click(label, scroll=True)
        self.command('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END')
        self.command('shell', 'input', 'keyevent', *(['KEYCODE_DEL'] * 50))
        self.command('shell', 'input', 'text', value.replace(' ', '%s'))
        self.command('shell', 'input', 'keyevent', 'KEYCODE_BACK')

    def evidence(self, name):
        xml = self.tree()
        (self.report / f'{name}.xml').write_text(xml, encoding='utf-8')
        remote = '/sdcard/e2e-evidence.png'
        self.command('shell', 'screencap', '-p', remote)
        self.command('pull', remote, str(self.report / f'{name}.png'))
        return xml

    def deny_notification_prompt(self):
        nodes = list(ET.fromstring(self.tree()).iter('node'))
        if any(n.get('text') == 'Allow Solo Studying E2E to send you notifications?' for n in nodes):
            deny_ids = {'com.android.permissioncontroller:id/permission_deny_button',
                        'com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button'}
            deny = [n for n in nodes if n.get('resource-id') in deny_ids]
            if len(deny) != 1:
                raise AssertionError('Unknown notification permission dialog')
            self.tap(deny[0])

    def labels(self):
        return [n.get('text') or n.get('content-desc') for n in ET.fromstring(self.tree()).iter('node')
                if n.get('text') or n.get('content-desc')]

    def snapshot(self, name):
        """Read a cold copy of real E2E storage; never seed or change app records."""
        self.command('shell', 'am', 'force-stop', E2E_PACKAGE)
        target = self.report / name
        target.mkdir()
        files = self.command('shell', 'run-as', E2E_PACKAGE, 'ls', 'databases').split()
        base = 'solo_studying_database'
        for filename in [base, base + '-wal', base + '-shm']:
            if filename not in files:
                continue
            data = subprocess.run([str(self.adb), '-s', self.serial, 'exec-out', 'run-as',
                                   E2E_PACKAGE, 'cat', 'databases/' + filename],
                                  capture_output=True, check=True, timeout=30).stdout
            (target / filename).write_bytes(data)
        with sqlite3.connect((target / base).as_uri() + '?mode=ro', uri=True) as db:
            db.row_factory = sqlite3.Row
            if db.execute('PRAGMA quick_check').fetchone()[0] != 'ok':
                raise AssertionError('E2E database integrity check failed')
            state = {table: [dict(row) for row in db.execute(f'SELECT * FROM {table} ORDER BY id')]
                     for table in ['bosses', 'study_sessions', 'user_profile']}
        (target / 'state.json').write_text(json.dumps(state, indent=2), encoding='utf-8')
        return state


def run_journey(device, apk, reset):
    result = {'api': device.api, 'package': E2E_PACKAGE, 'status': 'running', 'steps': []}

    def step(name, action):
        print(f'RUN {name}', flush=True)
        result['current_step'] = name
        action()
        device.evidence(name)
        result['steps'].append(name)
        (device.report / 'result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        print(f'PASS {name}', flush=True)

    try:
        device.install(apk, reset)
        device.launch()
        device.deny_notification_prompt()

        def onboarding():
            device.click('SKIP')
            device.click('Bypass Ceremony')
            device.find('Your day')
        step('01-onboarding', onboarding)

        def goal():
            device.click('Study')
            device.click('New goal')
            device.field('Enemy Name (e.g., Mathematics Exam)', 'E2E Physics')
            device.field('Required Study Duration (Minutes)', '2')
            device.click('SUMMON', scroll=True)
            device.find('E2E PHYSICS', scroll=True)
        step('02-goal-created', goal)

        def planner():
            device.click('Plan study hub section', scroll=True, down=False)
            device.click('Create weekly plan', scroll=True)
            device.click('E2E Physics')
            device.click('Continue', scroll=True)
            device.find('How much focused study does this course still need?')
            device.click('Continue', scroll=True)
            device.click('Continue', scroll=True)
            device.click('Preview plan', scroll=True)
            device.find('Your preparation fits the available study time.', scroll=True)
            device.click('Save plan', scroll=True)
            device.find('Weekly report', scroll=True)
        step('03-weekly-plan-saved', planner)

        def timer_recovery():
            device.click('Study course', scroll=True)
            device.click('PAUSE TIMER', scroll=True)
            device.find('RESUME TIMER')
            before = [s for s in device.labels() if re.fullmatch(r'\d{2}:\d{2}', s)]
            if len(before) != 1 or before == ['00:00']:
                raise AssertionError(f'Expected one unfinished countdown, found {before}')
            device.evidence('04-before-cold-recovery')
            device.launch(cold=True)
            device.deny_notification_prompt()
            device.find('RESUME TIMER', scroll=True)
            after = [s for s in device.labels() if re.fullmatch(r'\d{2}:\d{2}', s)]
            if before != after:
                raise AssertionError(f'Paused countdown changed on process restart: {before} -> {after}')
            device.click('RESUME TIMER')
            device.command('shell', 'input', 'keyevent', 'KEYCODE_HOME')
            time.sleep(5)
            device.launch()
            device.find('PAUSE TIMER', scroll=True)
        step('04-timer-recovered', timer_recovery)

        def completion():
            deadline = time.monotonic() + 150
            while time.monotonic() < deadline:
                xml = device.tree()
                labels = [n.get('text') for n in ET.fromstring(xml).iter('node')]
                for notice in ['ASCEND FORWARD', 'CONTINUE QUEST']:
                    if notice in labels:
                        device.evidence('05-notice-' + notice.split()[0].lower())
                        device.tap(unique_node(xml, notice))
                        break
                else:
                    if 'SESSION COMPLETE' in labels:
                        break
                time.sleep(0.5)
            device.find('SESSION COMPLETE')
            device.find('2m 0s')
            device.evidence('05-natural-completion-summary')
            device.field('SESSION NOTE (OPTIONAL)', 'E2E persisted note')
            device.click('DONE', scroll=True)
            device.click('Today')
            device.find('2 of 45 minutes · 43 minutes remaining')
        step('05-natural-completion', completion)

        first = device.snapshot('first-completion')
        verify_completion(first)

        def persisted_report():
            device.launch()
            device.find('2 of 45 minutes · 43 minutes remaining')
            device.click('Study')
            device.click('Plan study hub section')
            device.click('Open weekly plan', scroll=True)
            device.click('Weekly report', scroll=True)
            device.find('Planned focus: 2 min', scroll=True)
            device.find('Credit assigned to these sessions: 2 min', scroll=True)
        step('06-persisted-report', persisted_report)
        second = device.snapshot('after-cold-relaunch')
        if first != second:
            raise AssertionError('Cold relaunch duplicated or changed persisted progress, history or rewards')
        result['status'] = 'passed'
        result['verified_seconds'] = 120
        result['verified_sessions'] = 1
        print('PASS cold relaunch preserved exact records without duplicate credit', flush=True)
    except Exception as error:
        result['status'], result['error'] = 'failed', str(error)
        try:
            device.evidence('failure')
        except Exception as evidence_error:
            result['evidence_error'] = str(evidence_error)
        raise
    finally:
        (device.report / 'result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api', type=int, choices=[36, 37], default=36)
    parser.add_argument('--sdk', default=str(Path(os.environ.get('LOCALAPPDATA', '')) / 'Android/Sdk'))
    parser.add_argument('--reset-e2e-data', action='store_true', help='Clear only the disposable E2E copy')
    args = parser.parse_args()
    repo = Path(__file__).resolve().parent.parent
    report = repo / 'app/build/reports/e2e' / f"api{args.api}-{datetime.now():%Y%m%d-%H%M%S}"
    print(f'Evidence: {report}', flush=True)
    device = Device(args.sdk, args.api, report)
    run_journey(device, repo / 'app/build/outputs/apk/e2e/app-e2e.apk', args.reset_e2e_data)


if __name__ == '__main__':
    main()
