export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  roles: string[];
  createdAt: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  active: boolean;
  createdAt: string;
}

export interface Product {
  id: number;
  name: string;
  slug: string;
  sku: string;
  description: string | null;
  price: number;
  stockQuantity: number;
  lowStockThreshold: number;
  active: boolean;
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CartItem {
  cartItemId: number;
  productId: number;
  productName: string;
  productSlug: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface CartResponse {
  cartId: number;
  userId: number;
  items: CartItem[];
  totalPrice: number;
}

export interface ShipTo {
  recipientName: string | null;
  addressLine: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  country: string | null;
  phone: string | null;
}

export interface PaymentInfo {
  transactionId: string;
  amount: number;
  paidAt: string;
  refunded: boolean;
  refundedAt: string | null;
}

export interface OrderItem {
  orderItemId: number;
  productId: number;
  productName: string;
  productSlug: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderResponse {
  orderId: number;
  status: string;
  totalAmount: number;
  discountAmount: number;
  couponCode: string | null;
  shippingAddress: ShipTo;
  payment: PaymentInfo | null;
  items: OrderItem[];
  createdAt: string;
}

export interface Review {
  reviewId: number;
  productId: number;
  userId: number;
  userFullName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ProductReviewsResponse {
  productId: number;
  averageRating: number;
  totalReviews: number;
  reviews: Review[];
}