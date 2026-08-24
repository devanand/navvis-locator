package com.navvis.locator.application.strategy;

public enum LocateStrategy {

    /** Height filter in DB, ray casting in Java (domain layer). */
    JAVA,

    /** ST_Contains + height filter pushed entirely to PostGIS. */
    POSTGIS
}