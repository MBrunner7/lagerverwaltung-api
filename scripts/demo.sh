#!/usr/bin/env bash
# Walks through a typical warehouse scenario against a running instance (docker compose up).
# Usage: ./scripts/demo.sh [base-url]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
SUFFIX="$(date +%s)"

post() {
  curl -sS -X POST "$BASE_URL$1" -H 'Content-Type: application/json' -d "$2"
}

id_of() {
  sed -E 's/.*"id":([0-9]+).*/\1/'
}

echo "== Create product and two storage locations"
PRODUCT=$(post /api/products "{\"sku\":\"BOLT-M8-$SUFFIX\",\"name\":\"Hex bolt M8x40\",\"unit\":\"PCS\",\"minStock\":100}" | id_of)
SHELF_A=$(post /api/locations "{\"code\":\"A-01-$SUFFIX\",\"name\":\"Main warehouse, aisle A\"}" | id_of)
SHELF_B=$(post /api/locations "{\"code\":\"PROD-$SUFFIX\",\"name\":\"Production supply\"}" | id_of)
echo "product=$PRODUCT shelfA=$SHELF_A shelfB=$SHELF_B"

echo; echo "== Goods receipt: 250 pcs into aisle A"
post /api/movements/receipts "{\"productId\":$PRODUCT,\"locationId\":$SHELF_A,\"quantity\":250,\"reference\":\"PO-4711\"}"

echo; echo; echo "== Transfer: 80 pcs to production supply"
post /api/movements/transfers "{\"productId\":$PRODUCT,\"fromLocationId\":$SHELF_A,\"toLocationId\":$SHELF_B,\"quantity\":80}"

echo; echo; echo "== Goods issue: 200 pcs from aisle A (only 170 available -> 409)"
post /api/movements/issues "{\"productId\":$PRODUCT,\"locationId\":$SHELF_A,\"quantity\":200}"

echo; echo; echo "== Goods issue: 120 pcs from aisle A"
post /api/movements/issues "{\"productId\":$PRODUCT,\"locationId\":$SHELF_A,\"quantity\":120}"

echo; echo; echo "== Current stock of the product"
curl -sS "$BASE_URL/api/stock?productId=$PRODUCT"

echo; echo; echo "== Low-stock report (total 130 is above the reorder point of 100, so nothing yet)"
curl -sS "$BASE_URL/api/stock/low"

echo; echo; echo "== Movement history"
curl -sS "$BASE_URL/api/movements?productId=$PRODUCT"
echo
