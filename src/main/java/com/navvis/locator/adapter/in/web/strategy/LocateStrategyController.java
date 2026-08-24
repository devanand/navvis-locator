package com.navvis.locator.adapter.in.web.strategy;

import com.navvis.locator.adapter.in.web.strategy.dto.StrategyRequest;
import com.navvis.locator.adapter.in.web.strategy.dto.StrategyResponse;
import com.navvis.locator.application.strategy.LocateStrategyToggle;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategy")
public class LocateStrategyController {

    private final LocateStrategyToggle toggle;

    public LocateStrategyController(LocateStrategyToggle toggle) {
        this.toggle = toggle;
    }

    @GetMapping
    public StrategyResponse current() {
        return new StrategyResponse(toggle.current());
    }

    @PutMapping
    public StrategyResponse switchStrategy(@RequestBody StrategyRequest request) {
        toggle.switchTo(request.strategy());
        return new StrategyResponse(toggle.current());
    }
}