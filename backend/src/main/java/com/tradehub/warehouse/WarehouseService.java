package com.tradehub.warehouse;

import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.warehouse.dto.LowStockProductResponse;
import com.tradehub.warehouse.dto.RestockItemRequest;
import com.tradehub.warehouse.dto.RestockLogResponse;
import com.tradehub.warehouse.dto.RestockRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {

    private final ProductRepository productRepository;
    private final RestockLogRepository restockLogRepository;

    public WarehouseService(
            ProductRepository productRepository,
            RestockLogRepository restockLogRepository) {
        this.productRepository = productRepository;
        this.restockLogRepository = restockLogRepository;
    }

    @Transactional(readOnly = true)
    public List<LowStockProductResponse> getLowStockProducts() {
        return productRepository.findLowStock()
                .stream()
                .map(this::toLowStockResponse)
                .toList();
    }

    @Transactional
    public RestockLogResponse restock(
            Long productId,
            Integer quantity,
            String restockedBy) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Restock quantity must be at least 1");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setStockQuantity(product.getStockQuantity() + quantity);

        RestockLog log = new RestockLog();
        log.setProductId(product.getId());
        log.setProductName(product.getName());
        log.setQuantity(quantity);
        log.setRestockedBy(restockedBy);

        return toLogResponse(restockLogRepository.save(log));
    }

    @Transactional
    public List<RestockLogResponse> restockBatch(
            RestockRequest request,
            String restockedBy) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Restock list cannot be empty");
        }

        return request.items()
                .stream()
                .map(item -> restock(
                        item.productId(),
                        item.quantity(),
                        restockedBy))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RestockLogResponse> getRestockLogs() {
        return restockLogRepository.findAllByOrderByRestockedAtDesc()
                .stream()
                .map(this::toLogResponse)
                .toList();
    }

    private LowStockProductResponse toLowStockResponse(Product product) {
        int targetLevel = Math.max(1, product.getLowStockThreshold() * 3);
        int suggested = Math.max(targetLevel - product.getStockQuantity(), 0);

        return new LowStockProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                suggested);
    }

    private RestockLogResponse toLogResponse(RestockLog log) {
        return new RestockLogResponse(
                log.getId(),
                log.getProductId(),
                log.getProductName(),
                log.getQuantity(),
                log.getRestockedBy(),
                log.getRestockedAt());
    }
}