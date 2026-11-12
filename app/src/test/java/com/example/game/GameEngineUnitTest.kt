package com.example.game

import com.example.data.model.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineUnitTest {

  @Test
  fun swipeLeftAndRight_updatesTargetLaneWithinBounds() {
    val engine = GameEngine(Random(1))
    engine.startNewRun(PlayerProfile())
    val events = mutableListOf<GameEvent>()

    engine.swipeLeft(events)
    assertEquals(-1, engine.snapshot().targetLane)
    engine.swipeLeft(events)
    assertEquals(-1, engine.snapshot().targetLane)

    engine.swipeRight(events)
    assertEquals(0, engine.snapshot().targetLane)
    engine.swipeRight(events)
    assertEquals(1, engine.snapshot().targetLane)
    engine.swipeRight(events)
    assertEquals(1, engine.snapshot().targetLane)
  }

  @Test
  fun swipeUpAndDown_triggersJumpAndSlide() {
    val engine = GameEngine(Random(2))
    engine.startNewRun(PlayerProfile())
    val events = mutableListOf<GameEvent>()

    engine.swipeUp(events)
    engine.update(0.03f)
    assertTrue(engine.snapshot().playerY > 0f)
    assertTrue(events.contains(GameEvent.Jumped))

    engine.swipeDown(events)
    assertTrue(engine.snapshot().isSliding)
    assertTrue(events.contains(GameEvent.Slid))
  }

  @Test
  fun activateHoverboard_consumesChargeAndStartsTimer() {
    val engine = GameEngine(Random(3))
    engine.startNewRun(PlayerProfile(hoverboardCount = 2L))
    val events = mutableListOf<GameEvent>()

    val activated = engine.activateHoverboard(events)
    assertTrue(activated)
    assertEquals(1L, engine.snapshot().availableHoverboards)
    assertTrue(engine.snapshot().hoverboardRemaining > 0f)
  }
}
