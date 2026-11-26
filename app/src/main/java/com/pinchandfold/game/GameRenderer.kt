package com.pinchandfold.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Renders the paper-craft tabletop ring, origami wrestlers, elastic bands,
 * touch rings, comic popups, confetti, and paper-tag HUD.
 */
class GameRenderer {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(22f, 16f), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val path = Path()
    private val tmpRect = RectF()

    private var cachedMatWidth = 0f
    private var cachedMatHeight = 0f
    private var bgShader: Shader? = null
    private var woodShader: Shader? = null

    fun render(canvas: Canvas, engine: GameEngine) {
        val w = engine.width
        val h = engine.height
        if (w <= 0f || h <= 0f) return

        ensureShaders(engine)

        // 1. Dark charcoal mat background
        fillPaint.shader = bgShader
        canvas.drawRect(0f, 0f, w, h, fillPaint)
        fillPaint.shader = null

        canvas.save()
        canvas.translate(engine.shakeX, engine.shakeY)

        // 2. Cardboard & Wood Tabletop Ring
        drawRing(canvas, engine)

        // 3. Elastic Touch Slingshots (under wrestlers' arms)
        if (engine.screenState == ScreenState.PLAYING) {
            drawSlingshot(canvas, engine, engine.slingshotP1)
            if (engine.gameMode == GameMode.TWO_PLAYER) {
                drawSlingshot(canvas, engine, engine.slingshotP2)
            }
        }

        // 4. Origami Wrestlers
        for (wrestler in engine.wrestlers) {
            drawWrestler(canvas, engine, wrestler)
        }

        // 5. Rubber-band Ropes & Corner Posts (drawn over edge)
        drawRopesAndPosts(canvas, engine)

        // 6. Paper Confetti & Comic Starburst Popups
        drawParticles(canvas, engine)
        drawPopups(canvas, engine)

        canvas.restore()

        // 7. Paper-Tag HUD & Overlays
        drawHud(canvas, engine)
        when (engine.screenState) {
            ScreenState.MENU -> drawMenuOverlay(canvas, engine)
            ScreenState.ROUND_OVER -> drawRoundBanner(canvas, engine)
            ScreenState.GAME_OVER -> drawGameOverOverlay(canvas, engine)
            ScreenState.PLAYING -> Unit
        }
    }

    private fun ensureShaders(engine: GameEngine) {
        if (cachedMatWidth == engine.width && cachedMatHeight == engine.height && bgShader != null) return
        cachedMatWidth = engine.width
        cachedMatHeight = engine.height

        bgShader = RadialGradient(
            engine.ringCenterX,
            engine.ringCenterY,
            engine.ringRadius * 1.9f,
            intArrayOf(Color.rgb(42, 49, 58), Color.rgb(25, 29, 34)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        woodShader = RadialGradient(
            engine.ringCenterX,
            engine.ringCenterY,
            engine.ringRadius,
            intArrayOf(Color.rgb(243, 200, 146), Color.rgb(218, 165, 107)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private fun drawRing(canvas: Canvas, engine: GameEngine) {
        val cx = engine.ringCenterX
        val cy = engine.ringCenterY
        val r = engine.ringRadius

        // Drop shadow under ring
        fillPaint.color = Color.argb(120, 0, 0, 0)
        canvas.drawCircle(cx, cy + r * 0.045f, r * 1.09f, fillPaint)

        // Outer corrugated cardboard rim
        fillPaint.color = Color.rgb(168, 118, 72)
        canvas.drawCircle(cx, cy, r * 1.07f, fillPaint)

        // Inner cardboard lip
        fillPaint.color = Color.rgb(198, 144, 92)
        canvas.drawCircle(cx, cy, r * 1.02f, fillPaint)

        // Wood-grain deck
        fillPaint.shader = woodShader
        canvas.drawCircle(cx, cy, r * 0.96f, fillPaint)
        fillPaint.shader = null

        // Subtle inner wood rings
        strokePaint.color = Color.argb(36, 140, 82, 32)
        strokePaint.strokeWidth = 3f
        canvas.drawCircle(cx, cy, r * 0.72f, strokePaint)
        canvas.drawCircle(cx, cy, r * 0.44f, strokePaint)

        // Masking tape strips at cardinal points
        fillPaint.color = Color.argb(170, 245, 232, 204)
        val tapeW = r * 0.11f
        val tapeH = r * 0.16f
        for (i in 0 until 4) {
            canvas.save()
            canvas.rotate(i * 90f, cx, cy)
            canvas.drawRect(cx - tapeW * 0.5f, cy - r * 1.03f, cx + tapeW * 0.5f, cy - r * 1.03f + tapeH, fillPaint)
            canvas.restore()
        }

        // 2P center divider line if in 2P mode
        if (engine.gameMode == GameMode.TWO_PLAYER) {
            dashPaint.color = Color.argb(75, 110, 68, 30)
            dashPaint.strokeWidth = 4f
            canvas.drawLine(cx - r * 0.88f, cy, cx + r * 0.88f, cy, dashPaint)
        }
    }

    private fun drawRopesAndPosts(canvas: Canvas, engine: GameEngine) {
        val cx = engine.ringCenterX
        val cy = engine.ringCenterY
        val r = engine.ringRadius * 0.98f

        // 4 corner posts at 45, 135, 225, 315 degrees
        val postAngles = floatArrayOf(45f, 135f, 225f, 315f)
        val postX = FloatArray(4)
        val postY = FloatArray(4)
        for (i in 0 until 4) {
            val rad = Math.toRadians(postAngles[i].toDouble()).toFloat()
            postX[i] = cx + cos(rad) * r
            postY[i] = cy + sin(rad) * r
        }

        // Draw 4 curved rubber-band rope segments between posts
        // Segment i connects post (i-1+4)%4 to post i, centered around i*90 deg
        for (i in 0 until 4) {
            val prev = (i + 3) % 4
            val midAngle = Math.toRadians((i * 90f).toDouble()).toFloat()
            val flex = engine.ropeFlexes[i].flexOffset
            val ctrlR = r * 1.15f + flex * 1.8f
            val ctrlX = cx + cos(midAngle) * ctrlR
            val ctrlY = cy + sin(midAngle) * ctrlR

            path.reset()
            path.moveTo(postX[prev], postY[prev])
            path.quadTo(ctrlX, ctrlY, postX[i], postY[i])

            strokePaint.color = Color.argb(185, 245, 238, 224)
            strokePaint.strokeWidth = r * 0.022f
            canvas.drawPath(path, strokePaint)

            strokePaint.color = Color.argb(110, 255, 255, 255)
            strokePaint.strokeWidth = r * 0.008f
            canvas.drawPath(path, strokePaint)
        }

        // Draw cardboard corner posts
        val postSize = r * 0.085f
        for (i in 0 until 4) {
            fillPaint.color = Color.argb(90, 0, 0, 0)
            canvas.drawRect(
                postX[i] - postSize * 0.5f + 4f,
                postY[i] - postSize * 0.5f + 6f,
                postX[i] + postSize * 0.5f + 4f,
                postY[i] + postSize * 0.5f + 6f,
                fillPaint
            )
            fillPaint.color = Color.rgb(154, 108, 66)
            canvas.drawRect(
                postX[i] - postSize * 0.5f,
                postY[i] - postSize * 0.5f,
                postX[i] + postSize * 0.5f,
                postY[i] + postSize * 0.5f,
                fillPaint
            )
            fillPaint.color = Color.rgb(194, 146, 98)
            canvas.drawRect(
                postX[i] - postSize * 0.36f,
                postY[i] - postSize * 0.36f,
                postX[i] + postSize * 0.36f,
                postY[i] + postSize * 0.36f,
                fillPaint
            )
        }
    }

    private fun drawSlingshot(canvas: Canvas, engine: GameEngine, sling: TouchSlingshot) {
        if (sling.activeCount == 0) return
        val player = engine.wrestlers.firstOrNull { it.team == sling.team && it.alive } ?: return
        val ringTouchR = engine.ringRadius * 0.14f

        if (sling.activeCount == 2) {
            // Dashed tension line between A and B
            dashPaint.color = Color.argb(210, 254, 240, 138)
            dashPaint.strokeWidth = 6f
            canvas.drawLine(sling.ax, sling.ay, sling.bx, sling.by, dashPaint)

            // Stretchy elastic bands from wrestler to both touch rings
            strokePaint.color = Color.rgb(254, 249, 195)
            strokePaint.strokeWidth = 10f
            canvas.drawLine(player.x, player.y, sling.ax, sling.ay, strokePaint)
            canvas.drawLine(player.x, player.y, sling.bx, sling.by, strokePaint)

            drawTouchRing(canvas, sling.ax, sling.ay, ringTouchR)
            drawTouchRing(canvas, sling.bx, sling.by, ringTouchR)

            // Aim arrow from wrestler toward midpoint
            val midX = (sling.ax + sling.bx) * 0.5f
            val midY = (sling.ay + sling.by) * 0.5f
            val ang = atan2(midY - player.y, midX - player.x)
            drawAimArrow(canvas, player.x, player.y, ang, engine.ringRadius * 0.38f)
        } else {
            // 1-finger slingshot indicator
            val pullX = sling.singleStartX - sling.ax
            val pullY = sling.singleStartY - sling.ay
            val pullDist = hypot(pullX, pullY)

            strokePaint.color = Color.rgb(254, 249, 195)
            strokePaint.strokeWidth = 9f
            canvas.drawLine(player.x, player.y, sling.ax, sling.ay, strokePaint)
            drawTouchRing(canvas, sling.ax, sling.ay, ringTouchR * 0.9f)

            if (pullDist > engine.ringRadius * 0.08f) {
                val ang = atan2(pullY, pullX)
                drawAimArrow(canvas, player.x, player.y, ang, min(pullDist, engine.ringRadius * 0.42f))
            }
        }
    }

    private fun drawTouchRing(canvas: Canvas, x: Float, y: Float, r: Float) {
        fillPaint.color = Color.argb(48, 255, 255, 255)
        canvas.drawCircle(x, y, r, fillPaint)
        strokePaint.color = Color.argb(210, 255, 255, 255)
        strokePaint.strokeWidth = 5f
        canvas.drawCircle(x, y, r, strokePaint)
        strokePaint.color = Color.argb(110, 255, 255, 255)
        strokePaint.strokeWidth = 3f
        canvas.drawCircle(x, y, r * 0.65f, strokePaint)
    }

    private fun drawAimArrow(canvas: Canvas, x: Float, y: Float, angleRad: Float, len: Float) {
        val startX = x + cos(angleRad) * (len * 0.38f)
        val startY = y + sin(angleRad) * (len * 0.38f)
        val tipX = x + cos(angleRad) * (len * 1.15f)
        val tipY = y + sin(angleRad) * (len * 1.15f)

        strokePaint.color = Color.rgb(254, 240, 138)
        strokePaint.strokeWidth = 9f
        canvas.drawLine(startX, startY, tipX, tipY, strokePaint)

        val headLen = len * 0.28f
        val leftAng = angleRad + 2.55f
        val rightAng = angleRad - 2.55f
        canvas.drawLine(tipX, tipY, tipX + cos(leftAng) * headLen, tipY + sin(leftAng) * headLen, strokePaint)
        canvas.drawLine(tipX, tipY, tipX + cos(rightAng) * headLen, tipY + sin(rightAng) * headLen, strokePaint)
    }

    private fun drawWrestler(canvas: Canvas, engine: GameEngine, w: Wrestler) {
        if (w.fallAlpha <= 0) return

        canvas.save()
        canvas.translate(w.x, w.y)
        if (!w.alive) {
            canvas.scale(w.fallScale, w.fallScale)
        } else {
            canvas.scale(w.squashX, w.squashY)
        }
        canvas.rotate(w.angle)

        val r = w.radius

        // Drop shadow
        fillPaint.color = Color.argb((95 * w.fallAlpha) / 255, 0, 0, 0)
        canvas.drawCircle(r * 0.1f, r * 0.16f, r * 0.96f, fillPaint)

        // Folded paper torso (chamfered octagon/diamond)
        path.reset()
        path.moveTo(-r * 0.72f, -r * 0.45f)
        path.lineTo(0f, -r * 0.92f)
        path.lineTo(r * 0.72f, -r * 0.45f)
        path.lineTo(r * 0.86f, r * 0.32f)
        path.lineTo(r * 0.38f, r * 0.85f)
        path.lineTo(-r * 0.38f, r * 0.85f)
        path.lineTo(-r * 0.86f, r * 0.32f)
        path.close()

        fillPaint.color = w.archetype.baseColor
        fillPaint.alpha = w.fallAlpha
        canvas.drawPath(path, fillPaint)

        // Top origami fold highlight
        path.reset()
        path.moveTo(-r * 0.72f, -r * 0.45f)
        path.lineTo(0f, -r * 0.92f)
        path.lineTo(r * 0.72f, -r * 0.45f)
        path.lineTo(0f, r * 0.08f)
        path.close()
        fillPaint.color = w.archetype.foldColor
        fillPaint.alpha = w.fallAlpha
        canvas.drawPath(path, fillPaint)

        // Crisp paper crease outline
        strokePaint.color = Color.argb((180 * w.fallAlpha) / 255, 17, 24, 39)
        strokePaint.strokeWidth = maxOf(3f, r * 0.06f)
        canvas.drawLine(-r * 0.72f, -r * 0.45f, 0f, r * 0.08f, strokePaint)
        canvas.drawLine(r * 0.72f, -r * 0.45f, 0f, r * 0.08f, strokePaint)

        // Googly eyes
        val eyeR = r * 0.21f
        val eyeY = -r * 0.24f
        val eyeOffset = r * 0.27f
        val gazeX = (w.vx / (engine.ringRadius * 2f)).coerceIn(-0.45f, 0.45f) * eyeR
        val gazeY = (w.vy / (engine.ringRadius * 2f)).coerceIn(-0.45f, 0.45f) * eyeR
        val shockScale = if (w.shockTimer > 0f) 1.25f else 1f

        fillPaint.color = Color.argb(w.fallAlpha, 255, 255, 255)
        canvas.drawCircle(-eyeOffset, eyeY, eyeR * shockScale, fillPaint)
        canvas.drawCircle(eyeOffset, eyeY, eyeR * shockScale, fillPaint)

        fillPaint.color = Color.argb(w.fallAlpha, 17, 24, 39)
        canvas.drawCircle(-eyeOffset + gazeX, eyeY + gazeY, eyeR * 0.48f, fillPaint)
        canvas.drawCircle(eyeOffset + gazeX, eyeY + gazeY, eyeR * 0.48f, fillPaint)

        fillPaint.alpha = 255
        canvas.restore()
    }

    private fun drawParticles(canvas: Canvas, engine: GameEngine) {
        for (p in engine.particles) {
            val alpha = ((p.life / p.maxLife) * 255).toInt().coerceIn(0, 255)
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.angle)
            path.reset()
            path.moveTo(0f, -p.size)
            path.lineTo(p.size * 0.85f, p.size * 0.75f)
            path.lineTo(-p.size * 0.85f, p.size * 0.75f)
            path.close()
            fillPaint.color = p.color
            fillPaint.alpha = alpha
            canvas.drawPath(path, fillPaint)
            fillPaint.alpha = 255
            canvas.restore()
        }
    }

    private fun drawPopups(canvas: Canvas, engine: GameEngine) {
        for (pop in engine.popups) {
            val t = (pop.life / pop.maxLife).coerceIn(0f, 1f)
            val alpha = (min(1f, t * 2f) * 255).toInt()
            textPaint.textSize = engine.ringRadius * 0.135f * pop.scale
            textPaint.color = Color.argb(alpha, 17, 24, 39)
            canvas.drawText(pop.text, pop.x + 4f, pop.y + 5f, textPaint)
            textPaint.color = pop.color
            textPaint.alpha = alpha
            canvas.drawText(pop.text, pop.x, pop.y, textPaint)
            textPaint.alpha = 255
        }
    }

    private fun drawHud(canvas: Canvas, engine: GameEngine) {
        val w = engine.width
        val topY = engine.height * 0.03f
        val tagW = w * 0.25f
        val tagH = engine.height * 0.095f

        if (engine.screenState != ScreenState.MENU) {
            // YOU tag (left)
            drawPaperTag(
                canvas,
                w * 0.04f,
                topY,
                tagW,
                tagH,
                Color.rgb(15, 181, 204),
                "YOU",
                engine.scoreP1.toString()
            )

            // WAVE or 2P tag (center)
            val centerTitle = if (engine.gameMode == GameMode.SOLO_WAVES) "WAVE" else "FIRST TO 5"
            val centerVal = if (engine.gameMode == GameMode.SOLO_WAVES) engine.wave.toString() else "VS"
            drawPaperTag(
                canvas,
                w * 0.5f - tagW * 0.42f,
                topY,
                tagW * 0.84f,
                tagH * 0.86f,
                Color.rgb(203, 213, 225),
                centerTitle,
                centerVal
            )

            // CPU / P2 tag (right)
            val rightLabel = if (engine.gameMode == GameMode.SOLO_WAVES) "CPU" else "P2"
            drawPaperTag(
                canvas,
                w - w * 0.04f - tagW,
                topY,
                tagW,
                tagH,
                Color.rgb(242, 95, 58),
                rightLabel,
                engine.scoreP2.toString()
            )
        }

        // Bottom corner utility buttons: SFX mute & MENU
        val btnW = w * 0.21f
        val btnH = engine.height * 0.052f
        val btnY = engine.height - btnH - engine.height * 0.028f

        engine.btnMute.rect.set(w * 0.05f, btnY, w * 0.05f + btnW, btnY + btnH)
        drawSmallButton(
            canvas,
            engine.btnMute.rect,
            if (!engine.isMuted) "SFX: ON" else "SFX: OFF",
            Color.rgb(51, 65, 85)
        )

        if (engine.screenState != ScreenState.MENU) {
            engine.btnHome.rect.set(w - w * 0.05f - btnW, btnY, w - w * 0.05f, btnY + btnH)
            drawSmallButton(canvas, engine.btnHome.rect, "MENU", Color.rgb(51, 65, 85))
        }
    }

    private fun drawPaperTag(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        stripeColor: Int,
        label: String,
        value: String
    ) {
        // Shadow
        fillPaint.color = Color.argb(95, 0, 0, 0)
        canvas.drawRect(x + 5f, y + 7f, x + w + 5f, y + h + 7f, fillPaint)

        // Cream paper card
        fillPaint.color = Color.rgb(246, 244, 238)
        canvas.drawRect(x, y, x + w, y + h, fillPaint)

        // Colored top tape stripe
        fillPaint.color = stripeColor
        canvas.drawRect(x, y, x + w, y + h * 0.16f, fillPaint)

        textPaint.color = stripeColor
        textPaint.textSize = h * 0.24f
        canvas.drawText(label, x + w * 0.5f, y + h * 0.45f, textPaint)

        textPaint.color = Color.rgb(30, 41, 59)
        textPaint.textSize = h * 0.42f
        canvas.drawText(value, x + w * 0.5f, y + h * 0.87f, textPaint)
    }

    private fun drawSmallButton(canvas: Canvas, rect: RectF, text: String, bgColor: Int) {
        fillPaint.color = bgColor
        canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
        strokePaint.color = Color.argb(90, 255, 255, 255)
        strokePaint.strokeWidth = 2.5f
        canvas.drawRoundRect(rect, 18f, 18f, strokePaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = rect.height() * 0.42f
        canvas.drawText(text, rect.centerX(), rect.centerY() + textPaint.textSize * 0.35f, textPaint)
    }

    private fun drawMenuOverlay(canvas: Canvas, engine: GameEngine) {
        val w = engine.width
        val h = engine.height

        // Title badge at top
        textPaint.color = Color.argb(140, 0, 0, 0)
        textPaint.textSize = w * 0.13f
        canvas.drawText("PINCH & FOLD", w * 0.5f + 5f, h * 0.14f + 6f, textPaint)
        textPaint.color = Color.rgb(254, 240, 138)
        canvas.drawText("PINCH & FOLD", w * 0.5f, h * 0.14f, textPaint)

        textPaint.color = Color.rgb(203, 213, 225)
        textPaint.textSize = w * 0.036f
        canvas.drawText("2-FINGER ELASTIC WRESTLING", w * 0.5f, h * 0.185f, textPaint)

        // Two tactile paper-card mode buttons at bottom
        val btnW = w * 0.72f
        val btnH = h * 0.082f
        val cx = w * 0.5f

        engine.btnSolo.rect.set(cx - btnW * 0.5f, h * 0.71f, cx + btnW * 0.5f, h * 0.71f + btnH)
        drawActionCard(canvas, engine.btnSolo.rect, "SOLO WAVES", Color.rgb(15, 181, 204))

        engine.btnTwoPlayer.rect.set(cx - btnW * 0.5f, h * 0.815f, cx + btnW * 0.5f, h * 0.815f + btnH)
        drawActionCard(canvas, engine.btnTwoPlayer.rect, "2P SAME PHONE", Color.rgb(242, 95, 58))

        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = w * 0.031f
        canvas.drawText("Stretch 2 fingers & lift 1 to SNAP • Pinch enemies to FLING", w * 0.5f, h * 0.935f, textPaint)
    }

    private fun drawActionCard(canvas: Canvas, rect: RectF, label: String, accent: Int) {
        fillPaint.color = Color.argb(120, 0, 0, 0)
        tmpRect.set(rect.left + 6f, rect.top + 8f, rect.right + 6f, rect.bottom + 8f)
        canvas.drawRoundRect(tmpRect, 20f, 20f, fillPaint)

        fillPaint.color = Color.rgb(246, 244, 238)
        canvas.drawRoundRect(rect, 20f, 20f, fillPaint)

        fillPaint.color = accent
        tmpRect.set(rect.left, rect.top, rect.left + rect.width() * 0.08f, rect.bottom)
        canvas.drawRoundRect(tmpRect, 12f, 12f, fillPaint)

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = rect.height() * 0.38f
        canvas.drawText(label, rect.centerX() + rect.width() * 0.02f, rect.centerY() + textPaint.textSize * 0.34f, textPaint)
    }

    private fun drawRoundBanner(canvas: Canvas, engine: GameEngine) {
        val w = engine.width
        val h = engine.height
        fillPaint.color = Color.argb(180, 15, 23, 42)
        canvas.drawRect(0f, h * 0.42f, w, h * 0.56f, fillPaint)

        textPaint.color = Color.rgb(254, 240, 138)
        textPaint.textSize = w * 0.095f
        canvas.drawText(engine.roundBannerText, w * 0.5f, h * 0.505f, textPaint)
    }

    private fun drawGameOverOverlay(canvas: Canvas, engine: GameEngine) {
        val w = engine.width
        val h = engine.height
        fillPaint.color = Color.argb(175, 15, 23, 42)
        canvas.drawRect(0f, 0f, w, h, fillPaint)

        val title = if (engine.gameMode == GameMode.SOLO_WAVES) {
            "KNOCKED OUT!"
        } else if (engine.scoreP1 >= 5) {
            "TEAL WINS!"
        } else {
            "CORAL WINS!"
        }

        textPaint.color = Color.rgb(254, 240, 138)
        textPaint.textSize = w * 0.11f
        canvas.drawText(title, w * 0.5f, h * 0.39f, textPaint)

        val sub = if (engine.gameMode == GameMode.SOLO_WAVES) {
            "REACHED WAVE ${engine.wave}  •  BEST ${engine.bestWave}"
        } else {
            "FINAL SCORE  ${engine.scoreP1} - ${engine.scoreP2}"
        }
        textPaint.color = Color.rgb(226, 232, 240)
        textPaint.textSize = w * 0.044f
        canvas.drawText(sub, w * 0.5f, h * 0.45f, textPaint)

        val btnW = w * 0.66f
        val btnH = h * 0.082f
        engine.btnPlayAgain.rect.set(w * 0.5f - btnW * 0.5f, h * 0.54f, w * 0.5f + btnW * 0.5f, h * 0.54f + btnH)
        drawActionCard(canvas, engine.btnPlayAgain.rect, "PLAY AGAIN", Color.rgb(15, 181, 204))
    }
}
