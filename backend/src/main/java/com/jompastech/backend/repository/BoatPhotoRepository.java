package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.BoatPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BoatPhotoRepository extends JpaRepository<BoatPhoto, Long> {

    /**
     * Finds all photos for a specific boat, ordered by display order.
     *
     * @param boatId the ID of the boat
     * @return list of boat photos ordered by ordem (display order)
     */
    List<BoatPhoto> findByBoatIdOrderByOrdemAsc(Long boatId);

    /**
     * Deletes all photos for a specific boat.
     *
     * @param boatId the ID of the boat
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM BoatPhoto bp WHERE bp.boat.id = :boatId")
    void deleteByBoatId(@Param("boatId") Long boatId);

    /**
     * Finds a photo by its public ID from Cloudinary.
     *
     * @param publicId the public ID from Cloudinary
     * @return the boat photo entity
     */
    BoatPhoto findByPublicId(String publicId);

    /**
     * Counts the number of photos for a specific boat.
     *
     * @param boatId the ID of the boat
     * @return the count of photos
     */
    long countByBoatId(Long boatId);
}