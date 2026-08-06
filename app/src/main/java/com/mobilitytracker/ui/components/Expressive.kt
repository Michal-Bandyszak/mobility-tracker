package com.mobilitytracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
fun ExpressiveHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Slightly more breathing room
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge, // Larger, softer subtitle
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExpressivePanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), // Slightly more transparent for glass effect
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp), // Ethereal Minimal: large, soft squircles
        color = containerColor,
        tonalElevation = 0.dp, // Flat, relying on color contrast
        shadowElevation = 0.dp, // No shadow for clean glass effect
        content = content
    )
}

@Composable
fun StatusPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = color.copy(alpha = 0.18f)
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, color.copy(alpha = 0.14f), shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.78f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
