package org.linkwave.chatservice.common;

import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.function.Consumer;

@UtilityClass
public class ListUtils {

    public static <E> int iterateChunks(@NonNull List<E> sourceList, final int chunkSize,
                                        @NonNull Consumer<List<E>> action) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Parameter \"chunkSize\" can't be less than 1");
        }

        int offset = 0, requests = 0;

        while (offset + chunkSize <= sourceList.size()) {
            action.accept(sourceList.subList(offset, offset + chunkSize));
            offset += chunkSize;
            requests++;
        }

        if (offset != sourceList.size()) {
            action.accept(sourceList.subList(offset, sourceList.size()));
            requests++;
        }

        return requests;
    }

}
