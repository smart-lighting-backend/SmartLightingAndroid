package com.smartlighting.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartlighting.app.ui.theme.*

@Composable
fun SimpleLineChart(
    current: List<Double>,
    lastWeek: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    Column(modifier = modifier) {
        // ── 图例 ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(Cyan, "本日能耗")
            Spacer(Modifier.width(24.dp))
            LegendItem(Color(0xFF708496), "上周同期", dashed = true)
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (current.isEmpty()) return@Canvas
            val maxVal = (current.maxOrNull() ?: 1.0).coerceAtLeast(lastWeek.maxOrNull() ?: 1.0) * 1.2
            val w = size.width; val h = size.height
            val step = w / (current.size - 1).coerceAtLeast(1)

            // Grid
            for (i in 0..4) {
                val y = h * i / 4
                drawLine(Color(0x22E2E8F0), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Last week dashed
            if (lastWeek.size >= 2) {
                val path = Path()
                lastWeek.forEachIndexed { i, v -> val x = i * step; val y = h - (v / maxVal * h).toFloat(); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                drawPath(path, Color(0x99708496), style = Stroke(width = 2f, cap = StrokeCap.Round))
            }
            // Current
            if (current.size >= 2) {
                val path = Path()
                current.forEachIndexed { i, v -> val x = i * step; val y = h - (v / maxVal * h).toFloat(); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                drawPath(path, Cyan, style = Stroke(width = 3f, cap = StrokeCap.Round))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.filterIndexed { i, _ -> i % 4 == 0 }.forEach { Text(it, fontSize = 9.sp, color = TextMuted) }
        }
    }
}

@Composable
fun SimplePieChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier, height: Dp = 180.dp) {
    val colors = listOf(Green, Amber, Red, Color(0xFF6B7F93), PrimaryBlue)
    val legendColors = listOf(Green, Amber, Red, Color(0xFF6B7F93), PrimaryBlue)
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val total = data.sumOf { it.second }
            if (total <= 0) return@Canvas
            val sz = size.minDimension * 0.6f
            var sa = -90f
            data.forEachIndexed { i, (_, v) ->
                val sw = (v / total * 360).toFloat()
                drawArc(colors[i % colors.size], sa, sw, false, Offset((size.width - sz) / 2, (size.height - sz) / 2), Size(sz, sz), style = Stroke(width = sz * 0.18f, cap = StrokeCap.Butt))
                sa += sw
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEachIndexed { i, (label, v) ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Canvas(Modifier.size(8.dp)) { drawCircle(legendColors[i % legendColors.size], 4.dp.toPx()) }
                    Spacer(Modifier.width(3.dp))
                    Text("$label", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier, height: Dp = 160.dp) {
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (data.isEmpty()) return@Canvas
            val mv = data.maxOf { it.second }.coerceAtLeast(1.0) * 1.3
            val bw = size.width / data.size * 0.5f; val g = size.width / data.size * 0.5f
            data.forEachIndexed { i, (_, v) ->
                val bh = (v / mv * size.height).toFloat()
                drawRect(PrimaryBlue, Offset(i * (bw + g), size.height - bh), Size(bw, bh))
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        String.format("%.0f", v),
                        i * (bw + g) + bw / 2 - 10, size.height - bh - 6,
                        android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#8CBEDC"); textSize = 22f; textAlign = android.graphics.Paint.Align.LEFT }
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            data.forEach { (label, _) ->
                Box(Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                    Text(label, fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(20.dp, 3.dp)) {
            if (dashed) {
                val dashLen = 5f; val gapLen = 3f; var x = 0f
                while (x < size.width) {
                    drawLine(color, Offset(x, size.height / 2), Offset((x + dashLen).coerceAtMost(size.width), size.height / 2), strokeWidth = 2f)
                    x += dashLen + gapLen
                }
            } else {
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}
