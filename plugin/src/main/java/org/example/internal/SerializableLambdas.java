package org.example.internal;

import org.gradle.api.specs.Spec;

import java.io.Serializable;

public class SerializableLambdas {
    public static <T> Spec<T> spec(SerializableSpec<T> spec) {
        return spec;
    }

    public interface SerializableSpec<T> extends Spec<T>, Serializable {
    }
}
