package com.bnpp.regliss.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapUtils {

    public static <K,V> Map<K,V> filterKeys(Map<K, V> original, Predicate<K> test) {
        return original.entrySet().stream()
                .filter(e -> test.test(e.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <K,V> Map<K,V> filterValues(Map<K, V> original, Predicate<V> test) {
        return original.entrySet().stream()
                .filter(e -> test.test(e.getValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <K,V1,V2> Map<K,V2> mapValues(Map<K, V1> original, Function<V1, V2> mapper) {
        return original.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, entry -> mapper.apply(entry.getValue())));
    }

    public static <K1,K2,V> Map<K2,V> mapKeys(Map<K1, V> original, Function<K1, K2> mapper) {
        return original.entrySet().stream()
                .collect(toMap(entry -> mapper.apply(entry.getKey()), Map.Entry::getValue));
    }
}
