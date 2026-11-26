package com.pinchandfold.game

import android.graphics.Color
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Core physics, multi-touch slingshot/pinch input, CPU wave AI, and game state.
 */
class GameEngine(private val audio: SoundAndHaptics) {

    var width: Float = 1080f
        private set
    var height: Float = 1920f
        private set

    var ringCenterX: Float = 540f
        private set
    var ringCenterY: Float = 1020f
        private set
    var ringRadius: Float = 420f
        private set

    var screenState: ScreenState = ScreenState.MENU
        private set
    var gameMode: GameMode = GameMode.SOLO_WAVES
        private set

    var wave: Int = 1
        private set
    var scoreP1: Int = 0
        private set
    var scoreP2: Int = 0
        private set
    var bestWave: Int = 1
        private set
    val isMuted: Boolean
        get() = audio.muted

    var roundBannerText: String = ""
    var roundWinnerTeam: Int = 0
    private var roundTimer: Float = 0f

    // Juice & camera
    var shakeIntensity: Float = 0f
    var shakeX: Float = 0f
    var shakeY: Float = 0f
    private var hitStopTimer: Float = 0f
    var timeElapsed: Float = 0f

    val wrestlers = mutableListOf<Wrestler>()
    val particles = mutableListOf<Particle>()
    val popups = mutableListOf<ComicPopup>()
    val ropeFlexes = Array(4) { RopeFlex() }

    val slingshotP1 = TouchSlingshot(team = 0)
    val slingshotP2 = TouchSlingshot(team = 1)

    // UI hitboxes (updated by renderer/layout)
    val btnSolo = ButtonRect(label = "SOLO WAVES")
    val btnTwoPlayer = ButtonRect(label = "2P SAME PHONE")
    val btnMute = ButtonRect(label = "SFX")
    val btnHome = ButtonRect(label = "MENU")
    val btnPlayAgain = ButtonRect(label = "PLAY AGAIN")

    private var nextWrestlerId = 1
    private var demoTimer = 0f

    fun resize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        width = w.toFloat()
        height = h.toFloat()
        ringCenterX = width * 0.5f
        ringCenterY = height * 0.53f
        ringRadius = min(width * 0.43f, height * 0.32f)

        if (wrestlers.isEmpty()) {
            setupDemoRing()
        }
    }

    private fun setupDemoRing() {
        wrestlers.clear()
        val r = ringRadius * 0.135f
        wrestlers.add(
            Wrestler(
                id = nextWrestlerId++,
                team = 0,
                archetype = Archetype.PLAYER_TEAL,
                x = ringCenterX - ringRadius * 0.35f,
                y = ringCenterY + ringRadius * 0.25f,
                vx = 180f,
                vy = -140f,
                radius = r,
                mass = 1f
            )
        )
        wrestlers.add(
            Wrestler(
                id = nextWrestlerId++,
                team = 1,
                archetype = Archetype.LIGHT_ORIGAMI,
                x = ringCenterX + ringRadius * 0.35f,
                y = ringCenterY - ringRadius * 0.25f,
                vx = -160f,
                vy = 150f,
                radius = r * 0.95f,
                mass = 0.9f
            )
        )
    }

    fun startNewGame(mode: GameMode) {
        gameMode = mode
        wave = 1
        scoreP1 = 0
        scoreP2 = 0
        slingshotP1.reset()
        slingshotP2.reset()
        particles.clear()
        popups.clear()
        screenState = ScreenState.PLAYING
        audio.playUiTap()
        spawnRound()
    }

    private fun spawnRound() {
        wrestlers.clear()
        slingshotP1.reset()
        slingshotP2.reset()
        val baseR = ringRadius * 0.135f

        // Player 1 (bottom)
        wrestlers.add(
            Wrestler(
                id = nextWrestlerId++,
                team = 0,
                archetype = Archetype.PLAYER_TEAL,
                x = ringCenterX,
                y = ringCenterY + ringRadius * 0.42f,
                radius = baseR,
                mass = Archetype.PLAYER_TEAL.mass
            )
        )

        if (gameMode == GameMode.TWO_PLAYER) {
            wrestlers.add(
                Wrestler(
                    id = nextWrestlerId++,
                    team = 1,
                    archetype = Archetype.PLAYER_CORAL,
                    x = ringCenterX,
                    y = ringCenterY - ringRadius * 0.42f,
                    radius = baseR,
                    mass = Archetype.PLAYER_CORAL.mass
                )
            )
        } else {
            spawnWaveEnemies(baseR)
        }
    }

    private fun spawnWaveEnemies(baseR: Float) {
        when {
            wave == 1 -> {
                addEnemy(Archetype.LIGHT_ORIGAMI, 0f, -0.42f, baseR)
            }
            wave == 2 -> {
                addEnemy(Archetype.HEAVY_CARDBOARD, 0f, -0.42f, baseR)
            }
            wave == 3 -> {
                addEnemy(Archetype.LIGHT_ORIGAMI, -0.35f, -0.36f, baseR)
                addEnemy(Archetype.NINJA_STAR, 0.35f, -0.36f, baseR)
            }
            wave == 4 -> {
                addEnemy(Archetype.HEAVY_CARDBOARD, -0.28f, -0.38f, baseR)
                addEnemy(Archetype.NINJA_STAR, 0.32f, -0.34f, baseR)
            }
            wave % 5 == 0 -> {
                addEnemy(Archetype.GOLD_BOSS, 0f, -0.42f, baseR)
                addEnemy(Archetype.NINJA_STAR, 0.42f, -0.22f, baseR)
            }
            else -> {
                val count = min(3, 2 + wave / 4)
                val pool = listOf(
                    Archetype.LIGHT_ORIGAMI,
                    Archetype.HEAVY_CARDBOARD,
                    Archetype.NINJA_STAR
                )
                for (i in 0 until count) {
                    val arch = pool[(wave + i) % pool.size]
                    val offset = (i - (count - 1) * 0.5f) * 0.45f
                    addEnemy(arch, offset, -0.38f, baseR)
                }
            }
        }
    }

    private fun addEnemy(arch: Archetype, normX: Float, normY: Float, baseR: Float) {
        wrestlers.add(
            Wrestler(
                id = nextWrestlerId++,
                team = 1,
                archetype = arch,
                x = ringCenterX + ringRadius * normX,
                y = ringCenterY + ringRadius * normY,
                radius = baseR * arch.radiusScale,
                mass = arch.mass,
                aiCooldown = 0.6f + Random.nextFloat() * 0.6f
            )
        )
    }

    fun update(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.033f)
        timeElapsed += dt

        // Update screen shake
        if (shakeIntensity > 0.2f) {
            shakeX = (Random.nextFloat() * 2f - 1f) * shakeIntensity
            shakeY = (Random.nextFloat() * 2f - 1f) * shakeIntensity
            shakeIntensity *= 0.84f
        } else {
            shakeIntensity = 0f
            shakeX = 0f
            shakeY = 0f
        }

        // Update particles & popups even during hit-stop
        updateParticlesAndPopups(dt)
        updateRopeFlex(dt)

        if (hitStopTimer > 0f) {
            hitStopTimer -= dt
            return
        }

        if (screenState == ScreenState.MENU) {
            updateDemo(dt)
            return
        }

        if (screenState == ScreenState.ROUND_OVER) {
            roundTimer -= dt
            updatePhysics(dt * 0.7f)
            if (roundTimer <= 0f) {
                if (gameMode == GameMode.SOLO_WAVES) {
                    if (roundWinnerTeam == 0) {
                        wave++
                        if (wave > bestWave) bestWave = wave
                        screenState = ScreenState.PLAYING
                        spawnRound()
                    } else {
                        screenState = ScreenState.GAME_OVER
                    }
                } else {
                    if (scoreP1 >= 5 || scoreP2 >= 5) {
                        screenState = ScreenState.GAME_OVER
                    } else {
                        screenState = ScreenState.PLAYING
                        spawnRound()
                    }
                }
            }
            return
        }

        if (screenState == ScreenState.GAME_OVER) {
            updatePhysics(dt * 0.5f)
            return
        }

        // PLAYING state
        if (gameMode == GameMode.SOLO_WAVES) {
            updateCpuAi(dt)
        }
        updatePhysics(dt)
        checkRoundEnd()
    }

    private fun updateDemo(dt: Float) {
        demoTimer += dt
        if (demoTimer > 2.2f) {
            demoTimer = 0f
            for (w in wrestlers) {
                if (w.alive) {
                    val ang = atan2(ringCenterY - w.y, ringCenterX - w.x) + (Random.nextFloat() - 0.5f) * 0.9f
                    val spd = ringRadius * 1.4f
                    w.vx += cos(ang) * spd
                    w.vy += sin(ang) * spd
                }
            }
        }
        updatePhysics(dt)
        // Respawn demo wrestlers if they ring out
        if (wrestlers.count { it.alive } < 2) {
            setupDemoRing()
        }
    }

    private fun updateCpuAi(dt: Float) {
        val player = wrestlers.firstOrNull { it.team == 0 && it.alive } ?: return
        for (cpu in wrestlers) {
            if (cpu.team != 1 || !cpu.alive) continue
            if (cpu.shockTimer > 0f) continue

            cpu.aiCooldown -= dt
            if (cpu.aiCooldown <= 0f) {
                // Aim at player with slight prediction
                val tx = player.x + player.vx * 0.14f
                val ty = player.y + player.vy * 0.14f
                val dx = tx - cpu.x
                val dy = ty - cpu.y
                val dist = max(1f, hypot(dx, dy))

                val power = when (cpu.archetype) {
                    Archetype.NINJA_STAR -> ringRadius * 2.55f
                    Archetype.HEAVY_CARDBOARD -> ringRadius * 1.85f
                    Archetype.GOLD_BOSS -> ringRadius * 2.35f
                    else -> ringRadius * 2.0f
                }

                // Add small inaccuracy on early waves
                val jitter = max(0.05f, 0.35f - wave * 0.04f)
                val aimAngle = atan2(dy, dx) + (Random.nextFloat() * 2f - 1f) * jitter
                cpu.vx += cos(aimAngle) * power
                cpu.vy += sin(aimAngle) * power
                cpu.squashX = 1.25f
                cpu.squashY = 0.78f
                cpu.aiCooldown = when (cpu.archetype) {
                    Archetype.NINJA_STAR -> 0.75f + Random.nextFloat() * 0.35f
                    Archetype.GOLD_BOSS -> 0.85f + Random.nextFloat() * 0.35f
                    else -> max(0.75f, 1.35f - wave * 0.06f) + Random.nextFloat() * 0.45f
                }
                audio.playSnap(0.45f)
            }
        }
    }

    private fun updatePhysics(dt: Float) {
        val friction = 0.968f
        val ringOutSpeed = ringRadius * 1.65f

        for (w in wrestlers) {
            if (!w.alive) {
                // Falling off the table animation
                w.x += w.vx * dt
                w.y += w.vy * dt
                w.angle += w.angularVel * dt
                w.fallScale = max(0f, w.fallScale - dt * 2.1f)
                w.fallAlpha = (w.fallScale * 255).toInt().coerceIn(0, 255)
                continue
            }

            if (w.shockTimer > 0f) w.shockTimer = max(0f, w.shockTimer - dt)

            // Recover squash & stretch smoothly
            w.squashX += (1f - w.squashX) * min(1f, dt * 12f)
            w.squashY += (1f - w.squashY) * min(1f, dt * 12f)

            // Integrate velocity
            w.x += w.vx * dt
            w.y += w.vy * dt

            // Frame-rate independent damping
            val damp = Math.pow(friction.toDouble(), (dt * 60f).toDouble()).toFloat()
            w.vx *= damp
            w.vy *= damp

            // Spin slightly based on speed
            val spd = hypot(w.vx, w.vy)
            w.angle += (w.angularVel + spd * 0.04f) * dt
            w.angularVel *= damp

            // Ring boundary & elastic rubber-band ropes
            val dx = w.x - ringCenterX
            val dy = w.y - ringCenterY
            val dist = hypot(dx, dy)
            val maxDist = ringRadius - w.radius

            if (dist > maxDist && dist > 0.001f) {
                val nx = dx / dist
                val ny = dy / dist
                val outwardVel = w.vx * nx + w.vy * ny

                // Ring out if slammed hard OR already stunned/shocked
                val breaksRopes = outwardVel > ringOutSpeed || (w.shockTimer > 0.15f && outwardVel > ringOutSpeed * 0.55f)
                if (breaksRopes && screenState == ScreenState.PLAYING) {
                    w.alive = false
                    w.vx = nx * max(outwardVel, ringRadius * 1.1f)
                    w.vy = ny * max(outwardVel, ringRadius * 1.1f)
                    w.angularVel = (if (Random.nextBoolean()) 1f else -1f) * 320f
                    triggerRopeHit(atan2(ny, nx), outwardVel)
                    spawnConfetti(w.x, w.y, 18, w.archetype.baseColor)
                    popups.add(
                        ComicPopup(
                            text = "RING OUT!",
                            x = ringCenterX + nx * ringRadius * 0.72f,
                            y = ringCenterY + ny * ringRadius * 0.72f,
                            vy = -ringRadius * 0.25f,
                            color = w.archetype.foldColor,
                            scale = 1.25f,
                            life = 1.0f,
                            maxLife = 1.0f
                        )
                    )
                    shakeIntensity = max(shakeIntensity, 22f)
                    audio.playRingOut()
                    audio.vibrate(75, 220)
                } else {
                    // Elastic rubber-band bounce!
                    w.x = ringCenterX + nx * maxDist
                    w.y = ringCenterY + ny * maxDist
                    if (outwardVel > 0f) {
                        val bounce = 1.72f
                        w.vx -= bounce * outwardVel * nx
                        w.vy -= bounce * outwardVel * ny
                        w.squashX = 0.76f
                        w.squashY = 1.26f
                        if (outwardVel > ringRadius * 0.28f) {
                            triggerRopeHit(atan2(ny, nx), outwardVel)
                            audio.playRopeBounce(outwardVel / (ringRadius * 1.5f))
                            audio.vibrate(18, 90)
                        }
                    }
                }
            }
        }

        // Wrestler-vs-Wrestler collisions
        for (i in 0 until wrestlers.size) {
            val a = wrestlers[i]
            if (!a.alive) continue
            for (j in i + 1 until wrestlers.size) {
                val b = wrestlers[j]
                if (!b.alive) continue

                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy)
                val minDist = a.radius + b.radius

                if (dist < minDist && dist > 0.0001f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val overlap = minDist - dist

                    val totalMass = a.mass + b.mass
                    a.x -= nx * overlap * (b.mass / totalMass)
                    a.y -= ny * overlap * (b.mass / totalMass)
                    b.x += nx * overlap * (a.mass / totalMass)
                    b.y += ny * overlap * (a.mass / totalMass)

                    val rvx = b.vx - a.vx
                    val rvy = b.vy - a.vy
                    val velAlongNormal = rvx * nx + rvy * ny

                    if (velAlongNormal < 0f) {
                        val restitution = 1.18f
                        val impulse = -(1f + restitution) * velAlongNormal / (1f / a.mass + 1f / b.mass)
                        val ix = impulse * nx
                        val iy = impulse * ny

                        a.vx -= ix / a.mass
                        a.vy -= iy / a.mass
                        b.vx += ix / b.mass
                        b.vy += iy / b.mass

                        val impactSpeed = -velAlongNormal
                        if (impactSpeed > ringRadius * 0.45f) {
                            val midX = (a.x + b.x) * 0.5f
                            val midY = (a.y + b.y) * 0.5f
                            a.squashX = 0.72f
                            a.squashY = 1.32f
                            b.squashX = 0.72f
                            b.squashY = 1.32f
                            a.shockTimer = 0.38f
                            b.shockTimer = 0.38f

                            val heavy = impactSpeed > ringRadius * 1.1f
                            spawnConfetti(midX, midY, if (heavy) 14 else 7, Color.WHITE)
                            if (heavy) {
                                hitStopTimer = 0.038f
                                shakeIntensity = max(shakeIntensity, 16f)
                                val word = if (Random.nextBoolean()) "SNAP!" else "POW!"
                                popups.add(
                                    ComicPopup(
                                        text = word,
                                        x = midX,
                                        y = midY - a.radius * 0.6f,
                                        vy = -ringRadius * 0.22f,
                                        color = Color.rgb(253, 224, 71),
                                        scale = 1.15f,
                                        life = 0.65f,
                                        maxLife = 0.65f
                                    )
                                )
                                audio.vibrate(45, 190)
                            } else {
                                shakeIntensity = max(shakeIntensity, 6f)
                                audio.vibrate(20, 110)
                            }
                            audio.playHit(impactSpeed / (ringRadius * 1.6f))
                        }
                    }
                }
            }
        }
    }

    private fun triggerRopeHit(angleRad: Float, speed: Float) {
        // 4 quadrants centered at 0, 90, 180, 270 deg (posts at 45, 135, 225, 315)
        var deg = Math.toDegrees(angleRad.toDouble()).toFloat()
        if (deg < 0f) deg += 360f
        val idx = (((deg + 45f) % 360f) / 90f).toInt().coerceIn(0, 3)
        ropeFlexes[idx].flexVel = max(ropeFlexes[idx].flexVel, speed * 0.55f)
        ropeFlexes[idx].hitAngle = angleRad
    }

    private fun updateRopeFlex(dt: Float) {
        val stiffness = 180f
        val damping = 11f
        for (rope in ropeFlexes) {
            val acc = -stiffness * rope.flexOffset - damping * rope.flexVel
            rope.flexVel += acc * dt
            rope.flexOffset += rope.flexVel * dt
            rope.flexOffset = rope.flexOffset.coerceIn(-ringRadius * 0.12f, ringRadius * 0.22f)
        }
    }

    private fun spawnConfetti(x: Float, y: Float, count: Int, primaryColor: Int) {
        val palette = intArrayOf(
            primaryColor,
            Color.rgb(15, 181, 204),
            Color.rgb(242, 95, 58),
            Color.rgb(253, 224, 71),
            Color.WHITE
        )
        for (i in 0 until count) {
            val ang = Random.nextFloat() * Math.PI.toFloat() * 2f
            val spd = ringRadius * (0.4f + Random.nextFloat() * 1.2f)
            val life = 0.45f + Random.nextFloat() * 0.45f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    size = ringRadius * (0.025f + Random.nextFloat() * 0.03f),
                    angle = Random.nextFloat() * 360f,
                    spin = (Random.nextFloat() - 0.5f) * 720f,
                    color = palette[i % palette.size],
                    life = life,
                    maxLife = life
                )
            )
        }
    }

    private fun updateParticlesAndPopups(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIter.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= 0.94f
                p.vy *= 0.94f
                p.angle += p.spin * dt
            }
        }

        val popIter = popups.iterator()
        while (popIter.hasNext()) {
            val pop = popIter.next()
            pop.life -= dt
            if (pop.life <= 0f) {
                popIter.remove()
            } else {
                pop.y += pop.vy * dt
                pop.vy *= 0.92f
            }
        }
    }

    private fun checkRoundEnd() {
        val p1Alive = wrestlers.any { it.team == 0 && it.alive }
        val p2Alive = wrestlers.any { it.team == 1 && it.alive }

        if (!p1Alive || !p2Alive) {
            screenState = ScreenState.ROUND_OVER
            roundTimer = 1.45f
            if (p1Alive && !p2Alive) {
                roundWinnerTeam = 0
                scoreP1++
                roundBannerText = if (gameMode == GameMode.SOLO_WAVES) "WAVE $wave CLEAR!" else "YOU SCORE!"
                audio.playWin()
            } else {
                roundWinnerTeam = 1
                scoreP2++
                roundBannerText = if (gameMode == GameMode.SOLO_WAVES) "KNOCKED OUT!" else "P2 SCORES!"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Multi-Touch Input Handling
    // -------------------------------------------------------------------------

    fun handleTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        val px = event.getX(index)
        val py = event.getY(index)

        // Check global HUD buttons first on ACTION_DOWN
        if (action == MotionEvent.ACTION_DOWN) {
            if (btnMute.rect.contains(px, py)) {
                audio.muted = !audio.muted
                audio.playUiTap()
                return true
            }
            if (screenState != ScreenState.MENU && btnHome.rect.contains(px, py)) {
                audio.playUiTap()
                screenState = ScreenState.MENU
                setupDemoRing()
                return true
            }
        }

        if (screenState == ScreenState.MENU) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (btnSolo.rect.contains(px, py)) {
                    startNewGame(GameMode.SOLO_WAVES)
                } else if (btnTwoPlayer.rect.contains(px, py)) {
                    startNewGame(GameMode.TWO_PLAYER)
                }
            }
            return true
        }

        if (screenState == ScreenState.GAME_OVER) {
            if (action == MotionEvent.ACTION_DOWN && btnPlayAgain.rect.contains(px, py)) {
                startNewGame(gameMode)
            }
            return true
        }

        if (screenState != ScreenState.PLAYING) return true

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val sling = selectSlingshotForTouch(py)
                if (sling.pointerIdA == -1) {
                    sling.pointerIdA = pointerId
                    sling.ax = px
                    sling.ay = py
                    sling.singleStartX = px
                    sling.singleStartY = py
                    sling.lastTickDistance = 0f
                } else if (sling.pointerIdB == -1) {
                    sling.pointerIdB = pointerId
                    sling.bx = px
                    sling.by = py
                    sling.lastTickDistance = hypot(sling.bx - sling.ax, sling.by - sling.ay)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val mx = event.getX(i)
                    val my = event.getY(i)
                    updatePointerMove(slingshotP1, id, mx, my)
                    updatePointerMove(slingshotP2, id, mx, my)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                releasePointer(slingshotP1, pointerId)
                releasePointer(slingshotP2, pointerId)
            }
        }
        return true
    }

    private fun selectSlingshotForTouch(py: Float): TouchSlingshot {
        if (gameMode == GameMode.TWO_PLAYER && py < ringCenterY) {
            return slingshotP2
        }
        return slingshotP1
    }

    private fun updatePointerMove(sling: TouchSlingshot, id: Int, mx: Float, my: Float) {
        var moved = false
        if (sling.pointerIdA == id) {
            sling.ax = mx
            sling.ay = my
            moved = true
        } else if (sling.pointerIdB == id) {
            sling.bx = mx
            sling.by = my
            moved = true
        }
        if (!moved) return

        val stretchDist = currentStretchDistance(sling)
        if (kotlin.math.abs(stretchDist - sling.lastTickDistance) > ringRadius * 0.14f) {
            sling.lastTickDistance = stretchDist
            audio.playStretchTick()
            audio.vibrate(8, 55)
        }

        // Check Pinch Grapple: two fingers closing tightly around an opponent!
        if (sling.activeCount == 2 && stretchDist < ringRadius * 0.24f) {
            val midX = (sling.ax + sling.bx) * 0.5f
            val midY = (sling.ay + sling.by) * 0.5f
            val enemy = wrestlers.firstOrNull {
                it.team != sling.team && it.alive && hypot(it.x - midX, it.y - midY) < it.radius * 1.45f
            }
            if (enemy != null) {
                val player = wrestlers.firstOrNull { it.team == sling.team && it.alive }
                val throwAng = if (player != null) {
                    atan2(enemy.y - player.y, enemy.x - player.x)
                } else {
                    atan2(enemy.y - ringCenterY, enemy.x - ringCenterX)
                }
                val flingSpeed = ringRadius * 2.4f / enemy.mass
                enemy.vx += cos(throwAng) * flingSpeed
                enemy.vy += sin(throwAng) * flingSpeed
                enemy.shockTimer = 0.5f
                enemy.squashX = 0.7f
                enemy.squashY = 1.35f
                popups.add(
                    ComicPopup(
                        text = "PINCH!",
                        x = enemy.x,
                        y = enemy.y - enemy.radius,
                        vy = -ringRadius * 0.3f,
                        color = Color.rgb(43, 212, 234),
                        scale = 1.15f,
                        life = 0.7f,
                        maxLife = 0.7f
                    )
                )
                spawnConfetti(enemy.x, enemy.y, 10, enemy.archetype.baseColor)
                audio.playSnap(1.0f)
                audio.vibrate(35, 170)
                sling.reset()
            }
        }
    }

    private fun releasePointer(sling: TouchSlingshot, id: Int) {
        if (sling.pointerIdA != id && sling.pointerIdB != id) return

        val player = wrestlers.firstOrNull { it.team == sling.team && it.alive }

        if (sling.activeCount == 2 && player != null) {
            // 2-finger snap: lifting one finger whips the wrestler toward the remaining finger!
            val anchorX = if (sling.pointerIdA == id) sling.bx else sling.ax
            val anchorY = if (sling.pointerIdA == id) sling.by else sling.ay
            val dx = anchorX - player.x
            val dy = anchorY - player.y
            val stretch = hypot(sling.bx - sling.ax, sling.by - sling.ay)
            val normPower = (stretch / (ringRadius * 0.95f)).coerceIn(0.25f, 1.35f)
            val ang = atan2(dy, dx)
            val launchSpeed = ringRadius * 2.35f * normPower

            player.vx += cos(ang) * launchSpeed
            player.vy += sin(ang) * launchSpeed
            player.squashX = 1.32f
            player.squashY = 0.74f
            audio.playSnap(0.95f)
            audio.vibrate(25, 140)

            // Keep the remaining finger as single pointer
            if (sling.pointerIdA == id) {
                sling.pointerIdA = sling.pointerIdB
                sling.ax = sling.bx
                sling.ay = sling.by
            }
            sling.pointerIdB = -1
            sling.singleStartX = sling.ax
            sling.singleStartY = sling.ay
        } else if (sling.activeCount == 1 && player != null) {
            // 1-finger slingshot release: pull back from start, launch opposite
            val pullX = sling.singleStartX - sling.ax
            val pullY = sling.singleStartY - sling.ay
            val pullDist = hypot(pullX, pullY)
            if (pullDist > ringRadius * 0.08f) {
                val normPower = (pullDist / (ringRadius * 0.75f)).coerceIn(0.2f, 1.3f)
                val ang = atan2(pullY, pullX)
                val launchSpeed = ringRadius * 2.25f * normPower
                player.vx += cos(ang) * launchSpeed
                player.vy += sin(ang) * launchSpeed
                player.squashX = 1.28f
                player.squashY = 0.76f
                audio.playSnap(0.85f)
                audio.vibrate(22, 130)
            }
            sling.reset()
        } else {
            sling.reset()
        }
    }

    fun currentStretchDistance(sling: TouchSlingshot): Float {
        return when (sling.activeCount) {
            2 -> hypot(sling.bx - sling.ax, sling.by - sling.ay)
            1 -> hypot(sling.singleStartX - sling.ax, sling.singleStartY - sling.ay)
            else -> 0f
        }
    }
}
