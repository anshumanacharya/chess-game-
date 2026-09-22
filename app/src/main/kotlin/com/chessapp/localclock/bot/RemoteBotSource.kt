package com.chessapp.localclock.bot

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.chessapp.engine.Color
import com.chessapp.engine.GameState
import com.chessapp.engine.Move
import com.chessapp.engine.MoveGenerator
import com.chessapp.engine.PieceType
import com.chessapp.engine.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Computes the bot's move by running the exact same compiled bot the web build serves — straight
 * from [engineScriptUrl] — in a hidden WebView. This means the app always plays against whatever
 * bot code is currently live on the site, with no app update needed to pick up bot improvements.
 *
 * This never throws: any failure (no network, the page fails to load, a timeout, a JS exception)
 * just makes [chooseMove] return null, so [BotStrategy] can fall back to [LocalBotSource]. The
 * WebView is created lazily on first use and kept alive for reuse; a failed load is retried from
 * scratch on the next call rather than being remembered as a permanent failure, so recovery after
 * a connectivity blip doesn't need a restart.
 */
class RemoteBotSource(
    context: Context,
    private val engineScriptUrl: String = "https://anshumanacharya.github.io/chess-game-/chess-engine.js",
    private val timeoutMillis: Long = 5_000
) : BotSource {

    private val appContext = context.applicationContext
    private var webView: WebView? = null
    private var ready = false

    override suspend fun chooseMove(state: GameState): Move? {
        val decoded = withTimeoutOrNull(timeoutMillis) {
            withContext(Dispatchers.Main) {
                val view = ensureLoaded() ?: return@withContext null
                evaluateBotMove(view, state)
            }
        } ?: return null
        // Match the JS engine's answer back to one of our own Move instances, so the result is
        // guaranteed to be a real legal move this app's own engine recognizes.
        return MoveGenerator.legalMoves(state).firstOrNull {
            it.from == decoded.from && it.to == decoded.to && it.promotion == decoded.promotion
        }
    }

    /** Must run on the main thread — all WebView APIs require it. */
    private suspend fun ensureLoaded(): WebView? {
        val existing = webView
        if (existing != null && ready) return existing

        val view = existing ?: WebView(appContext).also {
            it.settings.javaScriptEnabled = true
            webView = it
        }

        suspendCancellableCoroutine<Unit> { cont ->
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(v: WebView?, url: String?) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            view.loadDataWithBaseURL(null, bootstrapHtml, "text/html", "UTF-8", null)
        }

        val engineIsReady = suspendCancellableCoroutine<Boolean> { cont ->
            view.evaluateJavascript(
                "typeof window[\"web-engine\"] !== \"undefined\" && typeof window[\"web-engine\"].JsGame !== \"undefined\""
            ) { result ->
                if (cont.isActive) cont.resume(result == "true")
            }
        }

        ready = engineIsReady
        return if (engineIsReady) view else null
    }

    /** Must run on the main thread. Replays [state]'s move history into a fresh JsGame instance
     *  (the JS API only has "start a new game and apply moves," not "load an arbitrary position"),
     *  then asks it for the bot's move exactly like the web build does. */
    private suspend fun evaluateBotMove(view: WebView, state: GameState): DecodedMove? {
        val rawResult = suspendCancellableCoroutine<String?> { cont ->
            view.evaluateJavascript(buildScript(state)) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }
        return decodeMove(rawResult)
    }

    private fun buildScript(state: GameState): String {
        val movesJson = JSONArray().apply {
            for (move in state.moveHistory) {
                put(
                    JSONObject().apply {
                        put("fromFile", move.from.file)
                        put("fromRank", move.from.rank)
                        put("toFile", move.to.file)
                        put("toRank", move.to.rank)
                        put("promotion", move.promotion?.name?.lowercase())
                    }
                )
            }
        }
        val color = if (state.sideToMove == Color.WHITE) "white" else "black"
        return """
            (function() {
                try {
                    var Engine = window["web-engine"];
                    var game = new Engine.JsGame();
                    var moves = $movesJson;
                    for (var i = 0; i < moves.length; i++) {
                        var mv = moves[i];
                        game.applyMoveExact(mv.fromFile, mv.fromRank, mv.toFile, mv.toRank, mv.promotion || null);
                    }
                    game.setBot("$color");
                    var chosen = game.playBotMove();
                    if (!chosen) return null;
                    return {
                        fromFile: chosen.fromFile, fromRank: chosen.fromRank,
                        toFile: chosen.toFile, toRank: chosen.toRank,
                        promotion: chosen.promotion || null
                    };
                } catch (e) {
                    return null;
                }
            })();
        """.trimIndent()
    }

    private fun decodeMove(rawResult: String?): DecodedMove? {
        if (rawResult == null || rawResult == "null") return null
        return try {
            val obj = JSONObject(rawResult)
            val promotionName = if (obj.isNull("promotion")) null else obj.optString("promotion")
            DecodedMove(
                from = Square(obj.getInt("fromFile"), obj.getInt("fromRank")),
                to = Square(obj.getInt("toFile"), obj.getInt("toRank")),
                promotion = when (promotionName) {
                    "queen" -> PieceType.QUEEN
                    "rook" -> PieceType.ROOK
                    "bishop" -> PieceType.BISHOP
                    "knight" -> PieceType.KNIGHT
                    else -> null
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    private val bootstrapHtml =
        "<!DOCTYPE html><html><head><script src=\"$engineScriptUrl\"></script></head><body></body></html>"

    private data class DecodedMove(val from: Square, val to: Square, val promotion: PieceType?)
}
