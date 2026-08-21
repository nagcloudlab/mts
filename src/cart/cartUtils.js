/**
 * Cart utility functions for FoodExpress
 * Handles cart total calculation and discount application
 */

function calculateCartTotal(items) {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0);
}

function applyDiscount(total, coupon) {
  if (!coupon || coupon.discountPercent === undefined) {
    return total;
  }
  const discount = total * coupon.discountPercent / 100;
  return total - discount;
}

function getDisplayTotal(cartState) {
  if (!cartState || !cartState.items) {
    return 0;
  }
  const raw = calculateCartTotal(cartState.items);
  const discounted = applyDiscount(raw, cartState.activeCoupon);
  console.log('Cart total calculated:', discounted);
  return discounted;
}

// TODO: add coupon validation

module.exports = { calculateCartTotal, applyDiscount, getDisplayTotal };
