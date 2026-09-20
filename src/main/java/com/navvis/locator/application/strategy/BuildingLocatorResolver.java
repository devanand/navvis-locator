package com.navvis.locator.application.strategy;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BuildingLocatorResolver {

    private final Map<LocateStrategy, BuildingLocator> locators;
    private final LocateStrategyToggle toggle;

    public BuildingLocatorResolver(List<BuildingLocator> locators, LocateStrategyToggle toggle) {
        this.locators = locators.stream()
                .collect(Collectors.toMap(BuildingLocator::type, Function.identity()));
        this.toggle = toggle;
    }

    public BuildingLocator current() {
        return locators.get(toggle.current());
    }
}