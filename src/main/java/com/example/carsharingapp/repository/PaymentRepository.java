package com.example.carsharingapp.repository;

import com.example.carsharingapp.model.Payment;
import com.example.carsharingapp.model.Payment.Status;
import com.example.carsharingapp.model.Payment.Type;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"rental", "rental.user", "rental.car"})
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"rental", "rental.user", "rental.car"})
    Page<Payment> findAllByRentalUserId(Long userId, Pageable pageable);

    boolean existsByRentalUserIdAndStatus(Long userId, Status status);

    Optional<Payment> findBySessionId(String sessionId);

    boolean existsByRentalIdAndTypeAndStatusIn(
            Long rentalId,
            Payment.Type type,
            List<Status> statuses
    );

    Optional<Payment> findByRentalIdAndTypeAndStatus(
            Long rentalId,
            Type type,
            Status status);

    List<Payment> findAllByStatus(Status status);
}
