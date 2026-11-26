package com.pinchandfold.game

import android.graphics.Color
import android.graphics.RectF

enum class GameMode {
    SOLO_WAVES,
    TWO_PLAYER
}

enum class ScreenState {
    MENU,
    PLAYING,
    ROUND_OVER,
    GAME_OVER
}

enum class Archetype(
    val label: String,
    val baseColor: Int,
    val foldColor: Int,
    val radiusScale: Float,
    val mass: Float
) {
    PLAYER_TEAL("YOU", Color.rgb(15, 181, 204), Color.rgb(43, 212, 234), 1.0f, 1.0f),
    PLAYER_CORAL("P2", Color.rgb(242, 95, 58), Color.rgb(255, 134, 98), 1.0f, 1.0f),
    LIGHT_ORIGAMI("FLIMSY", Color.rgb(242, 95, 58), Color.rgb(255, 134, 98), 0.92f, 0.82f),
    HEAVY_CARDBOARD("BRUTE", Color.rgb(184, 114, 62), Color.rgb(214, 148, 92), 1.24f, 1.85f),
    NINJA_STAR("DART", Color.rgb(168, 85, 247), Color.rgb(196, 132, 252), 0.84f, 0.78f),
    GOLD_BOSS("CHAMP", Color.rgb(234, 179, 8), Color.rgb(253, 224, 71), 1.32f, 2.2f)
}

data class Wrestler(
    val id: Int,
    val team: Int, // 0 = Player 1 (Teal), 1 = CPU or Player 2
    val archetype: Archetype,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var radius: Float,
    var mass: Float,
    var angle: Float = 0f,
    var angularVel: Float = 0f,
    var squashX: Float = 1f,
    var squashY: Float = 1f,
    var shockTimer: Float = 0f,
    var aiCooldown: Float = 0.8f,
    var aiChargeTimer: Float = 0f,
    var aiTargetX: Float = 0f,
    var aiTargetY: Float = 0f,
    var alive: Boolean = true,
    var fallScale: Float = 1f,
    var fallAlpha: Int = 255
)

/**
 * Tracks multi-touch fingers for a player (supports 2-finger stretch/pinch AND 1-finger drag).
 */
class TouchSlingshot(val team: Int) {
    var pointerIdA: Int = -1
    var ax: Float = 0f
    var ay: Float = 0f

    var pointerIdB: Int = -1
    var bx: Float = 0f
    var by: Float = 0f

    // For 1-finger fallback slingshot
    var singleStartX: Float = 0f
    var singleStartY: Float = 0f
    var lastTickDistance: Float = 0f

    val activeCount: Int
        get() = (if (pointerIdA != -1) 1 else 0) + (if (pointerIdB != -1) 1 else 0)

    fun reset() {
        pointerIdA = -1
        pointerIdB = -1
        lastTickDistance = 0f
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var angle: Float,
    var spin: Float,
    var color: Int,
    var life: Float,
    var maxLife: Float
)

data class ComicPopup(
    val text: String,
    var x: Float,
    var y: Float,
    var vy: Float,
    var color: Int,
    var scale: Float,
    var life: Float,
    val maxLife: Float
)

data class RopeFlex(
    var flexOffset: Float = 0f,
    var flexVel: Float = 0f,
    var hitAngle: Float = 0f
)

data class ButtonRect(
    val rect: RectF = RectF(),
    val label: String
)
