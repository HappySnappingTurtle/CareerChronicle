package com.hongyuwu.careerchronicle.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.4-07 follow-up: validation warnings must be queryable from the snapshot
 * itself (RegistrySnapshot.validationWarnings()), not just visible as log
 * lines from CareerDataReloadListener -- this is what lets a GameTest or a
 * future tool ask "what does the current registry currently warn about"
 * programmatically.
 */
class RegistrySnapshotTest {

    @Test
    void empty_hasNoValidationWarnings() {
        assertTrue(RegistrySnapshot.EMPTY.validationWarnings().isEmpty());
    }

    @Test
    void constructor_exposesValidationWarningsVerbatim() {
        RegistrySnapshot snapshot = new RegistrySnapshot(
                1L, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                List.of("Skill careerchronicle:foo is castable but declares no fx (missing cast/hit feedback)"));

        assertEquals(1, snapshot.validationWarnings().size());
        assertTrue(snapshot.validationWarnings().get(0).contains("careerchronicle:foo"));
    }

    @Test
    void constructor_defensivelyCopiesWarningsList() {
        List<String> mutable = new ArrayList<>();
        mutable.add("warning A");
        RegistrySnapshot snapshot = new RegistrySnapshot(
                1L, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), mutable);

        mutable.add("warning B (added after construction)");

        assertEquals(1, snapshot.validationWarnings().size(),
                "snapshot's warnings list must not reflect mutations to the caller's list after construction");
    }

    @Test
    void validationWarnings_isImmutable() {
        RegistrySnapshot snapshot = new RegistrySnapshot(
                1L, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                List.of("some warning"));

        assertThrows(UnsupportedOperationException.class, () -> snapshot.validationWarnings().add("oops"));
    }
}
