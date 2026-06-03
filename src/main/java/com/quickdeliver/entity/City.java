package com.quickdeliver.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cities")
@Data
@NoArgsConstructor
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String state;

    // Centre-point of the city — used for geolocation matching
    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private boolean active = true;
}
