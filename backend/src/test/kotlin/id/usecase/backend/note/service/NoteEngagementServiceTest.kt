package id.usecase.backend.note.service

import id.usecase.backend.data.note.InMemoryNoteRepository
import id.usecase.backend.data.note.InMemoryNoteCommentRepository
import id.usecase.backend.data.note.InMemoryNoteReactionRepository
import id.usecase.backend.data.note.InMemoryNoteShareRepository
import id.usecase.backend.domain.note.NoteReactionRepository
import id.usecase.backend.domain.note.NoteVisibility
import id.usecase.backend.domain.note.NoteLoveSnapshot
import id.usecase.backend.domain.note.StoredNote
import id.usecase.backend.domain.note.StoredNoteShare
import id.usecase.backend.service.note.NoteEngagementService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteEngagementServiceTest {
    @Test
    fun loveToggleIsIdempotentForSameUser() {
        runBlocking {
            val fixture = fixture()
            fixture.createNote(noteId = "note-1", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            val firstAdd = fixture.service.addLove(userId = "user-1", noteId = "note-1")
            val secondAdd = fixture.service.addLove(userId = "user-1", noteId = "note-1")
            assertEquals(1, firstAdd.loveCount)
            assertEquals(1, secondAdd.loveCount)
            assertTrue(secondAdd.hasLovedByMe)

            val firstRemove = fixture.service.removeLove(userId = "user-1", noteId = "note-1")
            val secondRemove = fixture.service.removeLove(userId = "user-1", noteId = "note-1")
            assertEquals(0, firstRemove.loveCount)
            assertEquals(0, secondRemove.loveCount)
            assertTrue(!secondRemove.hasLovedByMe)
        }
    }

    @Test
    fun commentCreationRejectsBlankContent() {
        runBlocking {
            val fixture = fixture()
            fixture.createNote(noteId = "note-blank", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            assertFailsWith<IllegalArgumentException> {
                fixture.service.addComment(userId = "user-1", noteId = "note-blank", content = "   ")
            }
        }
    }

    @Test
    fun commentCreationRejectsRawInputLongerThanMaxLength() {
        runBlocking {
            val fixture = fixture()
            fixture.createNote(noteId = "note-long", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            val content = " ${"a".repeat(500)}"

            assertFailsWith<IllegalArgumentException> {
                fixture.service.addComment(userId = "user-1", noteId = "note-long", content = content)
            }
        }
    }

    @Test
    fun interactionRejectedForInaccessiblePrivateNote() {
        runBlocking {
            val fixture = fixture()
            fixture.createNote(noteId = "note-private", ownerUserId = "owner-1", visibility = NoteVisibility.PRIVATE)

            assertFailsWith<IllegalArgumentException> {
                fixture.service.getEngagement(userId = "outsider", noteId = "note-private")
            }
            assertFailsWith<IllegalArgumentException> {
                fixture.service.addLove(userId = "outsider", noteId = "note-private")
            }
            assertFailsWith<IllegalArgumentException> {
                fixture.service.addComment(userId = "outsider", noteId = "note-private", content = "hello")
            }
        }
    }

    @Test
    fun interactionAllowedForLinkSharedAndPublicNote() {
        runBlocking {
            val fixture = fixture()
            fixture.createNote(noteId = "note-link", ownerUserId = "owner-1", visibility = NoteVisibility.LINK_SHARED)
            fixture.createNote(noteId = "note-public", ownerUserId = "owner-2", visibility = NoteVisibility.PUBLIC)

            val linkLove = fixture.service.addLove(userId = "other-user", noteId = "note-link")
            val publicComment = fixture.service.addComment(userId = "other-user", noteId = "note-public", content = "Great")

            assertEquals(1, linkLove.loveCount)
            assertEquals("note-public", publicComment.noteId)
            assertEquals("Great", publicComment.content)
        }
    }

    @Test
    fun commentsListUsesDescendingTimeAndCursor() {
        runBlocking {
            val fixture = fixture(times = mutableListOf(1_000L, 2_000L, 3_000L))
            fixture.createNote(noteId = "note-comments", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            fixture.service.addComment(userId = "u1", noteId = "note-comments", content = "first")
            fixture.service.addComment(userId = "u1", noteId = "note-comments", content = "second")
            fixture.service.addComment(userId = "u1", noteId = "note-comments", content = "third")

            val firstPage = fixture.service.listComments(
                userId = "reader",
                noteId = "note-comments",
                limit = 2,
                beforeEpochMillis = null,
            )
            assertEquals(listOf("third", "second"), firstPage.items.map { it.content })
            assertEquals(2_000L, firstPage.nextBeforeEpochMillis)

            val secondPage = fixture.service.listComments(
                userId = "reader",
                noteId = "note-comments",
                limit = 2,
                beforeEpochMillis = firstPage.nextBeforeEpochMillis,
            )
            assertEquals(listOf("first"), secondPage.items.map { it.content })
            assertNull(secondPage.nextBeforeEpochMillis)
        }
    }

    @Test
    fun commentsPaginationDoesNotSkipOrDuplicateWhenTimestampsAreEqual() {
        runBlocking {
            val fixture = fixture(times = mutableListOf(5_000L, 4_000L, 4_000L, 3_000L))
            fixture.createNote(noteId = "note-same-ts", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            fixture.service.addComment(userId = "u1", noteId = "note-same-ts", content = "newest")
            fixture.service.addComment(userId = "u1", noteId = "note-same-ts", content = "middle-a")
            fixture.service.addComment(userId = "u1", noteId = "note-same-ts", content = "middle-b")
            fixture.service.addComment(userId = "u1", noteId = "note-same-ts", content = "oldest")

            val firstPage = fixture.service.listComments(
                userId = "reader",
                noteId = "note-same-ts",
                limit = 2,
                beforeEpochMillis = null,
            )
            val secondPage = fixture.service.listComments(
                userId = "reader",
                noteId = "note-same-ts",
                limit = 2,
                beforeEpochMillis = firstPage.nextBeforeEpochMillis,
            )

            assertEquals(
                setOf("newest", "middle-a", "middle-b", "oldest"),
                (firstPage.items + secondPage.items).map { it.content }.toSet(),
            )
            assertEquals(
                4,
                (firstPage.items + secondPage.items).map { it.content }.size,
            )
        }
    }

    @Test
    fun commentsCursorIsNullWhenBoundaryExpansionReturnsAllRemainingItems() {
        runBlocking {
            val fixture = fixture(times = mutableListOf(4_000L, 4_000L, 4_000L))
            fixture.createNote(noteId = "note-boundary", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            fixture.service.addComment(userId = "u1", noteId = "note-boundary", content = "one")
            fixture.service.addComment(userId = "u1", noteId = "note-boundary", content = "two")
            fixture.service.addComment(userId = "u1", noteId = "note-boundary", content = "three")

            val firstPage = fixture.service.listComments(
                userId = "reader",
                noteId = "note-boundary",
                limit = 2,
                beforeEpochMillis = null,
            )

            assertEquals(3, firstPage.items.size)
            assertNull(firstPage.nextBeforeEpochMillis)
        }
    }

    @Test
    fun engagementUsesAtomicLoveSnapshotFromRepository() {
        runBlocking {
            val noteRepository = InMemoryNoteRepository()
            val shareRepository = InMemoryNoteShareRepository()
            val reactionRepository = SnapshotOnlyReactionRepository(
                snapshot = NoteLoveSnapshot(loveCount = 3, hasLovedByMe = true),
            )
            val service = NoteEngagementService(
                noteRepository = noteRepository,
                noteShareRepository = shareRepository,
                noteCommentRepository = InMemoryNoteCommentRepository(),
                noteReactionRepository = reactionRepository,
            )
            val fixture = Fixture(
                noteRepository = noteRepository,
                shareRepository = shareRepository,
                service = service,
            )
            fixture.createNote(noteId = "note-atomic", ownerUserId = "owner-1", visibility = NoteVisibility.PUBLIC)

            val engagement = fixture.service.getEngagement(userId = "reader", noteId = "note-atomic")

            assertEquals(3, engagement.loveCount)
            assertTrue(engagement.hasLovedByMe)
            assertEquals(1, reactionRepository.snapshotCalls)
            assertFalse(reactionRepository.countCalls > 0)
            assertFalse(reactionRepository.hasLovedCalls > 0)
        }
    }

    @Test
    fun interactionAllowedForDirectShareOnPrivateNote() {
        runBlocking {
            val fixture = fixture()
            fixture.createNote(noteId = "note-shared", ownerUserId = "owner-1", visibility = NoteVisibility.PRIVATE)
            fixture.shareRepository.createShare(
                StoredNoteShare(
                    noteId = "note-shared",
                    recipientUserId = "recipient-1",
                    sharedAtEpochMillis = 5_000L,
                ),
            )

            val engagement = fixture.service.getEngagement(userId = "recipient-1", noteId = "note-shared")
            assertEquals(0, engagement.loveCount)
        }
    }

    private fun fixture(times: MutableList<Long> = mutableListOf(10_000L)): Fixture {
        val noteRepository = InMemoryNoteRepository()
        val shareRepository = InMemoryNoteShareRepository()
        var commentCounter = 0
        val service = NoteEngagementService(
            noteRepository = noteRepository,
            noteShareRepository = shareRepository,
            noteCommentRepository = InMemoryNoteCommentRepository(),
            noteReactionRepository = InMemoryNoteReactionRepository(),
            idGenerator = {
                commentCounter += 1
                "comment-$commentCounter"
            },
            clock = {
                if (times.isEmpty()) {
                    10_000L
                } else {
                    times.removeAt(0)
                }
            },
        )
        return Fixture(
            noteRepository = noteRepository,
            shareRepository = shareRepository,
            service = service,
        )
    }

    private suspend fun Fixture.createNote(noteId: String, ownerUserId: String, visibility: NoteVisibility) {
        noteRepository.create(
            StoredNote(
                id = noteId,
                ownerUserId = ownerUserId,
                content = "content ${'$'}noteId",
                createdAtEpochMillis = 100L,
                updatedAtEpochMillis = 100L,
                version = 1,
                visibility = visibility,
            ),
        )
    }

    private data class Fixture(
        val noteRepository: InMemoryNoteRepository,
        val shareRepository: InMemoryNoteShareRepository,
        val service: NoteEngagementService,
    )

    private class SnapshotOnlyReactionRepository(
        private val snapshot: NoteLoveSnapshot,
    ) : NoteReactionRepository {
        var snapshotCalls: Int = 0
        var countCalls: Int = 0
        var hasLovedCalls: Int = 0

        override suspend fun addLove(noteId: String, userId: String): Boolean = true

        override suspend fun removeLove(noteId: String, userId: String): Boolean = true

        override suspend fun hasLoved(noteId: String, userId: String): Boolean {
            hasLovedCalls += 1
            throw AssertionError("Service should use snapshot API")
        }

        override suspend fun countLoves(noteId: String): Int {
            countCalls += 1
            throw AssertionError("Service should use snapshot API")
        }

        override suspend fun getLoveSnapshot(noteId: String, userId: String): NoteLoveSnapshot {
            snapshotCalls += 1
            return snapshot
        }
    }
}
