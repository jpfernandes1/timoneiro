package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.BoatAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoatAvailabilityRepository extends JpaRepository<BoatAvailability, Long> {

    List<BoatAvailability> findByBoatId(Long boatId);
    List<BoatAvailability> findByBoatIdAndStartDateAfterAndEndDateBefore(
            Long boatId, LocalDateTime startDate, LocalDateTime endDate);

    boolean existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long boatId, LocalDateTime endDate, LocalDateTime startDate);

}
