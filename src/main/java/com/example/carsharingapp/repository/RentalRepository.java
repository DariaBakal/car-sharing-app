package com.example.carsharingapp.repository;

import com.example.carsharingapp.model.Rental;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    @Query("SELECT r FROM Rental r "
            + "JOIN FETCH r.user "
            + "JOIN FETCH r.car "
            + "WHERE (:userId IS NULL OR r.user.id = :userId) "
            + "AND (:isActive IS NULL "
            + "    OR (:isActive = TRUE AND r.actualReturnDate IS NULL) "
            + "    OR (:isActive = FALSE AND r.actualReturnDate IS NOT NULL))")
    Page<Rental> findAllByUserIdAndIsActive(
            Long userId,
            Boolean isActive,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "car"})
    Optional<Rental> findRentalById(Long id);

    @Query("SELECT r FROM Rental r "
            + "JOIN FETCH r.user "
            + "JOIN FETCH r.car "
            + "WHERE r.returnDate <= CURRENT_DATE "
            + "AND r.actualReturnDate IS NULL")
    List<Rental> findOverdueRentals();
}
