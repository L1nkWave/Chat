package org.linkwave.chatservice.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordFilterTest {

    // Từ giả định để kiểm thử; thay bằng danh sách thật trong blocked-words.txt
    private final WordFilter filter = new WordFilter(List.of("# chú thích", "badword", "bad phrase"));

    @Test
    void detectsPlainWord() {
        assertTrue(filter.findViolation("this is a badword here").isPresent());
    }

    @Test
    void ignoresCaseAndDiacritics() {
        assertTrue(filter.findViolation("BÁDWÓRD").isPresent());
    }

    @Test
    void detectsLeetSpelling() {
        assertTrue(filter.findViolation("b4dw0rd").isPresent());
    }

    @Test
    void detectsSpacedOutLetters() {
        assertTrue(filter.findViolation("b.a.d.w.o.r.d").isPresent());
    }

    @Test
    void detectsPhrase() {
        assertTrue(filter.findViolation("a bad phrase appears").isPresent());
    }

    @Test
    void allowsCleanText() {
        assertTrue(filter.findViolation("xin chào các bạn, hẹn gặp lại").isEmpty());
    }

    @Test
    void ignoresBlankAndNull() {
        assertTrue(filter.findViolation(null).isEmpty());
        assertTrue(filter.findViolation("   ").isEmpty());
    }

    @Test
    void commentLinesAreNotWords() {
        assertTrue(filter.findViolation("chú thích").isEmpty());
    }

}