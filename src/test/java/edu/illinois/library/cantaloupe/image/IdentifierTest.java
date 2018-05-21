package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

public class IdentifierTest extends BaseTest {

    private Identifier instance;

    @Before
    public void setUp() {
        instance = new Identifier("cats");
    }

    @Test
    public void testSerialization() throws Exception {
        Identifier identifier = new Identifier("cats");
        try (StringWriter writer = new StringWriter()) {
            new ObjectMapper().writeValue(writer, identifier);
            assertEquals("\"cats\"", writer.toString());
        }
    }

    @Test
    public void testDeserialization() throws Exception {
        Identifier identifier = new ObjectMapper().readValue("\"cats\"",
                Identifier.class);
        assertEquals("cats", identifier.toString());
    }

    @Test
    public void testConstructor() {
        assertEquals("cats", instance.toString());

        try {
            new Identifier(null);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testCompareTo() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("cats");
        assertTrue(id1.compareTo(id2) < 0);
        assertEquals(0, id1.compareTo(id3));
    }

    @Test
    public void testEqualsWithIdentifier() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        Identifier id3 = new Identifier("dogs");
        assertTrue(id1.equals(id2));
        assertFalse(id2.equals(id3));
    }

    @Test
    public void testEqualsWithString() {
        assertTrue(instance.equals("cats"));
        assertFalse(instance.equals("dogs"));
    }

    @Test
    public void testHashCode() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("cats", instance.toString());
    }

}
