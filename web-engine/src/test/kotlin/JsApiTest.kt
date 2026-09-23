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
    fun promotionIsNotCountedAsACapturedPawnAndCountsTowardMaterial() {
        val game = JsGame()
        // a4 h6 a5 h5 a6 h4 axb7 h3 bxa8=Q
        val moves = listOf(
            intArrayOf(0, 1, 0, 3), intArrayOf(7, 6, 7, 5), intArrayOf(0, 3, 0, 4), intArrayOf(7, 5, 7, 4),
            intArrayOf(0, 4, 0, 5), intArrayOf(7, 4, 7, 3), intArrayOf(0, 5, 1, 6), intArrayOf(7, 3, 7, 2)
        )
        for (m in moves) assertTrue(game.applyMoveExact(m[0], m[1], m[2], m[3], null))
        assertTrue(game.applyMoveExact(1, 6, 0, 7, "queen"))
        assertTrue(game.capturedPieces("white").isEmpty())
        assertEquals(listOf("rook", "pawn"), game.capturedPieces("black").toList())
        assertEquals(14, game.materialAdvantage("white"))
        assertEquals(-14, game.materialAdvantage("black"))
    }

    @Test
    fun startingPositionRepeatedThreeTimesIsADraw() {
        val game = JsGame()
        // Nf3 Nf6 Ng1 Ng8, twice: the starting position occurs for the third time.
        val shuffle = listOf(intArrayOf(6, 0, 5, 2), intArrayOf(6, 7, 5, 5), intArrayOf(5, 2, 6, 0), intArrayOf(5, 5, 6, 7))
        for (m in shuffle) assertTrue(game.applyMoveExact(m[0], m[1], m[2], m[3], null))
        assertEquals("ongoing", game.status())
        for (m in shuffle) assertTrue(game.applyMoveExact(m[0], m[1], m[2], m[3], null))
        assertEquals("draw_repetition", game.status())
    }

    @Test
    fun sideToMoveKingSquareIfInCheckIsNullWhenNotInCheck() {
        val game = JsGame()
        assertNull(game.sideToMoveKingSquareIfInCheck())
    }

    @Test
    fun withNoBotSetIsBotTurnIsAlwaysFalse() {
        val game = JsGame()
        assertTrue(!game.hasBot())
        assertTrue(!game.isBotTurn())
        assertNull(game.playBotMove())
    }

    @Test
    fun settingBotToTheSideToMoveMakesItsTurn() {
        val game = JsGame()
        game.setBot("white")
        assertTrue(game.hasBot())
        assertTrue(game.isBotTurn())
    }

    @Test
    fun playBotMovePlaysALegalMoveAndSwitchesSideToMove() {
        val game = JsGame()
        game.setBot("white")
        val move = game.playBotMove()
        assertTrue(move != null)
        assertEquals("black", game.sideToMove())
        assertTrue(!game.isBotTurn())
    }

    @Test
    fun playBotMoveDoesNothingWhenItIsNotTheBotsTurn() {
        val game = JsGame()
        game.setBot("black")
        assertNull(game.playBotMove())
        assertEquals("white", game.sideToMove())
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
