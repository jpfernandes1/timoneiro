package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "boat_photos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoatPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "photo_url", nullable = false, length = 1000)
    private String photoUrl;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    // To manage the images on Cloudinary
    @Column(name = "public_id", nullable = false, length = 500)
    private String publicId;

    @Column(name = "file_name")
    private String fileName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boat_id", nullable = false)
    private Boat boat;

    // Without Id constructor to facilitate the creation
    public BoatPhoto(String photoUrl, Integer ordem, String publicId, String fileName, Boat boat) {
        this.photoUrl = photoUrl;
        this.ordem = ordem;
        this.publicId = publicId;
        this.fileName = fileName;
        this.boat = boat;
    }
}