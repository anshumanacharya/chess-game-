package com.chessapp.localclock.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chessapp.engine.Move

/** Vertically scrolling move list for a side panel; auto-scrolls to the latest move. */
@Composable
fun MoveHistoryList(moves: List<Move>, modifier: Modifier = Modifier) {
    val pairs = moves.chunked(2)
    val listState = rememberLazyListState()

    LaunchedEffect(pairs.size) {
        if (pairs.isNotEmpty()) listState.animateScrollToItem(pairs.size - 1)
    }

    LazyColumn(modifier = modifier, state = listState) {
        itemsIndexed(pairs) { index, pair ->
            Row {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
                Text(
                    pair.getOrNull(0)?.toShortAlgebraic().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(76.dp)
                )
                Text(
                    pair.getOrNull(1)?.toShortAlgebraic().orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
