package com.smartstudy.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyllabusParserTest {
    @Test
    void extractsRepresentativeItems() {
        String text = "Assignment 1 - Due 10/08/2026 - 15%\n" +
                "Midterm Exam - August 18, 2026 - 25%\n" +
                "Quiz 1 - 20/08/2026 - 5%";

        var items = new SyllabusParser().parseText(text);

        assertEquals(3, items.size());
        assertEquals(15, items.getFirst().weight());
        assertNotNull(items.get(1).dueDate());
    }

    @Test
    void rejectsPolicySentencesThatContainMisleadingWords() {
        String text = "For all students, absence is marked and may lead to final dismissal. 30%\n" +
                "Final dismissal will not be waived under any circumstances. 10% 09/08/2026\n" +
                "Final Exam - 30/08/2026 - 30%";

        var items = new SyllabusParser().parseText(text);

        assertEquals(1, items.size());
        assertEquals("Final Exam", items.getFirst().title());
    }

    @Test
    void keepsExtractedTitlesWithinDatabaseLimit() {
        String longTitle = "Assignment " + "verylongword ".repeat(40) + "10/08/2026 15%";

        var items = new SyllabusParser().parseText(longTitle);

        assertTrue(items.isEmpty() || items.getFirst().title().length() <= 181);
    }
}
