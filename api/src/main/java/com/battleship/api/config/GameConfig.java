package com.battleship.api.config;

import com.battleship.api.domain.ClassicFleetRules;
import com.battleship.api.domain.FleetRules;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfig {

    /** Pick the rule set here (Strategy). */
    @Bean
    public FleetRules fleetRules() {
        return new ClassicFleetRules();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
