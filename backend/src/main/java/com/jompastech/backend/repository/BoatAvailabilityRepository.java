package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoatAvailabilityRepository extends JpaRepository<BoatAvailability, Long> {

    List<BoatAvailability> findByBoatId(Long boatId);
    List<BoatAvailability> findByBoatIdAndStartDateAfterAndEndDateBefore(
            Long boatId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT ba FROM BoatAvailability ba WHERE ba.boat = :boat " +
            "AND (:startDate BETWEEN ba.startDate AND ba.endDate " +
            "OR :endDate BETWEEN ba.startDate AND ba.endDate " +
            "OR ba.startDate BETWEEN :startDate AND :endDate)")
    List<BoatAvailability> findByBoatAndDateRange(
            @Param("boat") Boat boat,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    boolean existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long boatId, LocalDateTime endDate, LocalDateTime startDate);

}
