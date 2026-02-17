package com.chef.william.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instructions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"recipe_id", "step"})
        },
        indexes = {
                @Index(name = "idx_recipe_step", columnList = "recipe_id, step")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer step;  // Use Integer instead of int for null safety

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "tutorial_video_url", length = 500)
    private String tutorialVideoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;


}