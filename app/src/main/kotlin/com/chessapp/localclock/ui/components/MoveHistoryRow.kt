package com.chessapp.localclock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chessapp.engine.Move

@Composable
fun MoveHistoryRow(moves: List<Move>, modifier: Modifier = Modifier) {
    val pairs = moves.chunked(2)
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(pairs) { index, pair ->
            Row {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.width(4.dp)) {}
                Text(pair.getOrNull(0)?.toShortAlgebraic().orEmpty(), style = MaterialTheme.typography.bodySmall)
                if (pair.size > 1) {
                    Row(Modifier.width(6.dp)) {}
                    Text(pair[1].toShortAlgebraic(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
