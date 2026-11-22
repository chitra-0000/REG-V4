package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.AbstractSimpleEntity;

import java.util.*;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class EntityCollectionDiff<T extends AbstractSimpleEntity> {
    private final Collection<T> oldCollection;
    private final Collection<T> newCollection;
    private final Function<T, List<Object>> equalityTupleExtractor;

    public EntityCollectionDiff(Collection<T> oldCollection, Collection<T> newCollection, Function<T, List<Object>> equalityCriteriaExtractor) {
        this.equalityTupleExtractor = equalityCriteriaExtractor;
        this.oldCollection = new HashSet<>(oldCollection);
        this.newCollection = new HashSet<>(newCollection);
    }

    public List<T> getDeletedElements() { return collectionsDiffOfEqualityFunction(oldCollection, newCollection); }

    public List<T> getCreatedElements() { return collectionsDiffOfEqualityFunction(newCollection, oldCollection); }

    private List<T> collectionsDiffOfEqualityFunction(Collection<T> fromWhichWillBeRemoved, Collection<T> toBeRemoved) {
        List<Object> toBeRemovedTuples = toBeRemoved.stream().map(equalityTupleExtractor).collect(toList());
        List<T> copyFromWhichWillBeRemoved = fromWhichWillBeRemoved.stream().collect(toList());

        for (Object toBeRemovedTuple :toBeRemovedTuples) {
            Optional<T> firstOpt = copyFromWhichWillBeRemoved.stream().filter(t -> equalityTupleExtractor.apply(t).equals(toBeRemovedTuple)).findFirst();
            if (firstOpt.isPresent()) {
                copyFromWhichWillBeRemoved.remove(firstOpt.get());
            }
        }


        return copyFromWhichWillBeRemoved;
    }

    public Map<T, T> getUpdatedElements() {
        Map<List<Object>, T> oldUpdated = oldCollection.stream()
                .collect(toMap(equalityTupleExtractor, identity(), (x, y) -> x));
        getDeletedElements().stream().map(equalityTupleExtractor).forEach(oldUpdated::remove);

        Map<List<Object>, T> newUpdated = newCollection.stream()
                .filter(newElement -> oldUpdated.containsKey(equalityTupleExtractor.apply(newElement)))
                .collect(toMap(equalityTupleExtractor, Function.identity(), (x, y) -> x));

        return oldUpdated.values().stream()
                .collect(toMap(Function.identity(), old -> newUpdated.get(equalityTupleExtractor.apply(old))));
    }

    @Override
    public String toString() {
        return "Created: " + getCreatedElements() + "; Updated: " + getUpdatedElements() + "; Deleted: "
                + getDeletedElements();
    }

}
