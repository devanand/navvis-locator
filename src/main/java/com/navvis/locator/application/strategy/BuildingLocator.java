package com.navvis.locator.application.strategy;

import com.navvis.locator.domain.model.geometry.Building;
import java.util.List;

public interface BuildingLocator {
    List<Building> locate(double x, double y, double z);
    LocateStrategy type();
}