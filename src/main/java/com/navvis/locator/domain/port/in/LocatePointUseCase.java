package com.navvis.locator.domain.port.in;

public interface LocatePointUseCase {
    LocationResult locate(double x, double y, double z);
}