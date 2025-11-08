package org.example;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Clock implements BuildService<Clock.Closs> {
    //private final ConcurrentHashMap.KeySetView<Object, Boolean> holders = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap.KeySetView<Object, Boolean> holders = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Object> dependencies = new ConcurrentHashMap<>();
    interface Closs extends BuildServiceParameters {

    }
}
