package com.example.carsharingapp.repository;

import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.model.Payment.Status;
import io.micrometer.common.lang.Nullable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"rental", "rental.user", "rental.car"})
    @Query("""
            SELECT p FROM Payment p
            WHERE (:userId IS NULL OR p.rental.user.id = :userId)
            """)
    Page<Payment> findAllByUserId(@Nullable Long userId, Pageable pageable);

    Optional<Payment> findBySessionId(String sessionId);

    boolean existsByRentalIdAndTypeAndStatusIn(
            Long rentalId,
            Payment.Type type,
            List<Status> statuses
    );

    Optional<Payment> findByRentalIdAndTypeAndStatus(
            Long rentalId,
            Payment.Type type,
            Payment.Status status);
}
