"""Deterministic shape check for an after-sales request. No network or business writes."""

import json
import sys
from decimal import Decimal, InvalidOperation

MAX_INPUT_BYTES = 65536
ACTION_RULES = {
    "QUERY": ("READ_ONLY", "orders:read", ()),
    "CANCEL": ("MUTATING", "orders:write", ("approvalId", "idempotencyKey")),
    "CHANGE_ADDRESS": (
        "MUTATING",
        "orders:write",
        ("newAddress", "approvalId", "idempotencyKey"),
    ),
    "REFUND": (
        "FINANCIAL",
        "orders:refund",
        ("amount", "currency", "approvalId", "idempotencyKey"),
    ),
}
IDENTITY_FIELDS = ("userId", "tenantId", "orderId")


def present(value):
    """Only non-empty strings count as supplied fields."""
    return isinstance(value, str) and bool(value.strip())


def validate(request):
    """Check shape only; field values do not prove identity, scope, or approval."""
    if not isinstance(request, dict):
        raise ValueError("input must be a JSON object")

    action = request.get("action")
    if not isinstance(action, str) or action not in ACTION_RULES:
        return {
            "valid": False,
            "riskLevel": "UNKNOWN",
            "requiredScope": None,
            "missingFields": [],
            "invalidFields": ["action"],
            "nextStep": "REJECT_UNSUPPORTED_ACTION",
        }

    risk, required_scope, action_fields = ACTION_RULES[action]
    missing = [field for field in IDENTITY_FIELDS + action_fields
               if not present(request.get(field))]
    invalid = []
    if action == "REFUND" and "amount" not in missing:
        try:
            amount = Decimal(request["amount"])
            if not amount.is_finite() or amount <= 0:
                invalid.append("amount")
        except InvalidOperation:
            invalid.append("amount")
    if action == "REFUND" and "currency" not in missing:
        currency = request["currency"].strip()
        if not (len(currency) == 3 and currency.isascii()
                and currency.isalpha() and currency.isupper()):
            invalid.append("currency")

    valid = not missing and not invalid
    if "userId" in missing or "tenantId" in missing:
        next_step = "NEED_TRUSTED_CONTEXT"
    elif "approvalId" in missing:
        next_step = "REQUEST_APPROVAL"
    elif "idempotencyKey" in missing:
        next_step = "PREPARE_IDEMPOTENCY"
    elif not valid:
        next_step = "REQUEST_MISSING_OR_CORRECTED_INPUT"
    elif action in {"REFUND", "CHANGE_ADDRESS"}:
        next_step = "HUMAN_REVIEW"
    else:
        next_step = "RUNTIME_AUTHORIZATION"

    return {
        "valid": valid,
        "riskLevel": risk,
        "requiredScope": required_scope,
        "missingFields": missing,
        "invalidFields": invalid,
        "nextStep": next_step,
    }


def main():
    raw = sys.stdin.buffer.read(MAX_INPUT_BYTES + 1)
    if len(raw) > MAX_INPUT_BYTES:
        print("input exceeds 65536 bytes", file=sys.stderr)
        return 2
    try:
        request = json.loads(raw.decode("utf-8"))
        result = validate(request)
    except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as error:
        print(f"invalid request: {error}", file=sys.stderr)
        return 2
    print(json.dumps(result, ensure_ascii=False, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
