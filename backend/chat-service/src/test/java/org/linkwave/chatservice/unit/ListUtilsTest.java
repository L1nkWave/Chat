package org.linkwave.chatservice.unit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.linkwave.chatservice.common.ListUtils.iterateChunks;

public class ListUtilsTest {

    @Nested
    class IterateChunksTest {

        private static final Consumer<List<Integer>> DO_NOTHING = list -> {};

        @Test
        void shouldThrowExceptionWhenChunkSizeIsInvalid() {
            assertThrows(IllegalArgumentException.class, () -> iterateChunks(emptyList(), -1, DO_NOTHING));
            assertThrows(IllegalArgumentException.class, () -> iterateChunks(emptyList(), 0, DO_NOTHING));
        }

        @Test
        void shouldNotBeAnyIterationsOnEmptyList() {
            final int step = 10;
            final int expectedIterations = 0;

            assertThat(iterateChunks(emptyList(), step, DO_NOTHING)).isEqualTo(expectedIterations);
        }

        @Test
        void shouldIterateNonEmptyList() {
            final int step = 10;

            final var dataset1 = List.of(1);
            final var dataset2 = rangeClosed(1, 5).boxed().toList();
            final var dataset3 = rangeClosed(1, 20).boxed().toList();
            final var dataset4 = rangeClosed(1, 50).boxed().toList();

            final int i1 = iterateChunks(dataset1, step, DO_NOTHING);
            final int i2 = iterateChunks(dataset2, step, DO_NOTHING);
            final int i3 = iterateChunks(dataset3, step, DO_NOTHING);
            final int i4 = iterateChunks(dataset4, step, DO_NOTHING);

            assertThat(i1).isEqualTo(1);
            assertThat(i2).isEqualTo(1);
            assertThat(i3).isEqualTo(2);
            assertThat(i4).isEqualTo(5);
        }

        @Test
        void shouldApplyActionForChunks() {
            final int step = 10;
            final var dataset = rangeClosed(1, 20).boxed().toList();
            final int expectedSum = dataset.stream().mapToInt(i -> i).sum();

            final var sum = new AtomicInteger();
            iterateChunks(dataset, step, chunk -> chunk.forEach(sum::addAndGet));

            assertThat(sum.get()).isEqualTo(expectedSum);
        }

    }

}
