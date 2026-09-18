package com.tradehub.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestockLogRepository extends JpaRepository<RestockLog, Long> {

    List<RestockLog> findAllByOrderByRestockedAtDesc();
}