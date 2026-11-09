package com.farias.jengine.toolkit3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesNestedValues() {
        Object value = Json.parse(" {\"a\": [1, -2.5, 3e2, true, false, null], \"b\": {\"c\": \"x\"}, \"e\": {}, \"f\": []} ");
        Map<?, ?> map = (Map<?, ?>) value;
        assertEquals(List.of(1.0, -2.5, 300.0, true, false), ((List<?>) map.get("a")).subList(0, 5));
        assertNull(((List<?>) map.get("a")).get(5));
        assertEquals("x", ((Map<?, ?>) map.get("b")).get("c"));
        assertTrue(((Map<?, ?>) map.get("e")).isEmpty());
        assertTrue(((List<?>) map.get("f")).isEmpty());
    }

    @Test
    void decodesEscapes() {
        assertEquals("a\"b\\c/d\ne\tfé", Json.parse("\"a\\\"b\\\\c\\/d\\ne\\tf\\u00e9\""));
    }

    @Test
    void reportsErrorsWithPosition() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\" 1}"));
        assertTrue(error.getMessage().contains("position 5"), error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[1, 2"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("\"open"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} x"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("tru"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("-"));
    }
}
