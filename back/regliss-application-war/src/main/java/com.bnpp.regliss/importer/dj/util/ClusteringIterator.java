package com.bnpp.regliss.importer.dj.util;

// [MISSING FROM SCREENSHOTS: lines 3-11 of the original file were not visible in the photos]

public class ClusteringIterator implements Iterator<LinesGroup> {

    @Data
    public static class LinesGroup {
        private final String key;
        private final List<String> lines;
    }

    private final Iterator<String> linesIterator;
    private final Function<String, Optional<String>> separatorExtractor;
    private List<String> previousLines = new ArrayList<>();

    public ClusteringIterator(Iterator<String> linesIterator, Function<String, Optional<String>> separatorExtractor) {
        this.linesIterator = linesIterator;
        this.separatorExtractor = separatorExtractor;
        skipToFirstSeparator();
    }

    private void skipToFirstSeparator() {
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            Optional<String> separatorOpt = separatorExtractor.apply(line);
            if (separatorOpt.isPresent()) {
                previousLines.add(line);
                return;
            }
        }
        throw new IllegalArgumentException("Could not find the first separator");
    }

    @Override
    public LinesGroup next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        String previousKey = separatorExtractor.apply(previousLines.get(0))
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        while (linesIterator.hasNext()) {
            String line = linesIterator.next();
            Optional<String> separatorOpt = separatorExtractor.apply(line);
            if (separatorOpt.isPresent()) {
                LinesGroup group = new LinesGroup(previousKey, previousLines);
                previousLines = new ArrayList<>();
                previousLines.add(line);
                return group;
            } else {
                previousLines.add(line);
            }
        }
        LinesGroup group = new LinesGroup(previousKey, previousLines);
        previousLines = new ArrayList<>();
        return group;
    }

    @Override
    public boolean hasNext() { return !previousLines.isEmpty(); }

    public static Stream<LinesGroup> breakByFirstLines(Iterator<String> linesIterator, Function<String, Optional<String>> separatorExtractor) {
        Iterator<LinesGroup> groupIterator = new ClusteringIterator(linesIterator, separatorExtractor);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(groupIterator, Spliterator.ORDERED), false);
    }

}
