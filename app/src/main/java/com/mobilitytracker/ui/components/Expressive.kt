package com.mobilitytracker.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

// Wspólne klocki UI: każdy ekran składa się z nagłówka i kart AppCard,
// a wewnątrz karty najwyżej jeden poziom InsetBlock.

@Composable
fun ExpressiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

/** Przewijana kolumna ekranu ze wspólnymi marginesami i odstępem między kartami. */
@Composable
fun ScreenColumn(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun ExpressiveHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/** Jedyny styl karty w aplikacji. Wyróżnienie tylko przez containerColor (np. primaryContainer). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColorFor(containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Blok wewnątrz karty (ćwiczenie, rozgrzewka). Nie zagnieżdżamy głębiej. */
@Composable
fun InsetBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** Nagłówek karty: tytuł, opcjonalny opis i element po prawej. Klikalny, gdy karta się zwija. */
@Composable
fun CardHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun ExpandChevron(expanded: Boolean) {
    Icon(
        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        contentDescription = if (expanded) "Zwiń" else "Rozwiń",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Liczba z podpisem, bez ramki. */
@Composable
fun StatValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Column(modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tytuł grupy kart (np. „Mobilność", „Siła"). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 12.dp, bottom = 0.dp)
    )
}

/** Znacznik stanu: zrobione = wypełniony akcent z ✓, dziś = obrys akcentem, czeka = neutralny obrys. */
@Composable
fun DoneMark(done: Boolean, modifier: Modifier = Modifier, active: Boolean = false) {
    val shape = CircleShape
    val base = modifier.size(24.dp).clip(shape)
    when {
        done -> Box(base.background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Zrobione",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        active -> Box(base.border(2.dp, MaterialTheme.colorScheme.primary, shape))
        else -> Box(base.border(1.5.dp, MaterialTheme.colorScheme.outline, shape))
    }
}

/** „1 dzień”, „2 dni”, „5 dni”. */
fun daysLabel(days: Int): String = if (days == 1) "1 dzień" else "$days dni"

/** „1 seria”, „3 serie”, „12 serii”. */
fun setsLabel(sets: Int): String = when {
    sets == 1 -> "1 seria"
    sets % 10 in 2..4 && sets % 100 !in 12..14 -> "$sets serie"
    else -> "$sets serii"
}

/** Cienki pasek postępu bez kropki na końcu. */
@Composable
fun ThinProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp),
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

/** Plan siłowy 1/2 nazywamy w całej aplikacji „A”/„B”. */
fun planName(planDay: Int): String = when (planDay) {
    1 -> "A"
    2 -> "B"
    else -> "$planDay"
}
