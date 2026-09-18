package com.tradehub.cart;

import com.tradehub.cart.dto.AddCartItemRequest;
import com.tradehub.cart.dto.CartItemResponse;
import com.tradehub.cart.dto.CartResponse;
import com.tradehub.cart.dto.UpdateCartItemRequest;
import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(String email) {
        User user = findUser(email);
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String email, AddCartItemRequest request) {
        User user = findUser(email);
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is inactive");
        }

        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + request.quantity();
            validateStock(product, newQuantity);
            item.setQuantity(newQuantity);
        } else {
            validateStock(product, request.quantity());

            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.quantity());
            item.setUnitPrice(product.getPrice());
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }

        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(
            String email,
            Long cartItemId,
            UpdateCartItemRequest request) {
        User user = findUser(email);
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        ensureItemBelongsToCart(item, cart.getId());

        Product product = item.getProduct();
        validateStock(product, request.quantity());
        item.setQuantity(request.quantity());

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String email, Long cartItemId) {
        User user = findUser(email);
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        ensureItemBelongsToCart(item, cart.getId());

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return toResponse(cart);
    }

    @Transactional
    public void clearCart(String email) {
        User user = findUser(email);
        Cart cart = getOrCreateCart(user);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
    }

    private void validateStock(Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product " + product.getName());
        }
    }

    private void ensureItemBelongsToCart(CartItem item, Long cartId) {
        if (!item.getCart().getId().equals(cartId)) {
            throw new IllegalArgumentException(
                    "Cart item does not belong to the user's cart");
        }
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = cart.getItems().stream()
                .map(this::computeSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                cart.getUser().getId(),
                items,
                total);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                item.getUnitPrice(),
                item.getQuantity(),
                computeSubtotal(item));
    }

    private BigDecimal computeSubtotal(CartItem item) {
        return item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}