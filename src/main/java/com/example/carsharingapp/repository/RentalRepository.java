package com.example.carsharingapp.repository;

import com.example.carsharingapp.model.Rental;
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
public interface RentalRepository extends JpaRepository<Rental, Long> {
    @EntityGraph(attributePaths = {"user", "car"})
    @Query("SELECT r FROM Rental r "
            + "WHERE (:userId IS NULL OR r.user.id = :userId) "
            + "AND (:isActive IS NULL "
            + "    OR (:isActive = TRUE AND r.actualReturnDate IS NULL) "
            + "    OR (:isActive = FALSE AND r.actualReturnDate IS NOT NULL))")
    Page<Rental> findAllByUserIdAndActualReturnDateStatus(
            @Nullable Long userId,
            @Nullable Boolean isActive,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "car"})
    Optional<Rental> findRentalById(Long id);

    @EntityGraph(attributePaths = {"user", "car"})
    @Query("SELECT r FROM Rental r "
            + "WHERE r.returnDate <= CURRENT_DATE "
            + "AND r.actualReturnDate IS NULL")
    List<Rental> findOverdueRentals();
}
