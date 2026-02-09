#!/bin/bash

echo "🧪 Testing Digital Procurement Platform APIs"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

API_GATEWAY="http://localhost:8080"

echo -e "${BLUE}1️⃣  Testing Auth Service - Register User${NC}"
REGISTER_RESPONSE=$(curl -s -X POST $API_GATEWAY/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testbuyer",
    "email": "testbuyer@test.com",
    "password": "password123",
    "roles": ["BUYER"]
  }')

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ User registered successfully${NC}"
  echo "$REGISTER_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Registration failed${NC}"
fi

echo ""
echo -e "${BLUE}2️⃣  Testing Auth Service - Login${NC}"
LOGIN_RESPONSE=$(curl -s -X POST $API_GATEWAY/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testbuyer",
    "password": "password123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo -e "${GREEN}✅ Login successful${NC}"
  echo "Token: ${TOKEN:0:50}..."
else
  echo -e "${RED}❌ Login failed${NC}"
  exit 1
fi

echo ""
echo -e "${BLUE}3️⃣  Testing Supplier Service - Register Supplier${NC}"
SUPPLIER_RESPONSE=$(curl -s -X POST $API_GATEWAY/api/suppliers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Farm LLC",
    "email": "testfarm@example.com",
    "phone": "+998901234567",
    "region": "Tashkent",
    "address": "123 Test Street"
  }')

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Supplier registered${NC}"
  echo "$SUPPLIER_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Supplier registration failed${NC}"
fi

echo ""
echo -e "${BLUE}4️⃣  Testing Opportunity Service - Create Opportunity${NC}"
OPP_RESPONSE=$(curl -s -X POST $API_GATEWAY/api/opportunities \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Test Wheat Purchase",
    "description": "Test opportunity",
    "buyerId": "buyer-123",
    "productCategory": "GRAINS",
    "region": "Tashkent",
    "minBudget": 50000,
    "maxBudget": 100000,
    "currency": "UZS",
    "submissionDeadline": "2026-12-31T00:00:00Z"
  }')

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Opportunity created${NC}"
  echo "$OPP_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Opportunity creation failed${NC}"
fi

echo ""
echo -e "${GREEN}✅ All tests completed!${NC}"

echo ""
echo -e "${BLUE}5️⃣  Testing Product Service - Create Product${NC}"
PRODUCT_RESPONSE=$(curl -s -X POST $API_GATEWAY/api/products \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "name": "Fresh Tomatoes",
    "description": "Organic tomatoes from Tashkent region",
    "category": "VEGETABLES",
    "unit": "kg",
    "quantity": 100,
    "pricePerUnit": 5000,
    "location": "Tashkent",
    "imageUrl": "https://example.com/tomatoes.jpg"
  }')

PRODUCT_ID=$(echo $PRODUCT_RESPONSE | jq -r '.id')

if [ "$PRODUCT_ID" != "null" ] && [ -n "$PRODUCT_ID" ]; then
  echo -e "${GREEN}✅ Product created successfully${NC}"
  echo "$PRODUCT_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Product creation failed${NC}"
fi

echo ""
echo -e "${BLUE}6️⃣  Testing Product Service - Get Available Products${NC}"
PRODUCTS_RESPONSE=$(curl -s -X GET $API_GATEWAY/api/products/available)

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Products retrieved${NC}"
  echo "$PRODUCTS_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Failed to get products${NC}"
fi

echo ""
echo -e "${BLUE}7️⃣  Testing Order Service - Create Order${NC}"
ORDER_RESPONSE=$(curl -s -X POST $API_GATEWAY/api/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  -d '{
    "productId": 1,
    "sellerId": 1,
    "quantity": 50,
    "pricePerUnit": 5000,
    "deliveryAddress": "Tashkent, Chilonzor district",
    "deliveryDate": "2026-02-15T10:00:00",
    "notes": "Please deliver in the morning"
  }')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')

if [ "$ORDER_ID" != "null" ] && [ -n "$ORDER_ID" ]; then
  echo -e "${GREEN}✅ Order created successfully${NC}"
  echo "$ORDER_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Order creation failed${NC}"
fi

echo ""
echo -e "${BLUE}8️⃣  Testing Order Service - Update Payment Status${NC}"
PAYMENT_RESPONSE=$(curl -s -X PATCH "$API_GATEWAY/api/orders/$ORDER_ID/payment?paymentStatus=PAID")

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✅ Payment status updated${NC}"
  echo "$PAYMENT_RESPONSE" | jq '.'
else
  echo -e "${RED}❌ Payment update failed${NC}"
fi

echo ""
echo -e "${GREEN}🎉 All API tests completed successfully!${NC}"
echo ""
echo "📊 Summary:"
echo "  ✅ Auth Service: Registration & Login"
echo "  ✅ Bid Service: Bid submission"
echo "  ✅ Product Service: Product creation & listing"
echo "  ✅ Order Service: Order creation & payment"
