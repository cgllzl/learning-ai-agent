"""Behavior tests for the deterministic after-sales preflight."""

import json
import subprocess
import sys
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("validate_after_sales_request.py")
BASE = {"userId": "user-1", "tenantId": "tenant-a", "orderId": "O1003"}


def run_case(payload):
    return subprocess.run(
        [sys.executable, str(SCRIPT)],
        input=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        capture_output=True,
        check=False,
    )


class AfterSalesPreflightTest(unittest.TestCase):
    def test_query_needs_identity_and_then_requests_runtime_authorization(self):
        result = run_case({**BASE, "action": "QUERY"})
        self.assertEqual(result.returncode, 0)
        output = json.loads(result.stdout)
        self.assertTrue(output["valid"])
        self.assertEqual(output["requiredScope"], "orders:read")
        self.assertEqual(output["nextStep"], "RUNTIME_AUTHORIZATION")

    def test_cancel_without_approval_or_idempotency_stops(self):
        result = run_case({**BASE, "action": "CANCEL", "scope": "orders:write"})
        output = json.loads(result.stdout)
        self.assertFalse(output["valid"])
        self.assertEqual(output["missingFields"], ["approvalId", "idempotencyKey"])
        self.assertEqual(output["nextStep"], "REQUEST_APPROVAL")

    def test_refund_requires_positive_amount_currency_and_human_review(self):
        incomplete = run_case({
            **BASE, "action": "REFUND", "amount": "-10", "currency": "CNY",
            "approvalId": "APR-1", "idempotencyKey": "refund-1",
        })
        self.assertEqual(json.loads(incomplete.stdout)["invalidFields"], ["amount"])
        complete = run_case({
            **BASE, "action": "REFUND", "amount": "10.00", "currency": "CNY",
            "approvalId": "APR-1", "idempotencyKey": "refund-1",
        })
        self.assertEqual(json.loads(complete.stdout)["nextStep"], "HUMAN_REVIEW")

    def test_unsupported_action_cannot_turn_into_tool_call(self):
        output = json.loads(run_case({**BASE, "action": "EXPORT_ALL"}).stdout)
        self.assertFalse(output["valid"])
        self.assertEqual(output["nextStep"], "REJECT_UNSUPPORTED_ACTION")

    def test_malformed_json_is_a_program_error(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT)], input=b"{", capture_output=True, check=False
        )
        self.assertEqual(result.returncode, 2)
        self.assertEqual(result.stdout, b"")


if __name__ == "__main__":
    unittest.main()
