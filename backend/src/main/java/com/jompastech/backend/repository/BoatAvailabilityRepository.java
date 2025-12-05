package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for BoatAvailability entity operations.
 *
 * <p>Extends JpaRepository to provide standard CRUD operations and defines
 * custom queries for boat availability specific business logic.</p>
 *
 * <p>Custom queries include:
 * <ul>
 *     <li>Finding availabilities by boat ID</li>
 *     <li>Finding availabilities within specific date ranges</li>
 *     <li>Checking for overlapping availability periods</li>
 *     <li>Finding availabilities that overlap with given date ranges</li>
 * </ul>
 * </p>
 */
@Repository
public interface BoatAvailabilityRepository extends JpaRepository<BoatAvailability, Long> {

    /**
     * Finds all availability slots for a specific boat.
     *
     * @param boatId the ID of the boat
     * @return list of availability slots for the specified boat
     */
    List<BoatAvailability> findByBoatId(Long boatId);

    /**
     * Finds availability slots for a boat that fall completely within a date range.
     *
     * @param boatId the ID of the boat
     * @param startDate the start date boundary (exclusive)
     * @param endDate the end date boundary (exclusive)
     * @return list of availability slots within the specified range
     */
    List<BoatAvailability> findByBoatIdAndStartDateAfterAndEndDateBefore(
            Long boatId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds availability slots for a boat that overlap with a given date range.
     *
     * <p>This query checks for three types of overlap:
     * <ul>
     *     <li>Request start date falls within an existing availability</li>
     *     <li>Request end date falls within an existing availability</li>
     *     <li>Existing availability falls completely within the requested period</li>
     * </ul>
     * </p>
     *
     * @param boat the boat entity
     * @param startDate the start date of the requested period
     * @param endDate the end date of the requested period
     * @return list of availability slots that overlap with the requested period
     */
    @Query("SELECT ba FROM BoatAvailability ba WHERE ba.boat = :boat " +
            "AND (:startDate BETWEEN ba.startDate AND ba.endDate " +
            "OR :endDate BETWEEN ba.startDate AND ba.endDate " +
            "OR ba.startDate BETWEEN :startDate AND :endDate)")
    List<BoatAvailability> findByBoatAndDateRange(
            @Param("boat") Boat boat,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Checks if any availability slot exists for a boat that overlaps with a given period.
     *
     * <p>The overlap condition is true when:
     * <ul>
     *     <li>Availability start date is on or before the requested end date</li>
     *     <li>Availability end date is on or after the requested start date</li>
     * </ul>
     * This covers all possible overlap scenarios between two time periods.</p>
     *
     * @param boatId the ID of the boat
     * @param endDate the end date of the requested period
     * @param startDate the start date of the requested period
     * @return true if any overlapping availability exists, false otherwise
     */
    boolean existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long boatId, LocalDateTime endDate, LocalDateTime startDate);
}