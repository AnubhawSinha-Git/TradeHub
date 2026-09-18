package com.tradehub.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Order> findAllByStatusOrderByCreatedAtDesc(
            OrderStatus status,
            Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query(value = "select coalesce(sum(o.total_amount), 0) "
            + "from orders o where o.status <> 'CANCELLED'",
            nativeQuery = true)
    BigDecimal sumTotalAmountForNonCancelled();

    @Query(value = "select coalesce(sum(o.total_amount), 0) "
            + "from orders o "
            + "join payments p on p.order_id = o.id",
            nativeQuery = true)
    BigDecimal sumTotalAmountPaid();

    @Query(value = "select count(*) "
            + "from orders o "
            + "join payments p on p.order_id = o.id",
            nativeQuery = true)
    long countPaidOrders();

    @Query(value = "select oi.product_id as productId, "
            + "oi.product_name as productName, "
            + "sum(oi.quantity) as unitsSold, "
            + "sum(oi.unit_price * oi.quantity) as revenue "
            + "from order_items oi "
            + "join orders o on o.id = oi.order_id "
            + "where o.status <> 'CANCELLED' "
            + "group by oi.product_id, oi.product_name "
            + "order by unitsSold desc "
            + "limit :limit",
            nativeQuery = true)
    List<TopProductProjection> findTopProducts(@Param("limit") int limit);

    @Query(value = "select count(*) "
            + "from orders o "
            + "join order_items oi on oi.order_id = o.id "
            + "where o.user_id = :userId "
            + "and oi.product_id = :productId "
            + "and o.status <> 'CANCELLED'",
            nativeQuery = true)
    long existsPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId);
}