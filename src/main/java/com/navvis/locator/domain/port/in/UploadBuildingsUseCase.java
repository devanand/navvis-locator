package com.navvis.locator.domain.port.in;

/**
 * Accepts a building-data file, parses it, and persists the buildings.
 * Returns the number of buildings created.
 */
public interface UploadBuildingsUseCase {

    int submit(byte[] fileContent);
}