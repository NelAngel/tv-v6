package com.lygone.smarttvsimulator

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ---------- Tokens ---------- */
private val Void = Color(0xFF07060A)
private val Ember = Color(0xFFFF3B63)
private val Moon = Color(0xFFFFD9C7)
private val Angel = Color(0xFF5EE0F0)   // color de foco
private val Glass = Color(0x8C120C14)
private val Dim = Color(0xFFB9A9AE)
private val Txt = Color(0xFFF6EFEE)

private fun t(size: Int, w: FontWeight = FontWeight.Normal, c: Color = Txt) =
    TextStyle(fontSize = size.sp, fontWeight = w, color = c, fontFamily = FontFamily.SansSerif)

/* ---------- Modelo ---------- */
data class TvApp(val label: String, val pkg: String, val intent: Intent, val icon: ImageBitmap?)
private data class Event(val title: String, val tag: String, val desc: String, val time: String, val color: Color)

private val events = listOf(
    Event("Liga 1 y Selección", "En agenda", "Partidos en vivo, previa y tabla de posiciones.", "20:00 hrs", Ember),
    Event("Torneos CONMEBOL y UEFA", "Estreno", "Cruces de fase final y partidos estelares.", "15:00 hrs", Angel),
    Event("Nueva temporada", "Nueva temporada", "Nuevos episodios disponibles para ver hoy.", "Hoy", Color(0xFFFFB347)),
    Event("Estrenos en streaming", "Cartelera", "Nuevos episodios y lanzamientos en streaming.", "Esta semana", Color(0xFFB98BFF)),
)

fun loadApps(ctx: Context): List<TvApp> {
    val pm = ctx.packageManager
    fun q(cat: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
    val list = q(Intent.CATEGORY_LEANBACK_LAUNCHER).ifEmpty { q(Intent.CATEGORY_LAUNCHER) }
    return list.map { it.activityInfo.packageName to it }
        .distinctBy { it.first }
        .filter { it.first != ctx.packageName }
        .mapNotNull { (pkg, r) ->
            val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            val icon = runCatching { r.loadIcon(pm).toBitmap(128, 128).asImageBitmap() }.getOrNull()
            TvApp(r.loadLabel(pm).toString(), pkg, intent, icon)
        }.sortedBy { it.label.lowercase() }
}

/* ---------- Foco de TV: escala + aro cian ---------- */
@Composable
private fun Modifier.tvFocus(shape: Shape, onFocus: () -> Unit = {}, onClick: () -> Unit = {}): Modifier {
    var focused by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (focused) 1.08f else 1f, label = "focusScale")
    return this
        .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
        .zIndex(if (focused) 1f else 0f)
        .graphicsLayer { scaleX = s; scaleY = s }
        .border(if (focused) 2.dp else 0.dp, Angel, shape)
        .clip(shape)
        .clickable(onClick = onClick)
}

/* ---------- Pantalla ---------- */
@Composable
fun LauncherApp(apps: List<TvApp>, onOpen: (TvApp) -> Unit) {
    var sel by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Void)) {
        Image(painterResource(R.drawable.launcher_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Void.copy(.92f), .38f to Void.copy(.55f), .62f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.45f to Color.Transparent, 1f to Void.copy(.95f))))

        Column(Modifier.fillMaxSize()) {
            TopBar()
            Row(Modifier.weight(1f).padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Hero(events[sel], Modifier.weight(1f))
                MiniPlayer()
            }
            Section("Cartelera") {
                Row(Modifier.padding(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    events.forEachIndexed { i, e ->
                        val shape = RoundedCornerShape(10.dp)
                        Box(
                            Modifier.width(210.dp).height(75.dp)
                                .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                                .tvFocus(shape, onFocus = { sel = i })
                                .background(Glass)
                        ) {
                            Box(Modifier.width(3.dp).fillMaxHeight().background(e.color))
                            Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 10.dp)) {
                                BasicText(e.title, style = t(15, FontWeight.SemiBold), maxLines = 2)
                                BasicText(e.time, style = t(12, c = Dim))
                            }
                        }
                    }
                }
            }
            Section("Tus apps") {
                if (apps.isEmpty()) {
                    BasicText("No se encontraron aplicaciones compatibles.", Modifier.padding(horizontal = 48.dp), style = t(14, c = Dim))
                } else LazyRow(contentPadding = PaddingValues(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(apps, key = { it.pkg }) { a -> AppTile(a) { onOpen(a) } }
                }
            }
            Ticker()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 14.dp)) {
        BasicText(title, Modifier.padding(start = 48.dp, bottom = 8.dp), style = t(14, FontWeight.SemiBold, Dim))
        content()
    }
}

@Composable
private fun TopBar() {
    var now by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val f = SimpleDateFormat("HH:mm", Locale("es", "PE"))
        while (true) { now = f.format(Date()); delay(15_000) }
    }
    Row(Modifier.fillMaxWidth().padding(start = 48.dp, end = 48.dp, top = 27.dp), verticalAlignment = Alignment.CenterVertically) {
        Row {
            BasicText("LYGANE", style = t(17, FontWeight.ExtraBold))
            BasicText(" TV", style = t(17, FontWeight.ExtraBold, Ember))
        }
        Spacer(Modifier.width(24.dp))
        listOf("Inicio", "Apps", "Favoritos", "Cartelera").forEachIndexed { i, s ->
            Box(Modifier.padding(end = 4.dp).tvFocus(CircleShape).background(if (i == 0) Glass else Color.Transparent).padding(horizontal = 14.dp, vertical = 6.dp)) {
                BasicText(s, style = t(13, FontWeight.SemiBold, if (i == 0) Txt else Dim))
            }
        }
        Spacer(Modifier.weight(1f))
        Chip("☀ 22° Despejado"); Spacer(Modifier.width(8.dp)); Chip("Wi‑Fi"); Spacer(Modifier.width(8.dp)); Chip("Bluetooth")
        Spacer(Modifier.width(18.dp))
        BasicText(now, style = t(22, FontWeight.ExtraBold))
    }
}

@Composable
private fun Chip(text: String) =
    Box(Modifier.tvFocus(CircleShape).background(Glass).padding(horizontal = 10.dp, vertical = 5.dp)) { BasicText(text, style = t(13)) }

@Composable
private fun Hero(e: Event, modifier: Modifier) = Column(modifier) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Ember).padding(horizontal = 9.dp, vertical = 3.dp)) {
        BasicText(e.tag, style = t(13, FontWeight.SemiBold, Void))
    }
    Spacer(Modifier.height(10.dp))
    BasicText(e.title, style = t(52, FontWeight.ExtraBold).copy(lineHeight = 52.sp), maxLines = 2)
    Spacer(Modifier.height(8.dp))
    BasicText(e.desc, Modifier.widthIn(max = 330.dp), style = t(16, c = Moon))
    Spacer(Modifier.height(14.dp))
    Box(Modifier.tvFocus(RoundedCornerShape(7.dp)).background(Txt).padding(horizontal = 15.dp, vertical = 7.dp)) {
        BasicText("▶  OK para abrir", style = t(14, FontWeight.SemiBold, Void))
    }
}

@Composable
private fun MiniPlayer() = Row(
    Modifier.width(215.dp).clip(RoundedCornerShape(12.dp)).background(Glass).padding(11.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Ember, Color(0xFF4B1030)))))
    Spacer(Modifier.width(10.dp))
    Column {
        BasicText("Nada sonando", style = t(14, FontWeight.SemiBold))
        BasicText("Activa notificaciones", style = t(12, c = Dim))
    }
}

@Composable
private fun AppTile(app: TvApp, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    Column(Modifier.width(105.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(59.dp)
                .onFocusChanged { f = it.isFocused }
                .tvFocus(RoundedCornerShape(10.dp), onClick = onClick)
                .background(Glass),
            contentAlignment = Alignment.Center
        ) {
            app.icon?.let { Image(it, app.label, Modifier.size(38.dp)) }
        }
        Spacer(Modifier.height(5.dp))
        BasicText(app.label, style = t(11, c = if (f) Txt else Dim), maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Ticker() = Row(
    Modifier.fillMaxWidth().height(32.dp).background(Void.copy(.9f)).padding(horizontal = 48.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    BasicText("NOTICIAS PERÚ", style = t(12, FontWeight.Bold, Ember))
    Spacer(Modifier.width(12.dp))
    BasicText(
        "Jornada de fútbol: cruces de fase final y partidos estelares  ·  Nuevos episodios disponibles hoy  ·  Torneos CONMEBOL y UEFA esta semana",
        Modifier.basicMarquee(iterations = Int.MAX_VALUE), style = t(13, c = Dim), maxLines = 1
    )
}
