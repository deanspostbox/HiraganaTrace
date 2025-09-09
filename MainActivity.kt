\
package com.example.hiraganatrace

import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HiraganaTraceApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiraganaTraceApp() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        val characters = remember { basicHiragana() }
        var index by remember { mutableStateOf(0) }
        var strokeWidth by remember { mutableStateOf(16f) }
        var showGrid by remember { mutableStateOf(true) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Hiragana Trace", fontWeight = FontWeight.SemiBold) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (char, romaji) = characters[index]
                Text(
                    text = "${char}  ·  ${romaji}",
                    fontSize = 28.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(Modifier.background(Color(0xFFF8F8F8))) {
                        TraceCanvas(
                            targetChar = char,
                            hintAlpha = 0.22f,
                            strokeWidth = strokeWidth,
                            showGrid = showGrid
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { index = (index - 1 + characters.size) % characters.size }) {
                        Text("Previous")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var slider by remember { mutableStateOf(strokeWidth) }
                        Slider(
                            value = slider,
                            onValueChange = { slider = it },
                            valueRange = 6f..32f,
                            steps = 0,
                            modifier = Modifier.width(160.dp)
                        )
                        LaunchedEffect(slider) { strokeWidth = slider }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Grid")
                        Switch(checked = showGrid, onCheckedChange = { showGrid = it })
                    }
                    OutlinedButton(onClick = { index = (index + 1) % characters.size }) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
fun TraceCanvas(
    targetChar: String,
    hintAlpha: Float,
    strokeWidth: Float,
    showGrid: Boolean
) {
    var paths by remember { mutableStateOf(mutableListOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Box(Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(strokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            currentPath?.let { p -> paths.add(p) }
                            currentPath = null
                        },
                        onDragCancel = { currentPath = null }
                    )
                }
        ) {
            if (showGrid) drawPracticeGrid()
            drawTargetGlyph(targetChar, hintAlpha)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            for (p in paths) drawPath(p, Color(0xFF1B8A5A), stroke)
            currentPath?.let { drawPath(it, Color(0xFF1B8A5A), stroke) }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(onClick = { if (paths.isNotEmpty()) paths.removeLast() }) { Text("Undo") }
            FilledTonalButton(onClick = { paths = mutableListOf() }) { Text("Clear") }
        }
    }
}

private fun DrawScope.drawTargetGlyph(char: String, hintAlpha: Float) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val minDim = minOf(canvasWidth, canvasHeight)
    val textSize = minDim * 0.78f
    val x = canvasWidth / 2f
    val y = canvasHeight / 2f + textSize * 0.33f

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.textSize = textSize
            color = android.graphics.Color.BLACK
            alpha = (hintAlpha * 255).toInt().coerceIn(0, 255)
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        drawText(char, x, y, paint)
    }
}

private fun DrawScope.drawPracticeGrid() {
    val stroke = Stroke(width = 2f)
    drawRect(color = Color(0xFFE0E0E0), size = size, style = stroke)
    drawLine(Color(0xFFE0E0E0), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke.width)
    drawLine(Color(0xFFE0E0E0), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke.width)
    drawLine(Color(0xFFF0F0F0), Offset(0f, 0f), Offset(size.width, size.height), 1f)
    drawLine(Color(0xFFF0F0F0), Offset(size.width, 0f), Offset(0f, size.height), 1f)
}

fun basicHiragana(): List<Pair<String, String>> = listOf(
    "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
    "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
    "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
    "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
    "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
    "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
    "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
    "や" to "ya", "ゆ" to "yu", "よ" to "yo",
    "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
    "わ" to "wa", "を" to "wo",
    "ん" to "n"
)
