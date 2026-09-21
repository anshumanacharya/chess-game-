import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Kotlin/JS test function names can't contain spaces, unlike the JVM target, so these use
// plain camelCase identifiers instead of chess-engine's `` `natural language` `` JVM test names.
class JsApiTest {

    @Test
    fun newGameReportsWhiteToMoveAndTheStartingBoard() {
        val game = JsGame()
        assertEquals("white", game.sideToMove())
        assertEquals("ongoing", game.status())
        assertEquals(64, game.boardSquares().size)

        val e2 = game.boardSquares().first { it.file == 4 && it.rank == 1 }
        assertEquals("pawn", e2.pieceType)
        assertEquals("white", e2.pieceColor)
    }

    @Test
    fun legalMovesFromE2PawnOffersSingleAndDoublePush() {
        val game = JsGame()
        val moves = game.legalMovesFrom(4, 1)
        val destinations = moves.map { it.toFile to it.toRank }.toSet()
        assertEquals(setOf(4 to 2, 4 to 3), destinations)
    }

    @Test
    fun applyMoveExactPlaysAMoveAndSwitchesSideToMove() {
        val game = JsGame()
        assertTrue(game.applyMoveExact(4, 1, 4, 3, null))
        assertEquals("black", game.sideToMove())
        assertEquals("e4", game.lastMoveAlgebraic())
    }

    @Test
    fun applyMoveExactRejectsAnIllegalMove() {
        val game = JsGame()
        assertTrue(!game.applyMoveExact(4, 1, 4, 4, null))
        assertEquals("white", game.sideToMove())
    }

    @Test
    fun capturedPiecesReflectsACaptureOnTheBoard() {
        val game = JsGame()
        assertTrue(game.applyMoveExact(4, 1, 4, 3, null)) // e4
        assertTrue(game.applyMoveExact(3, 6, 3, 4, null)) // d5
        assertTrue(game.applyMoveExact(4, 3, 3, 4, null)) // exd5
        assertEquals(listOf("pawn"), game.capturedPieces("black").toList())
        assertTrue(game.capturedPieces("white").isEmpty())
    }

    @Test
    fun sideToMoveKingSquareIfInCheckIsNullWhenNotInCheck() {
        val game = JsGame()
        assertNull(game.sideToMoveKingSquareIfInCheck())
    }

    @Test
    fun clockTicksDownActiveColorAndAppliesIncrementOnMoveCompletion() {
        val clock = JsClock(initialMinutes = 5, incrementSeconds = 2, unlimited = false)
        clock.start("white")
        clock.tick(1_000.0)
        assertEquals(5 * 60_000.0 - 1_000.0, clock.remainingMillis("white"))
        assertEquals(5 * 60_000.0, clock.remainingMillis("black"))

        clock.onMoveCompleted("white", "black")
        assertEquals(5 * 60_000.0 - 1_000.0 + 2_000.0, clock.remainingMillis("white"))
        assertEquals("black", clock.activeColor())
    }

    @Test
    fun unlimitedClockNeverRunsOut() {
        val clock = JsClock(initialMinutes = 0, incrementSeconds = 0, unlimited = true)
        assertTrue(clock.isUnlimited())
        clock.start("white")
        clock.tick(999_999_999.0)
        assertNull(clock.flaggedColor())
    }
}
