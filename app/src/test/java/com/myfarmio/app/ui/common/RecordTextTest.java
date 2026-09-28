package com.myfarmio.app.ui.common;

import org.junit.Test;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class RecordTextTest {
    private Map<String, Object> row(String status, String due) {
        Map<String, Object> value = new HashMap<>();
        value.put("status", status); value.put("due_date", due);
        return value;
    }
    @Test public void searchIgnoresAccentsCaseAndSurroundingSpaces() {
        assertEquals("maiz del norte", RecordText.normalize("  MAÍZ del Norte  "));
    }
    @Test public void searchMatchesDomainFieldsWithoutMutatingThem() {
        Map<String, Object> row = new HashMap<>(); row.put("current_crop", "Maíz temprano");
        assertTrue(RecordText.matches(row, "maiz", "current_crop", "zone"));
        assertFalse(RecordText.matches(row, "trigo", "current_crop"));
        assertEquals("Maíz temprano", row.get("current_crop"));
    }
    @Test public void todayIsNotOverdue() {
        assertFalse(RecordText.overdue(row("pending", "2026-09-27"), LocalDate.of(2026, 9, 27)));
    }
    @Test public void openTaskBeforeTodayIsOverdue() {
        assertTrue(RecordText.overdue(row("in_progress", "2026-09-26"), LocalDate.of(2026, 9, 27)));
    }
    @Test public void closedTasksNeverShowAsOverdue() {
        for (String status : new String[]{"completed", "done", "cancelled"})
            assertFalse(RecordText.overdue(row(status, "2026-01-01"), LocalDate.of(2026, 9, 27)));
    }
    @Test public void missingOrInvalidDatesDoNotBecomeOverdue() {
        assertFalse(RecordText.overdue(row("pending", null), LocalDate.of(2026, 9, 27)));
        assertNull(RecordText.date("invalid"));
        assertNull(RecordText.date("2026-02-30"));
    }
    @Test public void dateTimeUsesItsDatePrefix() {
        assertEquals(LocalDate.of(2026, 9, 27), RecordText.date("2026-09-27T14:30:00Z"));
    }
    @Test public void numberFormattingDoesNotInventUnits() {
        assertEquals("1.240,5", RecordText.amount(1240.5));
    }
    @Test public void nonFiniteNumbersAreNotRendered() {
        Map<String, Object> row = new HashMap<>(); row.put("weight", Double.NaN);
        assertEquals(0, RecordText.number(row, "weight"), 0.0);
    }
    @Test public void displayLabelsPreserveUnknownStates() {
        assertEquals("En progreso", RecordText.label("in_progress"));
        assertEquals("custom state", RecordText.label("custom_state"));
        assertEquals("Sin estado", RecordText.label(""));
    }
    @Test public void originalExamplesRemainIsolatedAndEditable() {
        List<Map<String, Object>> first = LocalExamples.forModule("tasks");
        List<Map<String, Object>> second = LocalExamples.forModule("tasks");
        assertEquals(3, first.size());
        assertEquals("Reponer stock de urea", first.get(0).get("title"));
        first.get(0).put("status", "completed");
        assertEquals("pending", second.get(0).get("status"));
        assertTrue(LocalExamples.forModule("fields").isEmpty());
        assertEquals(3, LocalExamples.forModule("livestock").size());
    }
}

