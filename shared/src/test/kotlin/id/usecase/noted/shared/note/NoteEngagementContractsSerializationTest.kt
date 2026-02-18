package id.usecase.noted.shared.note

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class NoteEngagementContractsSerializationTest {
    private val json: Json = Json

    @Test
    fun noteReactionTypeRoundTrip() {
        val encoded: String = json.encodeToString(NoteReactionType.LOVE)
        val decoded: NoteReactionType = json.decodeFromString(encoded)

        assertEquals("\"LOVE\"", encoded)
        assertEquals(NoteReactionType.LOVE, decoded)
    }

    @Test
    fun noteEngagementDtoRoundTrip() {
        val dto = NoteEngagementDto(
            noteId = "note-1",
            loveCount = 3,
            hasLovedByMe = true,
            commentCount = 7,
        )

        val encoded: String = json.encodeToString(dto)
        val decoded: NoteEngagementDto = json.decodeFromString(encoded)
        val encodedObject = json.parseToJsonElement(encoded).jsonObject

        assertEquals(
            setOf("noteId", "loveCount", "hasLovedByMe", "commentCount"),
            encodedObject.keys,
        )
        assertEquals("note-1", encodedObject.getValue("noteId").jsonPrimitive.content)
        assertEquals(3, encodedObject.getValue("loveCount").jsonPrimitive.int)
        assertFalse(encodedObject.getValue("hasLovedByMe").jsonPrimitive.isString)
        assertEquals(true, encodedObject.getValue("hasLovedByMe").jsonPrimitive.boolean)
        assertEquals(7, encodedObject.getValue("commentCount").jsonPrimitive.int)
        assertEquals(dto, decoded)
    }

    @Test
    fun noteCommentDtoJsonShape() {
        val dto = NoteCommentDto(
            id = "comment-1",
            noteId = "note-1",
            authorUserId = "user-1",
            authorUsername = "tester",
            content = "hello",
            createdAtEpochMillis = 123L,
        )

        val encoded: String = json.encodeToString(dto)
        val encodedObject = json.parseToJsonElement(encoded).jsonObject

        assertEquals(
            setOf("id", "noteId", "authorUserId", "authorUsername", "content", "createdAtEpochMillis"),
            encodedObject.keys,
        )
        assertFalse(encodedObject.getValue("createdAtEpochMillis").jsonPrimitive.isString)
        assertEquals(123L, encodedObject.getValue("createdAtEpochMillis").jsonPrimitive.long)
    }

    @Test
    fun noteCommentsPageDtoRoundTrip() {
        val dto = NoteCommentsPageDto(
            items = listOf(
                NoteCommentDto(
                    id = "comment-1",
                    noteId = "note-1",
                    authorUserId = "user-1",
                    authorUsername = "tester",
                    content = "hello",
                    createdAtEpochMillis = 123L,
                ),
            ),
            nextBeforeEpochMillis = 100L,
        )

        val encoded: String = json.encodeToString(dto)
        val decoded: NoteCommentsPageDto = json.decodeFromString(encoded)
        val encodedObject = json.parseToJsonElement(encoded).jsonObject

        assertEquals(setOf("items", "nextBeforeEpochMillis"), encodedObject.keys)
        assertFalse(encodedObject.getValue("nextBeforeEpochMillis").jsonPrimitive.isString)
        assertEquals(100L, encodedObject.getValue("nextBeforeEpochMillis").jsonPrimitive.long)
        assertEquals(1, encodedObject.getValue("items").jsonArray.size)
        assertEquals(
            setOf("id", "noteId", "authorUserId", "authorUsername", "content", "createdAtEpochMillis"),
            encodedObject.getValue("items").jsonArray.first().jsonObject.keys,
        )

        assertEquals(dto, decoded)
    }

    @Test
    fun noteCommentsPageDtoSerializationOmitsNextBeforeEpochMillisWhenNull() {
        val dto =
            NoteCommentsPageDto(
                items = emptyList(),
                nextBeforeEpochMillis = null,
            )

        val encoded: String = json.encodeToString(dto)
        val encodedObject = json.parseToJsonElement(encoded).jsonObject

        assertEquals(setOf("items"), encodedObject.keys)
        assertEquals(0, encodedObject.getValue("items").jsonArray.size)
    }

    @Test
    fun noteCommentsPageDtoDecodeWithoutNextBeforeEpochMillisDefaultsToNull() {
        val encoded =
            """
            {
              "items": [
                {
                  "id": "comment-1",
                  "noteId": "note-1",
                  "authorUserId": "user-1",
                  "authorUsername": "tester",
                  "content": "hello",
                  "createdAtEpochMillis": 123
                }
              ]
            }
            """.trimIndent()

        val decoded: NoteCommentsPageDto = json.decodeFromString(encoded)

        assertEquals(1, decoded.items.size)
        assertNull(decoded.nextBeforeEpochMillis)
    }

    @Test
    fun createNoteCommentRequestRoundTrip() {
        val dto = CreateNoteCommentRequest(content = "nice note")

        val encoded: String = json.encodeToString(dto)
        val decoded: CreateNoteCommentRequest = json.decodeFromString(encoded)
        val encodedObject = json.parseToJsonElement(encoded).jsonObject

        assertEquals(setOf("content"), encodedObject.keys)
        assertEquals("nice note", encodedObject.getValue("content").jsonPrimitive.content)

        assertEquals(dto, decoded)
    }
}
