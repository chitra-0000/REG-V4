package com.bnpp.regliss.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LambdaUtil {

    private LambdaUtil(){}

    public static <T> Stream<List<T>> partition(Stream<T> stream, int chunkSize) {
        Iterator<T> originalIterator = stream.iterator();

        Iterator<List<T>> blockIterator = new Iterator<List<T>>() {
            public boolean hasNext() {
                return originalIterator.hasNext();
            }
            public List<T> next() {
                List<T> partition = new ArrayList<>();
                while (originalIterator.hasNext() && partition.size() < chunkSize) {
                    partition.add(originalIterator.next());
                }
                return partition;
            }
        };

        Iterable<List<T>> iterable = () -> blockIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <R> Predicate<R> not(Predicate<R> predicate) {
        return predicate.negate();
    }
}
