package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.legacyfabric.LegacyFabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.ui.screens.content.download.GameDownloadViewModel
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val versionArtwork = mapOf(
    "26" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/MCV_SPR26Drop_TT_DotNet_Wallpaper_414x414.png",
    "1.21.9" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Minecraft_Fall_Drop_Campaign_Key_Art_DotNet_Downloadable_Wallpaper_414x414.png",
    "1.21" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_TrickyTrials_414x414_01.jpg",
    "1.20" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_TrailsAndTales_414x414_01.jpg",
    "1.19" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_WildUpdate_414x414_01.jpg",
    "1.18" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_CavesAndCliffs2_414x414_01.jpg",
    "1.17" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_CavesAndCliffs1_414x414_01.jpg",
    "1.16" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_NetherUpdate_414x414_01.jpg",
    "1.15" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_BuzzyBeesUpdate_414x414_01.jpg",
    "1.14" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_VillageandPillage_414x414_01.jpg",
    "1.13" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_UpdateAquatic_414x414_01.jpg",
    "1.12" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_WorldofColor_414x414_01.jpg",
    "1.8" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_BeachCabin_414x414_01.jpg",
    "1.7" to "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_Mining_414x414_01.jpg"
)

private fun getArtworkUrl(version: String): String {
    val parts = version.split(".")
    val majorNum = parts.firstOrNull()?.toIntOrNull() ?: 1
    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val major = parts.take(2).joinToString(".")
    if (majorNum >= 26) return versionArtwork["26"]!!
    if (major == "1.21" && patch >= 9) return versionArtwork["1.21.9"]!!
    return versionArtwork[major]
        ?: "https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/key-art/Wallpapers_MinecraftGame-Carousel-H-0_TrickyTrials_414x414_01.jpg"
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1_000_000 -> "${"%.1f".format(bytesPerSec / 1_000_000.0)} MB/s"
        bytesPerSec >= 1_000 -> "${"%.1f".format(bytesPerSec / 1_000.0)} KB/s"
        else -> "$bytesPerSec B/s"
    }
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
        val installedVersions = VersionsManager.versions
        var selectedIndex by remember { mutableStateOf(0) }
        var showLoaderSelection by remember { mutableStateOf(false) }
        var versionToDelete by remember { mutableStateOf<Version?>(null) }
        val velocityTracker = remember { VelocityTracker() }
        var dragAccumulator by remember { mutableStateOf(0f) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val downloadViewModel: GameDownloadViewModel = viewModel()

        val tasks by (downloadViewModel.installer?.tasksFlow
            ?.collectAsStateWithLifecycle(emptyList())
            ?: remember { mutableStateOf(emptyList()) })

        val isDownloading = downloadViewModel.installer != null
        val currentTask = tasks.firstOrNull()
        val downloadProgress = currentTask?.task?.currentProgress ?: -1f
        val downloadSpeed = currentTask?.task?.currentRateBytesPerSec ?: 0L
        val downloadTitle = currentTask?.title ?: ""

        // Delete confirmation dialog
        versionToDelete?.let { version ->
            AlertDialog(
                onDismissRequest = { versionToDelete = null },
                title = { Text("Delete Version") },
                text = { Text("Are you sure you want to delete "${version.getVersionName()}"? This will permanently remove all files.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            VersionsManager.deleteVersion(version)
                            versionToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { versionToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

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
                                    },
                                    onDelete = {
                                        versionToDelete = version
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

                        // Artwork
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = artworkUrl,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "artwork",
                                modifier = Modifier.fillMaxSize()
                            ) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (showLoaderSelection)
                                                Modifier.blur(8.dp)
                                            else Modifier
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            AnimatedContent(
                                targetState = currentVersion?.version?.id ?: "",
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "versionName",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            ) { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Loader selection overlay
                            if (showLoaderSelection && !isDownloading) {
                                var isLoadingLoader by remember { mutableStateOf(false) }
                                var loaderError by remember { mutableStateOf<String?>(null) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingLoader) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(color = Color.White)
                                            Text("Fetching loader versions...", color = Color.White)
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Select loader for ${currentVersion?.version?.id}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            loaderError?.let {
                                                Text(it, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                            }

                                            // Vanilla
                                            OutlinedButton(
                                                onClick = {
                                                    val versionId = currentVersion?.version?.id ?: return@OutlinedButton
                                                    downloadViewModel.install(
                                                        context,
                                                        GameDownloadInfo(
                                                            gameVersion = versionId,
                                                            customVersionName = versionId
                                                        )
                                                    )
                                                    showLoaderSelection = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) { Text("Vanilla") }

                                            // Fabric
                                            OutlinedButton(
                                                onClick = {
                                                    val versionId = currentVersion?.version?.id ?: return@OutlinedButton
                                                    isLoadingLoader = true
                                                    loaderError = null
                                                    scope.launch {
                                                        val fabricVersions = FabricVersions.fetchFabricLoaderList(versionId)
                                                        isLoadingLoader = false
                                                        if (fabricVersions.isNullOrEmpty()) {
                                                            loaderError = "Fabric not available for $versionId"
                                                        } else {
                                                            downloadViewModel.install(
                                                                context,
                                                                GameDownloadInfo(
                                                                    gameVersion = versionId,
                                                                    customVersionName = "$versionId-Fabric",
                                                                    fabric = fabricVersions.first()
                                                                )
                                                            )
                                                            showLoaderSelection = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) { Text("Fabric") }

                                            // Forge
                                            OutlinedButton(
                                                onClick = {
                                                    val versionId = currentVersion?.version?.id ?: return@OutlinedButton
                                                    isLoadingLoader = true
                                                    loaderError = null
                                                    scope.launch {
                                                        val forgeVersions = ForgeVersions.fetchForgeList(versionId)
                                                        isLoadingLoader = false
                                                        if (forgeVersions.isNullOrEmpty()) {
                                                            loaderError = "Forge not available for $versionId"
                                                        } else {
                                                            downloadViewModel.install(
                                                                context,
                                                                GameDownloadInfo(
                                                                    gameVersion = versionId,
                                                                    customVersionName = "$versionId-Forge",
                                                                    forge = forgeVersions.first()
                                                                )
                                                            )
                                                            showLoaderSelection = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) { Text("Forge") }

                                            // NeoForge
                                            OutlinedButton(
                                                onClick = {
                                                    val versionId = currentVersion?.version?.id ?: return@OutlinedButton
                                                    isLoadingLoader = true
                                                    loaderError = null
                                                    scope.launch {
                                                        val neoForgeVersions = NeoForgeVersions.fetchNeoForgeList(gameVersion = versionId)
                                                        isLoadingLoader = false
                                                        if (neoForgeVersions.isNullOrEmpty()) {
                                                            loaderError = "NeoForge not available for $versionId"
                                                        } else {
                                                            downloadViewModel.install(
                                                                context,
                                                                GameDownloadInfo(
                                                                    gameVersion = versionId,
                                                                    customVersionName = "$versionId-NeoForge",
                                                                    neoforge = neoForgeVersions.first()
                                                                )
                                                            )
                                                            showLoaderSelection = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) { Text("NeoForge") }

                                            // Quilt
                                            OutlinedButton(
                                                onClick = {
                                                    val versionId = currentVersion?.version?.id ?: return@OutlinedButton
                                                    isLoadingLoader = true
                                                    loaderError = null
                                                    scope.launch {
                                                        val quiltVersions = QuiltVersions.fetchQuiltLoaderList(versionId)
                                                        isLoadingLoader = false
                                                        if (quiltVersions.isNullOrEmpty()) {
                                                            loaderError = "Quilt not available for $versionId"
                                                        } else {
                                                            downloadViewModel.install(
                                                                context,
                                                                GameDownloadInfo(
                                                                    gameVersion = versionId,
                                                                    customVersionName = "$versionId-Quilt",
                                                                    quilt = quiltVersions.first()
                                                                )
                                                            )
                                                            showLoaderSelection = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) { Text("Quilt") }

                                            // LegacyFabric
                                            OutlinedButton(
                                                onClick = {
                                                    val versionId = currentVersion?.version?.id ?: return@OutlinedButton
                                                    isLoadingLoader = true
                                                    loaderError = null
                                                    scope.launch {
                                                        val legacyFabricVersions = LegacyFabricVersions.fetchFabricLoaderList(versionId)
                                                        isLoadingLoader = false
                                                        if (legacyFabricVersions.isNullOrEmpty()) {
                                                            loaderError = "LegacyFabric not available for $versionId"
                                                        } else {
                                                            downloadViewModel.install(
                                                                context,
                                                                GameDownloadInfo(
                                                                    gameVersion = versionId,
                                                                    customVersionName = "$versionId-LegacyFabric",
                                                                    legacyFabric = legacyFabricVersions.first()
                                                                )
                                                            )
                                                            showLoaderSelection = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) { Text("LegacyFabric") }

                                            TextButton(onClick = { showLoaderSelection = false }) {
                                                Text("Cancel", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Version nav row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .pointerInput(releaseVersions.size) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            dragAccumulator = 0f
                                            velocityTracker.resetTracking()
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            velocityTracker.addPosition(
                                                change.uptimeMillis,
                                                change.position
                                            )
                                            dragAccumulator -= dragAmount
                                            val threshold = 40f
                                            if (abs(dragAccumulator) >= threshold) {
                                                val steps = (dragAccumulator / threshold).roundToInt()
                                                selectedIndex = (selectedIndex + steps)
                                                    .coerceIn(0, releaseVersions.size - 1)
                                                dragAccumulator -= steps * threshold
                                            }
                                        },
                                        onDragEnd = {
                                            val velocity = velocityTracker.calculateVelocity()
                                            val extraSteps = (velocity.x / -2000f).roundToInt()
                                                .coerceIn(-10, 10)
                                            selectedIndex = (selectedIndex + extraSteps)
                                                .coerceIn(0, releaseVersions.size - 1)
                                        }
                                    )
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (selectedIndex > 0) selectedIndex-- },
                                enabled = selectedIndex > 0
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_left_rounded),
                                    contentDescription = "Previous"
                                )
                            }

                            AnimatedContent(
                                targetState = currentVersion?.version?.id ?: "",
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "navVersion"
                            ) { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

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

                        // Download button or progress
                        if (isDownloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = downloadTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (downloadSpeed > 0L) {
                                            Text(
                                                text = formatSpeed(downloadSpeed),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (downloadProgress >= 0f) {
                                            Text(
                                                text = "${(downloadProgress * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (downloadProgress < 0f) {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { downloadViewModel.cancel() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Button(
                                onClick = { showLoaderSelection = true },
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
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

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
    onClick: () -> Unit,
    onDelete: () -> Unit
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
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_outlined),
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
