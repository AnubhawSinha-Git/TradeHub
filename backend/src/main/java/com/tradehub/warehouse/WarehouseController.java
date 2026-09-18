package com.tradehub.warehouse;

import com.tradehub.warehouse.dto.LowStockProductResponse;
import com.tradehub.warehouse.dto.RestockItemRequest;
import com.tradehub.warehouse.dto.RestockLogResponse;
import com.tradehub.warehouse.dto.RestockRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/products/low-stock")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ResponseEntity<List<LowStockProductResponse>> getLowStockProducts() {
        return ResponseEntity.ok(warehouseService.getLowStockProducts());
    }

    @PostMapping("/restock")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ResponseEntity<RestockLogResponse> restock(
            @Valid @RequestBody RestockItemRequest request,
            Authentication authentication) {
        RestockLogResponse response = warehouseService.restock(
                request.productId(),
                request.quantity(),
                authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/restock/batch")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ResponseEntity<List<RestockLogResponse>> restockBatch(
            @Valid @RequestBody RestockRequest request,
            Authentication authentication) {
        List<RestockLogResponse> response = warehouseService.restockBatch(
                request,
                authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/restocks")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ResponseEntity<List<RestockLogResponse>> getRestockLogs() {
        return ResponseEntity.ok(warehouseService.getRestockLogs());
    }
}