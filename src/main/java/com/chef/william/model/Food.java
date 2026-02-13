package com.chef.william.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String imageUrl;

    @OneToMany(mappedBy = "food", fetch = FetchType.LAZY)
    private List<Recipe> recipes = new ArrayList<>();
}
