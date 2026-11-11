import unittest
import copy
from android_e2e import Device, E2E_PACKAGE, validate_target, unique_node, tap_point, validate_apk
from android_e2e import verify_completion


class RunnerSafetyTest(unittest.TestCase):
    def test_both_first_and_repeated_notification_denials_are_handled(self):
        for button in ['permission_deny_button', 'permission_deny_and_dont_ask_again_button']:
            xml = ('<hierarchy><node text="Allow Solo Studying E2E to send you notifications?"/>'
                   f'<node resource-id="com.android.permissioncontroller:id/{button}"/></hierarchy>')
            device, tapped = object.__new__(Device), []
            device.tree = lambda: xml
            device.tap = lambda node: tapped.append(node.get('resource-id'))
            with self.subTest(button=button):
                device.deny_notification_prompt()
                self.assertEqual(['com.android.permissioncontroller:id/' + button], tapped)

    def test_only_matching_dedicated_emulator_is_allowed(self):
        validate_target(36, 'emulator-5580', 'SoloStudying_QA_API36', '36')
        validate_target(37, 'emulator-5582', 'SoloStudying_QA_API37', '37')
        for args in [(36, 'phone', 'SoloStudying_QA_API36', '36'),
                     (36, 'emulator-5580', 'PersonalPhone', '36'),
                     (35, None, 'SoloStudying_QA_API35', '35'),
                     (37, 'emulator-5582', 'SoloStudying_QA_API37', '36')]:
            with self.subTest(args=args), self.assertRaises(ValueError):
                validate_target(*args)

    def test_disabled_parent_is_not_bypassed_by_a_child_label(self):
        xml = '<hierarchy><node enabled="false"><node text="Save" enabled="true"/></node></hierarchy>'
        with self.assertRaises(ValueError):
            unique_node(xml, 'Save')

    def test_normal_application_cannot_be_reset_as_an_e2e_fixture(self):
        validate_apk("package: name='%s' versionCode='1'" % E2E_PACKAGE)
        for name in ['com.amkumirab.solostudying', E2E_PACKAGE + '.other']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                validate_apk("package: name='%s' versionCode='1'" % name)

    def test_description_is_preferred_to_duplicate_child_text(self):
        xml = '<hierarchy><node content-desc="Study" bounds="[0,0][40,40]" enabled="true"><node text="Study" bounds="[0,0][40,40]" enabled="true"/></node></hierarchy>'
        self.assertEqual('Study', unique_node(xml, 'Study').get('content-desc'))

    def test_missing_ambiguous_and_disabled_controls_are_not_clicked(self):
        for nodes in ['', '<node text="Save"/><node text="Save"/>',
                      '<node text="Save" enabled="false"/>']:
            with self.subTest(nodes=nodes), self.assertRaises(ValueError):
                unique_node('<hierarchy>' + nodes + '</hierarchy>', 'Save')

    def test_only_visible_valid_bounds_produce_a_tap(self):
        self.assertEqual((20, 30), tap_point('[10,20][30,40]', 100, 200))
        for bounds in ['[0,0][0,0]', '[-1,2][30,40]', '[40,40][10,20]',
                       '[0,201][10,220]', 'not bounds']:
            with self.subTest(bounds=bounds), self.assertRaises(ValueError):
                tap_point(bounds, 100, 200)


class CompletionOracleTest(unittest.TestCase):
    def state(self):
        return {'bosses': [{'id': 7, 'name': 'E2E Physics', 'timeSpentSeconds': 120, 'isCompleted': 1}],
                'study_sessions': [{'bossId': 7, 'durationSeconds': 120, 'wasCompleted': 1,
                                    'note': 'E2E persisted note', 'xpEarned': 150, 'goldEarned': 130}],
                'user_profile': [{'totalStudyTimeSeconds': 120, 'totalSessionCount': 1,
                                  'totalXpEarned': 150, 'gold': 230, 'level': 2}]}

    def test_exact_two_minutes_and_level_up_rewards_are_accepted(self):
        verify_completion(self.state())

    def test_duplicate_or_missing_records_are_rejected(self):
        for table in ['bosses', 'study_sessions', 'user_profile']:
            for count in [0, 2]:
                state = self.state()
                state[table] *= count
                with self.subTest(table=table, count=count), self.assertRaises(AssertionError):
                    verify_completion(state)

    def test_wrong_progress_linkage_notes_and_rewards_are_rejected(self):
        wrong = [('bosses', 'timeSpentSeconds', 240), ('bosses', 'isCompleted', 0),
                 ('study_sessions', 'bossId', 99), ('study_sessions', 'durationSeconds', 59),
                 ('study_sessions', 'wasCompleted', 0), ('study_sessions', 'note', None),
                 ('study_sessions', 'xpEarned', 2), ('study_sessions', 'goldEarned', 1),
                 ('user_profile', 'totalStudyTimeSeconds', 240),
                 ('user_profile', 'totalSessionCount', 2), ('user_profile', 'totalXpEarned', 2),
                 ('user_profile', 'gold', 101)]
        for table, field, value in wrong:
            state = copy.deepcopy(self.state())
            state[table][0][field] = value
            with self.subTest(table=table, field=field), self.assertRaises(AssertionError):
                verify_completion(state)


if __name__ == '__main__':
    unittest.main()
