package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.model.PlayerProfile
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class GamePhase {
  MENU_IDLE,
  RUNNING,
  PAUSED,
  GAME_OVER,
}

enum class ObstacleType(
  val isTrain: Boolean,
  val isRamp: Boolean,
  val lengthZ: Float,
  val topHeight: Float,
  val bottomClearance: Float,
) {
  TRAIN_YELLOW(isTrain = true, isRamp = false, lengthZ = 18f, topHeight = 1.18f, bottomClearance = 0f),
  TRAIN_TEAL(isTrain = true, isRamp = false, lengthZ = 18f, topHeight = 1.18f, bottomClearance = 0f),
  TRAIN_RED(isTrain = true, isRamp = false, lengthZ = 22f, topHeight = 1.18f, bottomClearance = 0f),
  TRAIN_RAMP(isTrain = false, isRamp = true, lengthZ = 6.5f, topHeight = 1.18f, bottomClearance = 0f),
  BARRIER_JUMP(isTrain = false, isRamp = false, lengthZ = 1.4f, topHeight = 0.62f, bottomClearance = 0f),
  BARRIER_SLIDE(isTrain = false, isRamp = false, lengthZ = 1.4f, topHeight = 1.45f, bottomClearance = 0.52f),
  BARRIER_FULL(isTrain = false, isRamp = false, lengthZ = 1.8f, topHeight = 1.28f, bottomClearance = 0f),
}

enum class PowerUpType(val labelTr: String, val accentColor: Color) {
  MAGNET("Mıknatıs", Color(0xFFFF3B5C)),
  JETPACK("Böcek Kanadı", Color(0xFFFF6D00)),
  MULTIPLIER_2X("2x Çarpan", Color(0xFFFFD600)),
  SNEAKERS("Süper Ayakkabı", Color(0xFF00E5FF)),
}

data class TrackObstacle(
  val id: Int,
  val lane: Int,
  var z: Float,
  val type: ObstacleType,
  val speedOffset: Float = 0f,
)

data class TrackCoin(
  val id: Int,
  var lane: Float,
  var z: Float,
  var yOffset: Float,
  var collected: Boolean = false,
)

data class TrackPowerUp(
  val id: Int,
  val lane: Int,
  var z: Float,
  val yOffset: Float,
  val type: PowerUpType,
  var collected: Boolean = false,
)

data class GameParticle(
  var xNorm: Float,
  var yNorm: Float,
  var vx: Float,
  var vy: Float,
  var life: Float,
  val maxLife: Float,
  val color: Color,
  val radiusDp: Float,
)

data class FloatingCallout(
  val id: Int,
  val text: String,
  var xNorm: Float,
  var yNorm: Float,
  var life: Float,
  val maxLife: Float,
  val color: Color,
)

sealed interface GameEvent {
  data object CoinCollected : GameEvent
  data object Jumped : GameEvent
  data object Slid : GameEvent
  data object LaneSwitched : GameEvent
  data class PowerUpPicked(val type: PowerUpType) : GameEvent
  data object HoverboardActivated : GameEvent
  data object HoverboardShattered : GameEvent
  data object Stumbled : GameEvent
  data class Crashed(
    val finalScore: Long,
    val distanceMeters: Long,
    val coinsCollected: Long,
    val hoverboardsUsed: Long,
  ) : GameEvent
}

data class GameSnapshot(
  val phase: GamePhase = GamePhase.MENU_IDLE,
  val visualLane: Float = 0f,
  val targetLane: Int = 0,
  val playerY: Float = 0f,
  val groundHeight: Float = 0f,
  val isSliding: Boolean = false,
  val isJumping: Boolean = false,
  val isOnJetpack: Boolean = false,
  val runCycle: Float = 0f,
  val tiltAngle: Float = 0f,
  val squashStretch: Float = 1f,
  val worldScroll: Float = 0f,
  val speed: Float = 26f,
  val distanceMeters: Long = 0L,
  val score: Long = 0L,
  val runCoins: Long = 0L,
  val activeMultiplier: Int = 1,
  val magnetRemaining: Float = 0f,
  val magnetMax: Float = 8f,
  val jetpackRemaining: Float = 0f,
  val jetpackMax: Float = 6f,
  val multiplier2xRemaining: Float = 0f,
  val multiplier2xMax: Float = 10f,
  val sneakersRemaining: Float = 0f,
  val sneakersMax: Float = 9f,
  val hoverboardRemaining: Float = 0f,
  val hoverboardMax: Float = 15f,
  val chaserDistance: Float = 0.35f,
  val stumbleFlash: Float = 0f,
  val screenShake: Float = 0f,
  val obstacles: List<TrackObstacle> = emptyList(),
  val coins: List<TrackCoin> = emptyList(),
  val powerUps: List<TrackPowerUp> = emptyList(),
  val particles: List<GameParticle> = emptyList(),
  val callouts: List<FloatingCallout> = emptyList(),
  val availableHoverboards: Long = 3L,
)

class GameEngine(
  private val random: Random = Random.Default,
) {
  companion object {
    const val PLAYER_Z = 5.5f
    const val SPAWN_Z = 118f
    const val DESPAWN_Z = -18f
    const val TRAIN_ROOF_Y = 1.18f
    const val JETPACK_Y = 2.65f
    const val GRAVITY = -36f
    const val JUMP_VELOCITY = 13.8f
    const val SUPER_JUMP_VELOCITY = 18.2f
  }

  private var phase = GamePhase.MENU_IDLE
  private var targetLane = 0
  private var visualLane = 0f
  private var playerY = 0f
  private var playerVy = 0f
  private var groundHeight = 0f
  private var slideTimer = 0f
  private var runCycle = 0f
  private var tiltAngle = 0f
  private var squashStretch = 1f
  private var worldScroll = 0f
  private var speed = 26f
  private var distance = 0f
  private var scoreAccum = 0.0
  private var runCoins = 0L

  private var magnetTimer = 0f
  private var jetpackTimer = 0f
  private var multiplier2xTimer = 0f
  private var sneakersTimer = 0f
  private var hoverboardTimer = 0f
  private var invulnTimer = 0f

  private var magnetDuration = 8f
  private var jetpackDuration = 6f
  private var multiplier2xDuration = 10f
  private var sneakersDuration = 9f
  private var baseMultiplier = 1
  private var availableHoverboards = 3L
  private var hoverboardsUsed = 0L

  private var chaserDistance = 0.35f
  private var stumbleFlash = 0f
  private var screenShake = 0f

  private var nextEntityId = 1
  private var nextChunkZ = 42f
  private var scoreAccumulatorForCoins = 0

  private val obstacles = mutableListOf<TrackObstacle>()
  private val coins = mutableListOf<TrackCoin>()
  private val powerUps = mutableListOf<TrackPowerUp>()
  private val particles = mutableListOf<GameParticle>()
  private val callouts = mutableListOf<FloatingCallout>()

  private var profileRef = PlayerProfile()

  fun syncProfile(profile: PlayerProfile) {
    profileRef = profile
    baseMultiplier = profile.scoreMultiplierLevel.toInt().coerceIn(1, 6)
    magnetDuration = 7f + (profile.magnetLevel - 1) * 2.0f
    jetpackDuration = 5.5f + (profile.jetpackLevel - 1) * 1.4f
    sneakersDuration = 8f + (profile.sneakersLevel - 1) * 2.0f
    if (phase == GamePhase.MENU_IDLE) {
      availableHoverboards = profile.hoverboardCount
    }
  }

  fun startNewRun(profile: PlayerProfile) {
    syncProfile(profile)
    phase = GamePhase.RUNNING
    targetLane = 0
    visualLane = 0f
    playerY = 0f
    playerVy = 0f
    groundHeight = 0f
    slideTimer = 0f
    runCycle = 0f
    tiltAngle = 0f
    squashStretch = 1f
    worldScroll = 0f
    speed = 26f
    distance = 0f
    scoreAccum = 0.0
    runCoins = 0L
    magnetTimer = 0f
    jetpackTimer = 0f
    multiplier2xTimer = 0f
    sneakersTimer = 0f
    hoverboardTimer = 0f
    invulnTimer = 0f
    availableHoverboards = profile.hoverboardCount
    hoverboardsUsed = 0L
    chaserDistance = 0.25f
    stumbleFlash = 0f
    screenShake = 0f
    nextChunkZ = 40f
    scoreAccumulatorForCoins = 0
    obstacles.clear()
    coins.clear()
    powerUps.clear()
    particles.clear()
    callouts.clear()

    // Initial welcoming coin line
    for (i in 0 until 6) {
      coins.add(TrackCoin(nextEntityId++, 0f, 22f + i * 3.0f, 0.25f))
    }
    spawnChunk(nextChunkZ)
  }

  fun resetToMenu(profile: PlayerProfile) {
    syncProfile(profile)
    phase = GamePhase.MENU_IDLE
    targetLane = 0
    visualLane = 0f
    playerY = 0f
    playerVy = 0f
    groundHeight = 0f
    slideTimer = 0f
    tiltAngle = 0f
    squashStretch = 1f
    obstacles.clear()
    coins.clear()
    powerUps.clear()
    particles.clear()
    callouts.clear()
    chaserDistance = 0.35f
  }

  fun pause() {
    if (phase == GamePhase.RUNNING) {
      phase = GamePhase.PAUSED
    }
  }

  fun resume() {
    if (phase == GamePhase.PAUSED) {
      phase = GamePhase.RUNNING
    }
  }

  fun swipeLeft(events: MutableList<GameEvent>) {
    if (phase != GamePhase.RUNNING) return
    if (targetLane > -1) {
      val candidate = targetLane - 1
      // Check if blocked laterally by a train while on ground
      val blockedBySideTrain = obstacles.any { obs ->
        obs.lane == candidate &&
          obs.type.isTrain &&
          PLAYER_Z + 1.1f > obs.z &&
          PLAYER_Z - 1.1f < obs.z + obs.type.lengthZ &&
          playerY < TRAIN_ROOF_Y - 0.18f
      }
      if (blockedBySideTrain && jetpackTimer <= 0f) {
        triggerStumble(events)
        return
      }
      targetLane = candidate
      tiltAngle = -18f
      events.add(GameEvent.LaneSwitched)
    }
  }

  fun swipeRight(events: MutableList<GameEvent>) {
    if (phase != GamePhase.RUNNING) return
    if (targetLane < 1) {
      val candidate = targetLane + 1
      val blockedBySideTrain = obstacles.any { obs ->
        obs.lane == candidate &&
          obs.type.isTrain &&
          PLAYER_Z + 1.1f > obs.z &&
          PLAYER_Z - 1.1f < obs.z + obs.type.lengthZ &&
          playerY < TRAIN_ROOF_Y - 0.18f
      }
      if (blockedBySideTrain && jetpackTimer <= 0f) {
        triggerStumble(events)
        return
      }
      targetLane = candidate
      tiltAngle = 18f
      events.add(GameEvent.LaneSwitched)
    }
  }

  fun swipeUp(events: MutableList<GameEvent>) {
    if (phase != GamePhase.RUNNING || jetpackTimer > 0f) return
    if (playerY <= groundHeight + 0.08f) {
      slideTimer = 0f
      playerVy = if (sneakersTimer > 0f) SUPER_JUMP_VELOCITY else JUMP_VELOCITY
      squashStretch = 1.24f
      spawnBurst(0.5f + visualLane * 0.18f, 0.78f, Color(0xFFB7B2D9), 6)
      events.add(GameEvent.Jumped)
    }
  }

  fun swipeDown(events: MutableList<GameEvent>) {
    if (phase != GamePhase.RUNNING || jetpackTimer > 0f) return
    if (playerY > groundHeight + 0.15f) {
      // Subway Surfers fast-fall slam!
      playerVy = -28f
    }
    slideTimer = 0.65f
    squashStretch = 0.74f
    events.add(GameEvent.Slid)
  }

  fun activateHoverboard(events: MutableList<GameEvent>): Boolean {
    if (phase != GamePhase.RUNNING) return false
    if (hoverboardTimer > 0f || jetpackTimer > 0f) return false
    if (availableHoverboards <= 0L) return false

    availableHoverboards -= 1L
    hoverboardsUsed += 1L
    hoverboardTimer = 15f
    spawnBurst(0.5f + visualLane * 0.18f, 0.76f, Color(0xFF00E5FF), 14)
    addCallout("KAYKAY AKTİF!", 0.5f, 0.52f, Color(0xFF00E5FF))
    events.add(GameEvent.HoverboardActivated)
    return true
  }

  fun update(dtRaw: Float): List<GameEvent> {
    val events = mutableListOf<GameEvent>()
    val dt = dtRaw.coerceIn(0.001f, 0.045f)

    if (phase == GamePhase.MENU_IDLE) {
      runCycle += dt * 5.2f
      worldScroll += dt * 4.0f
      return events
    }

    if (phase != GamePhase.RUNNING) {
      updateParticlesAndCallouts(dt)
      return events
    }

    // Speed ramp
    val targetBaseSpeed = (26f + (distance / 42f)).coerceAtMost(54f)
    val jetpackBoost = if (jetpackTimer > 0f) 15f else 0f
    speed += ((targetBaseSpeed + jetpackBoost) - speed) * (dt * 3f)

    val dz = speed * dt
    worldScroll += dz
    distance += dz * 0.36f

    val mult = currentMultiplier()
    scoreAccum += dz * 1.45 * mult

    // Timers
    if (slideTimer > 0f) slideTimer = (slideTimer - dt).coerceAtLeast(0f)
    if (magnetTimer > 0f) magnetTimer = (magnetTimer - dt).coerceAtLeast(0f)
    if (jetpackTimer > 0f) jetpackTimer = (jetpackTimer - dt).coerceAtLeast(0f)
    if (multiplier2xTimer > 0f) multiplier2xTimer = (multiplier2xTimer - dt).coerceAtLeast(0f)
    if (sneakersTimer > 0f) sneakersTimer = (sneakersTimer - dt).coerceAtLeast(0f)
    if (hoverboardTimer > 0f) hoverboardTimer = (hoverboardTimer - dt).coerceAtLeast(0f)
    if (invulnTimer > 0f) invulnTimer = (invulnTimer - dt).coerceAtLeast(0f)
    if (stumbleFlash > 0f) stumbleFlash = (stumbleFlash - dt * 2.5f).coerceAtLeast(0f)
    if (screenShake > 0f) screenShake = (screenShake - dt * 2.8f).coerceAtLeast(0f)

    // Chaser retreats gradually when running clean
    if (chaserDistance < 1.0f) {
      chaserDistance = (chaserDistance + dt * 0.09f).coerceAtMost(1.0f)
    }

    // Lane smoothing & tilt
    val laneDiff = targetLane.toFloat() - visualLane
    visualLane += laneDiff * (dt * 16f).coerceAtMost(1f)
    tiltAngle += (0f - tiltAngle) * (dt * 11f).coerceAtMost(1f)
    squashStretch += (1f - squashStretch) * (dt * 12f).coerceAtMost(1f)
    runCycle += dt * (speed * 0.55f)

    // Compute groundHeight under player (track vs ramp vs train roof)
    val playerLaneRounded = when {
      visualLane < -0.5f -> -1
      visualLane > 0.5f -> 1
      else -> 0
    }
    var targetGround = 0f
    for (obs in obstacles) {
      if (obs.lane == playerLaneRounded && PLAYER_Z + 0.5f >= obs.z && PLAYER_Z - 0.5f <= obs.z + obs.type.lengthZ) {
        if (obs.type.isRamp) {
          val progress = ((PLAYER_Z - obs.z) / obs.type.lengthZ).coerceIn(0f, 1f)
          targetGround = maxOf(targetGround, progress * TRAIN_ROOF_Y)
        } else if (obs.type.isTrain && playerY >= TRAIN_ROOF_Y - 0.25f) {
          targetGround = maxOf(targetGround, TRAIN_ROOF_Y)
        }
      }
    }
    groundHeight = targetGround

    // Vertical physics
    if (jetpackTimer > 0f) {
      playerY += (JETPACK_Y - playerY) * (dt * 7f).coerceAtMost(1f)
      playerVy = 0f
      if (random.nextFloat() < 0.45f) {
        spawnBurst(0.5f + visualLane * 0.16f, 0.38f, Color(0xFFFF6D00), 1)
      }
    } else {
      val wasInAir = playerY > groundHeight + 0.05f
      playerVy += GRAVITY * dt
      playerY += playerVy * dt
      if (playerY <= groundHeight) {
        playerY = groundHeight
        if (wasInAir && playerVy < -6f) {
          squashStretch = 0.80f
          spawnBurst(0.5f + visualLane * 0.17f, 0.78f, Color(0xFF90A4AE), 4)
        }
        playerVy = 0f
      }
    }

    // Move obstacles & spawn chunks
    val obsIter = obstacles.iterator()
    while (obsIter.hasNext()) {
      val obs = obsIter.next()
      obs.z -= (speed + obs.speedOffset) * dt
      if (obs.z + obs.type.lengthZ < DESPAWN_Z) {
        obsIter.remove()
      }
    }

    nextChunkZ -= dz
    while (nextChunkZ < SPAWN_Z) {
      nextChunkZ += spawnChunk(nextChunkZ)
    }

    // Move & collect coins (with Magnet pull!)
    val coinIter = coins.iterator()
    while (coinIter.hasNext()) {
      val coin = coinIter.next()
      coin.z -= dz
      if (!coin.collected) {
        if (magnetTimer > 0f && coin.z in (PLAYER_Z - 4f)..(PLAYER_Z + 26f)) {
          coin.lane += (visualLane - coin.lane) * (dt * 11f).coerceAtMost(1f)
          coin.yOffset += ((playerY + 0.25f) - coin.yOffset) * (dt * 11f).coerceAtMost(1f)
          coin.z += (PLAYER_Z - coin.z) * (dt * 7f).coerceAtMost(1f)
        }

        val dx = abs(coin.lane - visualLane)
        val dzCoin = abs(coin.z - PLAYER_Z)
        val dyCoin = abs(coin.yOffset - (playerY + if (slideTimer > 0f) 0.15f else 0.35f))
        if (dx < 0.58f && dzCoin < 2.2f && dyCoin < 0.95f) {
          coin.collected = true
          runCoins += 1L
          scoreAccum += 15.0 * mult
          scoreAccumulatorForCoins++
          spawnBurst(0.5f + coin.lane * 0.17f, 0.72f - coin.yOffset * 0.13f, Color(0xFFFFD600), 4)
          events.add(GameEvent.CoinCollected)
        }
      }
      if (coin.collected || coin.z < DESPAWN_Z) {
        coinIter.remove()
      }
    }

    // Move & collect power-ups
    val puIter = powerUps.iterator()
    while (puIter.hasNext()) {
      val pu = puIter.next()
      pu.z -= dz
      if (!pu.collected) {
        val dx = abs(pu.lane.toFloat() - visualLane)
        val dzPu = abs(pu.z - PLAYER_Z)
        val dyPu = abs(pu.yOffset - (playerY + 0.3f))
        if (dx < 0.55f && dzPu < 2.3f && dyPu < 1.0f) {
          pu.collected = true
          applyPowerUp(pu.type)
          events.add(GameEvent.PowerUpPicked(pu.type))
        }
      }
      if (pu.collected || pu.z < DESPAWN_Z) {
        puIter.remove()
      }
    }

    // Obstacle collision detection
    if (jetpackTimer <= 0f && invulnTimer <= 0f) {
      checkObstacleCollisions(events)
    }

    updateParticlesAndCallouts(dt)
    return events
  }

  private fun checkObstacleCollisions(events: MutableList<GameEvent>) {
    val playerBottom = playerY
    val playerTop = playerY + if (slideTimer > 0f) 0.40f else 0.95f

    val iterator = obstacles.iterator()
    while (iterator.hasNext()) {
      val obs = iterator.next()
      if (obs.type.isRamp) continue

      val laneDist = abs(obs.lane.toFloat() - visualLane)
      if (laneDist > 0.42f) continue

      val zStart = obs.z
      val zEnd = obs.z + obs.type.lengthZ
      if (PLAYER_Z + 0.85f < zStart || PLAYER_Z - 0.85f > zEnd) continue

      // Check vertical overlap
      val obsBottom = obs.type.bottomClearance
      val obsTop = obs.type.topHeight

      // If safely riding on train roof
      if (obs.type.isTrain && playerBottom >= obsTop - 0.22f) continue

      val verticalOverlap = playerTop > obsBottom + 0.08f && playerBottom < obsTop - 0.08f
      if (!verticalOverlap) continue

      // Collision occurred!
      if (hoverboardTimer > 0f) {
        // Hoverboard shield absorbs hit and blasts obstacle!
        hoverboardTimer = 0f
        invulnTimer = 1.6f
        screenShake = 1.0f
        iterator.remove()
        obstacles.removeAll { abs(it.z - PLAYER_Z) < 18f }
        spawnBurst(0.5f + visualLane * 0.17f, 0.72f, Color(0xFF00E5FF), 22)
        addCallout("KAYKAY KORUDU!", 0.5f, 0.45f, Color(0xFF00E5FF))
        events.add(GameEvent.HoverboardShattered)
        return
      }

      // Brush/stumble if glancing blow on side or jump/slide barrier vs lethal train front
      val isGlancingBlow = !obs.type.isTrain && laneDist > 0.26f
      if (isGlancingBlow && chaserDistance > 0.48f) {
        iterator.remove()
        triggerStumble(events)
        return
      }

      // Fatal crash!
      phase = GamePhase.GAME_OVER
      screenShake = 1.2f
      chaserDistance = 0.0f
      spawnBurst(0.5f + visualLane * 0.17f, 0.72f, Color(0xFFFF3B5C), 24)
      events.add(
        GameEvent.Crashed(
          finalScore = scoreAccum.toLong(),
          distanceMeters = distance.toLong(),
          coinsCollected = runCoins,
          hoverboardsUsed = hoverboardsUsed,
        )
      )
      return
    }
  }

  private fun triggerStumble(events: MutableList<GameEvent>) {
    stumbleFlash = 1f
    screenShake = 0.65f
    invulnTimer = 0.75f
    if (chaserDistance < 0.35f) {
      // Caught by the angry chef!
      phase = GamePhase.GAME_OVER
      chaserDistance = 0f
      addCallout("ŞEF YAKALADI!", 0.5f, 0.42f, Color(0xFFFF3B5C))
      events.add(
        GameEvent.Crashed(
          finalScore = scoreAccum.toLong(),
          distanceMeters = distance.toLong(),
          coinsCollected = runCoins,
          hoverboardsUsed = hoverboardsUsed,
        )
      )
    } else {
      chaserDistance = 0.18f
      addCallout("DİKKAT! ŞEF PEŞİNDE!", 0.5f, 0.45f, Color(0xFFFF6D00))
      events.add(GameEvent.Stumbled)
    }
  }

  private fun applyPowerUp(type: PowerUpType) {
    when (type) {
      PowerUpType.MAGNET -> magnetTimer = magnetDuration
      PowerUpType.JETPACK -> {
        jetpackTimer = jetpackDuration
        // Spawn a sky trail of bonus coins!
        val skyLane = targetLane
        for (i in 0 until 14) {
          coins.add(TrackCoin(nextEntityId++, skyLane.toFloat(), PLAYER_Z + 12f + i * 3.8f, JETPACK_Y + 0.1f))
        }
      }
      PowerUpType.MULTIPLIER_2X -> multiplier2xTimer = multiplier2xDuration
      PowerUpType.SNEAKERS -> sneakersTimer = sneakersDuration
    }
    spawnBurst(0.5f + visualLane * 0.17f, 0.68f, type.accentColor, 14)
    addCallout(type.labelTr.uppercase() + "!", 0.5f, 0.44f, type.accentColor)
  }

  private fun spawnChunk(startZ: Float): Float {
    val pattern = random.nextInt(5)
    val lanes = listOf(-1, 0, 1).shuffled(random)
    val safeLane = lanes[0]
    val laneB = lanes[1]
    val laneC = lanes[2]

    when (pattern) {
      0 -> {
        // Train with Ramp climb + side barriers
        obstacles.add(TrackObstacle(nextEntityId++, laneB, startZ, ObstacleType.TRAIN_RAMP))
        obstacles.add(TrackObstacle(nextEntityId++, laneB, startZ + 6.5f, ObstacleType.TRAIN_YELLOW))
        for (i in 0 until 6) {
          coins.add(TrackCoin(nextEntityId++, laneB.toFloat(), startZ + 7.5f + i * 2.8f, TRAIN_ROOF_Y + 0.35f))
        }
        obstacles.add(TrackObstacle(nextEntityId++, laneC, startZ + 4f, ObstacleType.BARRIER_JUMP))
        for (i in 0 until 5) {
          coins.add(TrackCoin(nextEntityId++, safeLane.toFloat(), startZ + i * 3.2f, 0.25f))
        }
        maybeSpawnPowerUp(safeLane, startZ + 16f, 0.35f)
        return 30f
      }
      1 -> {
        // Hurdle + Duck rhythm across lanes
        obstacles.add(TrackObstacle(nextEntityId++, safeLane, startZ + 2f, ObstacleType.BARRIER_JUMP))
        // Coin arc over jump barrier
        for (i in 0..4) {
          val t = (i - 2) / 2f
          val arcY = 0.25f + (1f - t * t) * 0.78f
          coins.add(TrackCoin(nextEntityId++, safeLane.toFloat(), startZ + i * 1.4f, arcY))
        }
        obstacles.add(TrackObstacle(nextEntityId++, laneB, startZ + 6f, ObstacleType.BARRIER_SLIDE))
        obstacles.add(TrackObstacle(nextEntityId++, laneC, startZ + 10f, ObstacleType.BARRIER_FULL))
        for (i in 0 until 4) {
          coins.add(TrackCoin(nextEntityId++, laneB.toFloat(), startZ + 12f + i * 2.8f, 0.25f))
        }
        return 25f
      }
      2 -> {
        // Oncoming train + parked train canyon
        obstacles.add(TrackObstacle(nextEntityId++, laneB, startZ, ObstacleType.TRAIN_TEAL, speedOffset = 6f))
        obstacles.add(TrackObstacle(nextEntityId++, laneC, startZ + 4f, ObstacleType.TRAIN_RED))
        for (i in 0 until 7) {
          coins.add(TrackCoin(nextEntityId++, safeLane.toFloat(), startZ + i * 3.0f, 0.25f))
        }
        obstacles.add(TrackObstacle(nextEntityId++, safeLane, startZ + 22f, ObstacleType.BARRIER_SLIDE))
        maybeSpawnPowerUp(safeLane, startZ + 24.5f, 0.25f)
        return 32f
      }
      3 -> {
        // Slalom barriers with diagonal coin weave
        obstacles.add(TrackObstacle(nextEntityId++, -1, startZ, ObstacleType.BARRIER_FULL))
        obstacles.add(TrackObstacle(nextEntityId++, 1, startZ + 8f, ObstacleType.BARRIER_FULL))
        obstacles.add(TrackObstacle(nextEntityId++, 0, startZ + 16f, ObstacleType.BARRIER_JUMP))
        for (i in 0 until 6) {
          val weaveLane = if (i < 3) 0f else -1f
          coins.add(TrackCoin(nextEntityId++, weaveLane, startZ + i * 3.0f, 0.25f))
        }
        return 26f
      }
      else -> {
        // Ramp onto long red train + adjacent teal train roof jump!
        obstacles.add(TrackObstacle(nextEntityId++, laneB, startZ, ObstacleType.TRAIN_RAMP))
        obstacles.add(TrackObstacle(nextEntityId++, laneB, startZ + 6.5f, ObstacleType.TRAIN_RED))
        obstacles.add(TrackObstacle(nextEntityId++, laneC, startZ + 14f, ObstacleType.TRAIN_TEAL))
        for (i in 0 until 5) {
          coins.add(TrackCoin(nextEntityId++, laneC.toFloat(), startZ + 16f + i * 2.8f, TRAIN_ROOF_Y + 0.35f))
        }
        obstacles.add(TrackObstacle(nextEntityId++, safeLane, startZ + 10f, ObstacleType.BARRIER_JUMP))
        maybeSpawnPowerUp(safeLane, startZ + 18f, 0.35f)
        return 36f
      }
    }
  }

  private fun maybeSpawnPowerUp(lane: Int, z: Float, yOffset: Float) {
    if (random.nextFloat() < 0.28f) {
      val types = PowerUpType.entries
      val chosen = types[random.nextInt(types.size)]
      powerUps.add(TrackPowerUp(nextEntityId++, lane, z, yOffset, chosen))
    }
  }

  private fun spawnBurst(xNorm: Float, yNorm: Float, color: Color, count: Int) {
    for (i in 0 until count) {
      val angle = random.nextFloat() * 6.283f
      val speedP = 0.15f + random.nextFloat() * 0.45f
      particles.add(
        GameParticle(
          xNorm = xNorm,
          yNorm = yNorm,
          vx = cos(angle) * speedP,
          vy = sin(angle) * speedP - 0.15f,
          life = 0.45f,
          maxLife = 0.45f,
          color = color,
          radiusDp = 3f + random.nextFloat() * 4f,
        )
      )
    }
  }

  private fun addCallout(text: String, xNorm: Float, yNorm: Float, color: Color) {
    callouts.add(
      FloatingCallout(
        id = nextEntityId++,
        text = text,
        xNorm = xNorm,
        yNorm = yNorm,
        life = 1.15f,
        maxLife = 1.15f,
        color = color,
      )
    )
  }

  private fun updateParticlesAndCallouts(dt: Float) {
    val pIter = particles.iterator()
    while (pIter.hasNext()) {
      val p = pIter.next()
      p.life -= dt
      p.xNorm += p.vx * dt
      p.yNorm += p.vy * dt
      p.vy += 0.9f * dt
      if (p.life <= 0f) pIter.remove()
    }
    val cIter = callouts.iterator()
    while (cIter.hasNext()) {
      val c = cIter.next()
      c.life -= dt
      c.yNorm -= 0.12f * dt
      if (c.life <= 0f) cIter.remove()
    }
  }

  private fun currentMultiplier(): Int {
    return baseMultiplier * (if (multiplier2xTimer > 0f) 2 else 1)
  }

  fun snapshot(): GameSnapshot = GameSnapshot(
    phase = phase,
    visualLane = visualLane,
    targetLane = targetLane,
    playerY = playerY,
    groundHeight = groundHeight,
    isSliding = slideTimer > 0f,
    isJumping = playerY > groundHeight + 0.08f && jetpackTimer <= 0f,
    isOnJetpack = jetpackTimer > 0f,
    runCycle = runCycle,
    tiltAngle = tiltAngle,
    squashStretch = squashStretch,
    worldScroll = worldScroll,
    speed = speed,
    distanceMeters = distance.toLong(),
    score = scoreAccum.toLong(),
    runCoins = runCoins,
    activeMultiplier = currentMultiplier(),
    magnetRemaining = magnetTimer,
    magnetMax = magnetDuration,
    jetpackRemaining = jetpackTimer,
    jetpackMax = jetpackDuration,
    multiplier2xRemaining = multiplier2xTimer,
    multiplier2xMax = multiplier2xDuration,
    sneakersRemaining = sneakersTimer,
    sneakersMax = sneakersDuration,
    hoverboardRemaining = hoverboardTimer,
    hoverboardMax = 15f,
    chaserDistance = chaserDistance,
    stumbleFlash = stumbleFlash,
    screenShake = screenShake,
    obstacles = obstacles.toList(),
    coins = coins.toList(),
    powerUps = powerUps.toList(),
    particles = particles.toList(),
    callouts = callouts.toList(),
    availableHoverboards = availableHoverboards,
  )
}
