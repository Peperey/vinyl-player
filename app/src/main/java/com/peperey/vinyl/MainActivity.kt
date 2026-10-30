package com.peperey.vinyl

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val audioPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    var hasAccess by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAccess = results[audioPermission] == true
    }

    LaunchedEffect(Unit) {
        if (!hasAccess) {
            val perms = if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(audioPermission, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                arrayOf(audioPermission)
            }
            launcher.launch(perms)
        }
    }

    val tracks = remember(hasAccess) {
        if (hasAccess) loadTracks(context) else emptyList()
    }

    if (hasAccess) {
        PlayerScreen(tracks)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Necesito permiso para leer la música del teléfono y la SD.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch(arrayOf(audioPermission)) }) {
                Text("Dar permiso")
            }
        }
    }
}

@Composable
fun PlayerScreen(tracks: List<Track>) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("Ninguna canción") }
    var artist by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(-1) }
    var showCassette by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Conecta con el servicio de música en segundo plano
    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({ controller = future.get() }, ContextCompat.getMainExecutor(context))
        onDispose { MediaController.releaseFuture(future) }
    }

    // Escucha cambios de reproducción
    DisposableEffect(controller) {
        val c = controller ?: return@DisposableEffect onDispose {}
        isPlaying = c.isPlaying
        title = c.mediaMetadata.title?.toString() ?: "Ninguna canción"
        artist = c.mediaMetadata.artist?.toString() ?: ""
        currentIndex = c.currentMediaItemIndex

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                title = mediaMetadata.title?.toString() ?: "Ninguna canción"
                artist = mediaMetadata.artist?.toString() ?: ""
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = c.currentMediaItemIndex
            }
        }
        c.addListener(listener)
        onDispose { c.removeListener(listener) }
    }

    // Calcula el avance de la canción para mover la cinta del casete
    LaunchedEffect(controller) {
        val c = controller ?: return@LaunchedEffect
        while (isActive) {
            val dur = c.duration
            progress = if (dur > 0) {
                (c.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
            } else {
                0f
            }
            delay(250)
        }
    }

    // Carga la lista de canciones una sola vez en el servicio
    LaunchedEffect(controller, tracks) {
        val c = controller ?: return@LaunchedEffect
        if (c.mediaItemCount == 0 && tracks.isNotEmpty()) {
            c.setMediaItems(tracks.map { it.mediaItem })
            c.prepare()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showCassette) {
            CassettePlayer(
                isPlaying = isPlaying,
                label = title,
                progress = progress,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        } else {
            VinylPlayer(
                isPlaying = isPlaying,
                label = title,
                modifier = Modifier.fillMaxWidth(0.7f)
            )
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { showCassette = !showCassette }) {
            Text(if (showCassette) "Ver disco" else "Ver casete")
        }

        Spacer(Modifier.height(12.dp))
        Text(title)
        Text(artist)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { controller?.seekToPrevious() }) { Text("Anterior") }
            Button(onClick = {
                val c = controller ?: return@Button
                if (c.isPlaying) c.pause() else c.play()
            }) {
                Text(if (isPlaying) "Pausar" else "Reproducir")
            }
            Button(onClick = { controller?.seekToNext() }) { Text("Siguiente") }
        }
        Spacer(Modifier.height(12.dp))

        if (tracks.isEmpty()) {
            Text("No encontré canciones en el teléfono ni en la SD.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(tracks) { index, track ->
                    Text(
                        text = (if (index == currentIndex) "▶ " else "") +
                            "${track.title} - ${track.artist}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                controller?.let { c ->
                                    c.seekToDefaultPosition(index)
                                    c.play()
                                }
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
