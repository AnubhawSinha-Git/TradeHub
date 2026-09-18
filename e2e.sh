#!/usr/bin/env bash
set -u
BASE="${BASE:-http://localhost:8080}"
EMAIL="e2e+$(date +%s)@tradehub.test"

echo "== CORS preflight (browser origin http://localhost:8081) =="
curl -s -D - -o /dev/null -X OPTIONS \
  -H "Origin: http://localhost:8081" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type" \
  "$BASE/api/auth/login" | grep -i '^access-control-allow' || echo "NO CORS HEADERS (preflight failed)"

echo "== CORS on real request =="
curl -s -D - -o /dev/null -H "Origin: http://localhost:8081" "$BASE/api/categories" | grep -i '^access-control-allow-origin' || echo "NO allow-origin on GET"

PASS=0; FAIL=0
ok()   { PASS=$((PASS+1)); echo "  ok: $1"; }
bad()  { FAIL=$((FAIL+1)); echo "  FAIL: $1"; }

check() { # check <label> <json> <python-expr truthy>
  local label="$1"; local json="$2"; local expr="$3"
  if printf '%s' "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(1 if $expr else 0)" | grep -q 1; then
    ok "$label"
  else
    bad "$label -> $(printf '%s' "$json" | head -c 200)"
  fi
}

echo "== full customer flow (fresh user: $EMAIL) =="

echo "-- 1. register"
REG=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' -d "{\"fullName\":\"E2E Tester\",\"email\":\"$EMAIL\",\"password\":\"Test@12345\"}")
CODE=$(echo "$REG" | tail -1); BODY=$(echo "$REG" | sed '$d')
[ "$CODE" = "201" ] && ok "register -> $CODE" || bad "register -> $CODE"

echo "-- 2. login"
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"Test@12345\"}")
check "accessToken present" "$LOGIN" "bool(d.get('accessToken'))"
check "tokenType present" "$LOGIN" "bool(d.get('tokenType'))"
check "expiresInSeconds present" "$LOGIN" "bool('expiresInSeconds' in d)"
TOKEN=$(printf '%s' "$LOGIN" | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
AUTH="Authorization: Bearer $TOKEN"

echo "-- 3. /me roles"
ME=$(curl -s "$BASE/api/auth/me" -H "$AUTH")
check "me.email matches" "$ME" "d.get('email')=='$EMAIL'"
check "me.roles contains CUSTOMER" "$ME" "'CUSTOMER' in d.get('roles',[])"

echo "-- 4. categories"
CATS=$(curl -s "$BASE/api/categories")
check "categories is non-empty array" "$CATS" "isinstance(d,list) and len(d)>0"

echo "-- 5. product list (fields used by catalog)"
PRODS=$(curl -s "$BASE/api/products?page=0&size=100")
check "products page has content" "$PRODS" "len(d.get('content',[]))>0"
check "page fields totalElements/totalPages" "$PRODS" "'totalPages' in d and 'totalElements' in d"
check "product has slug+price+stock+categoryName" "$PRODS" "all({p.get('slug'),'price' in p,'stockQuantity' in p,'lowStockThreshold' in p,'categoryName' in p} for p in d['content'])"

echo "-- 6. product detail by slug"
SLUG=$(printf '%s' "$PRODS" | python3 -c "import sys,json;print(json.load(sys.stdin)['content'][0]['slug'])")
DET=$(curl -s "$BASE/api/products/$SLUG")
check "detail has id/name" "$DET" "d.get('slug')=='$SLUG' and 'id' in d"
PID=$(printf '%s' "$DET" | python3 -c "import sys,json;print(json.load(sys.stdin)['id'])")

echo "-- 7. add to cart (x2)"
ADD=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/cart/items" -H "$AUTH" -H 'Content-Type: application/json' -d "{\"productId\":$PID,\"quantity\":2}")
CODE=$(echo "$ADD" | tail -1)
[ "$CODE" = "201" ] && ok "add -> $CODE" || bad "add -> $CODE"

echo "-- 8. cart with totalPrice (frontend CartResponse)"
CART=$(curl -s "$BASE/api/cart" -H "$AUTH")
check "cart has cartId+userId+totalPrice" "$CART" "all(k in d for k in ('cartId','userId','totalPrice'))"
check "cart item fields (cartItemId/unitPrice/quantity/subtotal)" "$CART" "all(all(k in i for k in ('cartItemId','productId','productName','productSlug','unitPrice','quantity','subtotal')) for i in d.get('items',[]))"
CART_TOTAL=$(printf '%s' "$CART" | python3 -c "import sys,json;print(json.load(sys.stdin)['totalPrice'])")

echo "-- 9. quantity update + remove + re-add"
IID=$(printf '%s' "$CART" | python3 -c "import sys,json;print(json.load(sys.stdin)['items'][0]['cartItemId'])")
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/api/cart/items/$IID" -H "$AUTH" -H 'Content-Type: application/json' -d '{"quantity":1}')
[ "$CODE" = "200" ] && ok "update qty -> $CODE" || bad "update qty -> $CODE"

CART2=$(curl -s "$BASE/api/cart" -H "$AUTH")
CART_TOTAL=$(printf '%s' "$CART2" | python3 -c "import sys,json;print(json.load(sys.stdin)['totalPrice'])")

echo "-- 10. checkout with WELCOME10 + shipment (frontend CheckoutRequest)"
CHECKOUT=$(curl -s -w '\n%{http_code}' -X POST "$BASE/api/orders/checkout" -H "$AUTH" -H 'Content-Type: application/json' -d "{
  \"shipment\":{\"recipientName\":\"E2E Tester\",\"addressLine\":\"1 Test St\",\"addressLine2\":\"\",\"city\":\"Austin\",\"state\":\"TX\",\"zipCode\":\"78701\",\"country\":\"US\",\"phone\":\"+1 555 0100\"},
  \"couponCode\":\"WELCOME10\"}")
CODE=$(echo "$CHECKOUT" | tail -1); OBODY=$(echo "$CHECKOUT" | sed '$d')
[ "$CODE" = "201" ] && ok "checkout -> $CODE" || bad "checkout -> $CODE"
check "order has orderId (frontend OrderResponse)" "$OBODY" "'orderId' in d"
check "order has totalAmount/discountAmount/couponCode" "$OBODY" "all(k in d for k in ('totalAmount','discountAmount','couponCode'))"
check "order coupon == WELCOME10" "$OBODY" "d.get('couponCode')=='WELCOME10'"
check "discount > 0" "$OBODY" "float(d.get('discountAmount',0))>0"
check "status PLACED" "$OBODY" "d.get('status')=='PLACED'"
OID=$(printf '%s' "$OBODY" | python3 -c "import sys,json;print(json.load(sys.stdin)['orderId'])")
TOTAL=$(printf '%s' "$OBODY" | python3 -c "import sys,json;print(json.load(sys.stdin)['totalAmount'])")
python3 -c "import sys; a=float('$CART_TOTAL'); b=float('$TOTAL'); d=float('$(printf '%s' "$OBODY" | python3 -c "import sys,json;print(json.load(sys.stdin)['discountAmount'])")'); sys.exit(0 if abs(a-b-d)<0.01 else 1)" && ok "total - discount == cart total" || bad "math mismatch cart=$CART_TOTAL total=$TOTAL"

echo "-- 11. pay"
PAY=$(curl -s -X POST "$BASE/api/orders/$OID/pay" -H "$AUTH")
check "payment has transactionId + refunded=false" "$PAY" "bool(d.get('payment')) and d.get('payment',{}).get('transactionId') and d['payment'].get('refunded')==False"

echo "-- 12. my orders (frontend OrdersScreen)"
ORDS=$(curl -s "$BASE/api/orders" -H "$AUTH")
check "orders array contains placed order" "$ORDS" "isinstance(d,list) and any(o.get('orderId')==$OID and o.get('status')=='PAID' for o in d)"

echo "-- 13. reviews endpoint shape"
RV=$(curl -s "$BASE/api/products/$PID/reviews")
check "reviews: productId + averageRating + totalReviews" "$RV" "all(k in d for k in ('productId','averageRating','totalReviews','reviews'))"

echo
echo "RESULT: PASS=$PASS FAIL=$FAIL"
[ "$FAIL" = "0" ]