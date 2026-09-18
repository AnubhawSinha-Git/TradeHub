package com.tradehub.order;

import com.tradehub.cart.Cart;
import com.tradehub.cart.CartItem;
import com.tradehub.cart.CartItemRepository;
import com.tradehub.cart.CartRepository;
import com.tradehub.common.dto.PageResponse;
import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.coupon.CouponService;
import com.tradehub.order.dto.CheckoutRequest;
import com.tradehub.order.dto.DashboardResponse;
import com.tradehub.order.dto.OrderItemResponse;
import com.tradehub.order.dto.OrderResponse;
import com.tradehub.order.dto.ShipToRequest;
import com.tradehub.order.dto.ShipToResponse;
import com.tradehub.order.dto.TopProductResponse;
import com.tradehub.order.dto.UpdateOrderStatusRequest;
import com.tradehub.payment.Payment;
import com.tradehub.payment.PaymentRepository;
import com.tradehub.payment.dto.PaymentResponse;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PaymentRepository paymentRepository,
            CouponService couponService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.couponService = couponService;
    }

    @Transactional
    public OrderResponse checkout(String email, CheckoutRequest request) {
        User user = findUser(email);

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        applyShippingAddress(order, user, request);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (!product.isActive()) {
                throw new IllegalArgumentException(
                        "Product is inactive: " + product.getName());
            }

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product " + product.getName());
            }

            product.setStockQuantity(
                    product.getStockQuantity() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductSlug(product.getSlug());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            order.getItems().add(orderItem);

            subtotal = subtotal.add(
                    product.getPrice()
                            .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = resolveCouponCode(request);

        if (couponCode != null) {
            couponService.validateAndConsume(couponCode, subtotal);
            discount = couponService.computeDiscount(couponCode, subtotal);
        }

        order.setDiscountAmount(discount);
        order.setCouponCode(couponCode);
        order.setTotalAmount(subtotal.subtract(discount));
        Order savedOrder = orderRepository.save(order);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse payOrder(String email, Long orderId) {
        User user = findUser(email);

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalArgumentException("Only placed orders can be paid");
        }

        if (order.getPayment() != null) {
            throw new IllegalArgumentException("Order already has a payment");
        }

        String transactionId = UUID.randomUUID()
                .toString()
                .replace("-", "");

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        order.setPayment(payment);

        order.setStatus(OrderStatus.PAID);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long totalOrders = orderRepository.count();
        long paidOrders = orderRepository.countPaidOrders();
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        BigDecimal totalRevenue = orderRepository.sumTotalAmountForNonCancelled();
        BigDecimal paidRevenue = orderRepository.sumTotalAmountPaid();
        BigDecimal averageOrderValue = totalOrders == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(
                        BigDecimal.valueOf(totalOrders),
                        2,
                        RoundingMode.HALF_UP);

        List<TopProductResponse> topProducts = orderRepository
                .findTopProducts(5)
                .stream()
                .map(p -> new TopProductResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getUnitsSold(),
                        p.getRevenue()))
                .toList();

        List<OrderResponse> recentOrders = orderRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new DashboardResponse(
                totalOrders,
                paidOrders,
                cancelledOrders,
                totalRevenue,
                paidRevenue,
                averageOrderValue,
                topProducts,
                recentOrders);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = findUser(email);

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(
            OrderStatus status,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending());

        Page<Order> orderPage = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc(pageable)
                : orderRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isFirst(),
                orderPage.isLast());
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String email, Long orderId) {
        User user = findUser(email);

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String email, Long orderId) {
        User user = findUser(email);

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order is already cancelled");
        }

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalArgumentException(
                    "Only placed orders can be cancelled");
        }

        restoreStock(order);
        order.setStatus(OrderStatus.CANCELLED);

        return toResponse(order);
    }

@Transactional
public OrderResponse refundOrder(String email, Long orderId) {
    User user = findUser(email);

    Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    if (order.getStatus() == OrderStatus.CANCELLED) {
        throw new IllegalArgumentException("Order is already cancelled");
    }

    if (order.getStatus() == OrderStatus.PLACED) {
        throw new IllegalArgumentException(
                "Order is not paid yet, cancel it instead");
    }

    refundPayment(order.getPayment());
    restoreStock(order);
    order.setStatus(OrderStatus.CANCELLED);

    return toResponse(order);
}

@Transactional
public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED
                && request.status() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "A cancelled order cannot change status");
        }

        if (request.status() == OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.CANCELLED) {
            refundPayment(order.getPayment());
            restoreStock(order);
        }

        order.setStatus(request.status());

        return toResponse(order);
    }

    private String resolveCouponCode(CheckoutRequest request) {
        if (request == null
                || request.couponCode() == null
                || request.couponCode().isBlank()) {
            return null;
        }

        return request.couponCode()
                .trim()
                .toUpperCase(java.util.Locale.ROOT);
    }

    private void applyShippingAddress(
            Order order,
            User user,
            CheckoutRequest request) {
        if (request == null || request.shipment() == null) {
            order.setRecipientName(user.getFullName());
            return;
        }

        ShipToRequest shipment = request.shipment();
        order.setRecipientName(shipment.recipientName().trim());
        order.setAddressLine(shipment.addressLine().trim());
        order.setAddressLine2(
                shipment.addressLine2() == null
                        ? null
                        : shipment.addressLine2().trim());
        order.setCity(shipment.city().trim());
        order.setState(shipment.state().trim());
        order.setZipCode(shipment.zipCode().trim());
        order.setCountry(shipment.country().trim());
        order.setPhone(shipment.phone());
    }

    private void refundPayment(Payment payment) {
        if (payment != null && !payment.isRefunded()) {
            payment.setRefunded(true);
            payment.setRefundedAt(LocalDateTime.now());
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem orderItem : order.getItems()) {
            productRepository.findById(orderItem.getProductId())
                    .ifPresent(product -> product.setStockQuantity(
                            product.getStockQuantity() + orderItem.getQuantity()));
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getCouponCode(),
                toShippingAddress(order),
                toPayment(order),
                items,
                order.getCreatedAt());
    }

    private PaymentResponse toPayment(Order order) {
        Payment payment = order.getPayment();

        if (payment == null) {
            return null;
        }

        return new PaymentResponse(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getPaidAt(),
                payment.isRefunded(),
                payment.getRefundedAt());
    }

    private ShipToResponse toShippingAddress(Order order) {
        return new ShipToResponse(
                order.getRecipientName(),
                order.getAddressLine(),
                order.getAddressLine2(),
                order.getCity(),
                order.getState(),
                order.getZipCode(),
                order.getCountry(),
                order.getPhone());
    }

    private OrderItemResponse toItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getProductSlug(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getQuantity())));
    }
}