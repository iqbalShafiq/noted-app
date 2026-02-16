package id.usecase.noted.domain

import org.junit.Assert.*
import org.junit.Test

class NoteVisibilityTest {
    @Test
    fun `fromString returns correct enum for valid values`() {
        assertEquals(NoteVisibility.PRIVATE, NoteVisibility.fromString("PRIVATE"))
        assertEquals(NoteVisibility.LINK_SHARED, NoteVisibility.fromString("LINK_SHARED"))
        assertEquals(NoteVisibility.PUBLIC, NoteVisibility.fromString("PUBLIC"))
    }
    
    @Test
    fun `fromString defaults to PRIVATE for unknown values`() {
        assertEquals(NoteVisibility.PRIVATE, NoteVisibility.fromString("UNKNOWN"))
        assertEquals(NoteVisibility.PRIVATE, NoteVisibility.fromString(""))
    }
    
    @Test
    fun `enum values are correct`() {
        assertEquals(3, NoteVisibility.entries.size)
        assertTrue(NoteVisibility.entries.contains(NoteVisibility.PRIVATE))
        assertTrue(NoteVisibility.entries.contains(NoteVisibility.LINK_SHARED))
        assertTrue(NoteVisibility.entries.contains(NoteVisibility.PUBLIC))
    }
}
