package icu.takeneko.omms.controller.fabric.util;

import java.util.function.Function;

@FunctionalInterface
public interface Lookup<K, V> {
    V get(K key);

    default <T> Lookup<K, T> then(Function<? super V, ? extends T> after) {
        return (t) -> after.apply(get(t));
    }
}
