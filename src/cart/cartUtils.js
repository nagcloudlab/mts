/**
 * Cart utility functions for FoodExpress
 * Handles cart total calculation and discount application
 */

function calculateCartTotal(items) {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0);
}

function applyDiscount(total, coupon) {
  if (coupon && coupon.discountPercent) {
    return total - (total * coupon.discountPercent / 100);
  }
  return total;
}

function getDisplayTotal(cartState) {
  const raw = calculateCartTotal(cartState.items);
  const discounted = applyDiscount(raw, cartState.activeCoupon);
  return discounted;
}

module.exports = { calculateCartTotal, applyDiscount, getDisplayTotal };
