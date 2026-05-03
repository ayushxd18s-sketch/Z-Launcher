package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage

// Minecraft version artwork URLs
private val versionArtwork = mapOf(
    "1.21" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/Tricky-Trials-Key-Art.jpg",
    "1.20" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/trails-and-tales-key-art.jpg",
    "1.19" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/wild-update-key-art.jpg",
    "1.18" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/caves-cliffs-key-art.jpg",
    "1.17" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/caves-cliffs-key-art.jpg",
    "1.16" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/nether-update-key-art.jpg",
    "1.15" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/buzzy-bees-key-art.jpg",
    "1.14" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/village-and-pillage-key-art.jpg",
    "1.13" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/update-aquatic-key-art.jpg",
    "1.12" to "https://www.minecraft.net/content/dam/games/minecraft/screenshots/world-of-color-key-art.jpg"
)

private fun getArtworkUrl(version: String): String? {
    val major = version.split(".").take(2).joinToString(".")
    return versionArtwork[major]
}

@Composable
fun GameLibraryButton(
    onClick: () -> Unit,
    isOpen: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isOpen) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.img_chest),
                contentDescription = "Library",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Library",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun GameLibraryPanel(
    isOpen: Boolean,
    onVersionSelect: (Version) -> Unit,
    onClose: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "panelScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "panelAlpha"
    )

    if (scale > 0f) {
        val allVersions by MinecraftVersions.allVersions.collectAsStateWithLifecycle()
        val releaseVersions = remember(allVersions) {
            allVersions.filter { it.type == MinecraftVersion.Type.Release }
        }
        var selectedIndex by remember { mutableStateOf(0) }
        val installedVersions = VersionsManager.versions

        LaunchedEffect(Unit) {
            MinecraftVersions.refreshVersions()
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                // Left - Installed versions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Installed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    if (installedVersions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.versions_manage_no_versions),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(installedVersions) { version ->
                                VersionLibraryItem(
                                    version = version,
                                    onClick = {
                                        onVersionSelect(version)
                                        onClose()
                                    }
                                )
                            }
                        }
                    }
                }

                VerticalDivider()

                // Right - Download browser
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    if (releaseVersions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val currentVersion = releaseVersions.getOrNull(selectedIndex)
                        val artworkUrl = currentVersion?.version?.id?.let { getArtworkUrl(it) }

                        // Version artwork hero
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (artworkUrl != null) {
                                AsyncImage(
                                    model = artworkUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }

                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            )

                            // Version name overlay
                            Text(
                                text = currentVersion?.version?.id ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            )
                        }

                        // Arrow navigation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedIndex > 0) selectedIndex--
                                },
                                enabled = selectedIndex > 0
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                    contentDescription = "Previous"
                                )
                            }

                            Text(
                                text = currentVersion?.version?.id ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = {
                                    if (selectedIndex < releaseVersions.size - 1) selectedIndex++
                                },
                                enabled = selectedIndex < releaseVersions.size - 1
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right_rounded),
                                    contentDescription = "Next"
                                )
                            }
                        }

                        // Download button
                        Button(
                            onClick = { /* navigate to download */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download_2_outlined),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Download ${currentVersion?.version?.id ?: ""}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close"
                )
            }
        }
    }
}

@Composable
private fun VersionLibraryItem(
    version: Version,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VersionIconImage(
            version = version,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = version.getVersionName(),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
            if (version.isValid()) {
                Text(
                    text = version.getVersionSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
