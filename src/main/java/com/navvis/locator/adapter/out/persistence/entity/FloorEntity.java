package com.navvis.locator.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Polygon;

import java.util.UUID;

@Entity
@Table(name = "floors")
@Getter
@Setter
@NoArgsConstructor
public class FloorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(columnDefinition = "geometry(Polygon)")
    private Polygon outline;

    private double heightMin;
    private double heightMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private BuildingEntity building;
}