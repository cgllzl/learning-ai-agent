---
name: minimal-order-status-explainer
description: Explain a supplied order status in plain language with one next-step suggestion. Use only when the user provides an order status and asks what it means; do not query or change any order.
---

# Minimal Order Status Explainer

Explain the supplied status in plain language, then give one safe next-step suggestion. Match the user's language.

Never invent an order state, call a Tool, or claim that an order was queried or changed.

## Example

Input: `The order status is PAID. What does that mean?`

Output: `PAID means the order has been paid and is usually waiting for fulfillment; check again later for a shipment update.`
