package com.navvis.locator.application.strategy;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class LocateStrategyToggle {

    private final AtomicReference<LocateStrategy> current =
            new AtomicReference<>(LocateStrategy.JAVA);

    public LocateStrategy current() {
        return current.get();
    }

    public void switchTo(LocateStrategy strategy) {
        current.set(strategy);
    }
}