package com.smartlighting.app.ui.components

import android.graphics.*
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory

object StreetlightIcon {

    fun create(color: Int, statusLabel: String, bright: Boolean = false): BitmapDescriptor {
        val iconW = 72
        val iconH = 96
        val totalW = iconW + 20
        val totalH = iconH + 30

        val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val cx = totalW / 2f
        val cy = iconH - 10f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── bright glow rings ──
        if (bright) {
            p.color = android.graphics.Color.argb(80,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color))
            c.drawCircle(cx, cy - 34f, 28f, p)
            p.color = android.graphics.Color.argb(40,
                android.graphics.Color.red(color),
                android.graphics.Color.green(color),
                android.graphics.Color.blue(color))
            c.drawCircle(cx, cy - 34f, 35f, p)
        }

        // ── ground shadow ──
        p.style = Paint.Style.FILL
        p.color = android.graphics.Color.argb(30, 0, 0, 0)
        c.drawOval(cx - 8f, cy + 2f, cx + 8f, cy + 8f, p)

        // ── pole ──
        val poleColor = if (bright) color else android.graphics.Color.rgb(80, 90, 100)
        p.color = poleColor
        c.drawRoundRect(cx - 2.5f, cy - 42f, cx + 2.5f, cy - 5f, 2f, 2f, p)

        // ── lamp housing ──
        val housingPath = Path().apply {
            moveTo(cx - 9f, cy - 36f)
            lineTo(cx - 10f, cy - 46f)
            lineTo(cx - 7f, cy - 49f)
            lineTo(cx + 7f, cy - 49f)
            lineTo(cx + 10f, cy - 46f)
            lineTo(cx + 9f, cy - 36f)
            close()
        }
        p.shader = LinearGradient(cx, cy - 49f, cx, cy - 36f,
            android.graphics.Color.rgb(60, 70, 80), android.graphics.Color.rgb(100, 110, 120),
            Shader.TileMode.CLAMP)
        c.drawPath(housingPath, p)
        p.shader = null

        // ── bulb glow ──
        val glowColors = if (bright)
            intArrayOf(
                android.graphics.Color.argb(220,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)),
                android.graphics.Color.argb(60,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)))
        else
            intArrayOf(
                android.graphics.Color.argb(150,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)),
                android.graphics.Color.argb(30,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)))
        p.shader = RadialGradient(cx, cy - 41f, 18f, glowColors, null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy - 41f, 18f, p)
        p.shader = null

        // ── bulb ──
        p.style = Paint.Style.FILL
        p.color = if (bright) android.graphics.Color.WHITE else color
        c.drawCircle(cx, cy - 41f, 5f, p)
        // highlight dot
        p.color = android.graphics.Color.argb(if (bright) 255 else 150, 255, 255, 255)
        c.drawCircle(cx - 1.5f, cy - 43f, 2f, p)

        // ── arm ──
        p.color = poleColor
        p.strokeWidth = 2.5f
        p.style = Paint.Style.STROKE
        c.drawLine(cx + 2f, cy - 45f, cx + 7f, cy - 47f, p)
        p.style = Paint.Style.FILL
        p.strokeWidth = 0f

        // ── cap ──
        p.color = android.graphics.Color.rgb(100, 110, 120)
        c.drawCircle(cx, cy - 50f, 2f, p)

        // ── Status label below icon ──
        val labelColor = when (statusLabel) {
            "在线" -> android.graphics.Color.rgb(22, 160, 133)
            "异常" -> android.graphics.Color.rgb(245, 158, 11)
            else -> android.graphics.Color.rgb(148, 163, 184)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = labelColor
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textW = textPaint.measureText(statusLabel) + 12f

        // pill background
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG)
        pillBg.color = android.graphics.Color.argb(180, 0, 0, 0)
        pillBg.style = Paint.Style.FILL
        c.drawRoundRect(cx - textW / 2, cy + 10f, cx + textW / 2, cy + 28f, 8f, 8f, pillBg)

        // pill border
        val pillBd = Paint(Paint.ANTI_ALIAS_FLAG)
        pillBd.color = labelColor
        pillBd.style = Paint.Style.STROKE
        pillBd.strokeWidth = 1.2f
        c.drawRoundRect(cx - textW / 2, cy + 10f, cx + textW / 2, cy + 28f, 8f, 8f, pillBd)

        // text
        c.drawText(statusLabel, cx, cy + 24f, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun statusLabel(status: Int): String = when (status) {
        1 -> "在线"; 2 -> "离线"; 3 -> "异常"; else -> "停用"
    }
}
