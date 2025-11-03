package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Data
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "boat_id", nullable = false)
    private Boat boat;

    @Min(1)
    @Max(5)
    private int rating;
    private String comment;

    @CreationTimestamp
    @Column(name = "date", updatable = false)
    private LocalDateTime createdAt;

}
