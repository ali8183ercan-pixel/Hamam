package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.data.model.HoverboardSkin
import com.example.data.model.RoachAccessory
import com.example.data.model.RoachSkin
import com.example.ui.theme.BalooDisplayFontFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private data class ProjectedPoint(
  val x: Float,
  val y: Float,
  val scale: Float,
)

private fun projectWorld(
  lane: Float,
  worldY: Float,
  z: Float,
  width: Float,
  height: Float,
): ProjectedPoint {
  val horizonY = height * 0.25f
  val groundBottomY = height * 0.94f
  val depthFactor = (11.5f / (z + 8.5f)).coerceIn(0.04f, 2.4f)
  val roadWidthAtZ = width * (0.14f + 0.96f * depthFactor)
  val laneSpacing = roadWidthAtZ * 0.30f
  val screenX = width * 0.5f + lane * laneSpacing
  val groundY = horizonY + (groundBottomY - horizonY) * depthFactor
  val heightScale = height * 0.22f * depthFactor
  val screenY = groundY - worldY * heightScale
  return ProjectedPoint(screenX, screenY, depthFactor)
}

@Composable
fun SubwayGameCanvas(
  snapshot: GameSnapshot,
  roachSkin: RoachSkin,
  boardSkin: HoverboardSkin,
  modifier: Modifier = Modifier,
) {
  val textMeasurer = rememberTextMeasurer()

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    val shakeX = if (snapshot.screenShake > 0f) {
      sin(snapshot.worldScroll * 5f) * snapshot.screenShake * 14f
    } else {
      0f
    }
    val shakeY = if (snapshot.screenShake > 0f) {
      cos(snapshot.worldScroll * 7f) * snapshot.screenShake * 10f
    } else {
      0f
    }

    drawContext.transform.translate(shakeX, shakeY)

    // 1. Tunnel Sky & Neon Horizon
    drawTunnelBackdrop(w, h, snapshot.worldScroll, snapshot.speed)

    // 2. Subway Track Bed, Sleepers & Steel Rails
    drawSubwayTracks(w, h, snapshot.worldScroll)

    // 3. Draw sorted 3D entities far-to-near (painter's algorithm)
    val renderItems = buildList {
      snapshot.obstacles.forEach { add(it.z to it) }
      snapshot.coins.forEach { add(it.z to it) }
      snapshot.powerUps.forEach { add(it.z to it) }
    }.sortedByDescending { it.first }

    // Draw player in depth order relative to obstacles
    var playerDrawn = false
    for ((entityZ, item) in renderItems) {
      if (!playerDrawn && entityZ <= GameEngine.PLAYER_Z) {
        drawPlayerAndChaser(snapshot, roachSkin, boardSkin, w, h)
        playerDrawn = true
      }
      when (item) {
        is TrackObstacle -> drawObstacle(item, w, h)
        is TrackCoin -> drawCoin(item, snapshot.worldScroll, w, h)
        is TrackPowerUp -> drawPowerUp(item, snapshot.worldScroll, w, h)
      }
    }
    if (!playerDrawn) {
      drawPlayerAndChaser(snapshot, roachSkin, boardSkin, w, h)
    }

    // 4. Particles
    for (p in snapshot.particles) {
      val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
      drawCircle(
        color = p.color.copy(alpha = alpha),
        radius = p.radiusDp * density,
        center = Offset(p.xNorm * w, p.yNorm * h),
      )
    }

    // 5. Floating Callouts
    for (callout in snapshot.callouts) {
      drawCallout(textMeasurer, callout, w, h)
    }

    // 6. Stumble flash vignette
    if (snapshot.stumbleFlash > 0f) {
      drawRect(
        color = Color(0xFFFF3B5C).copy(alpha = snapshot.stumbleFlash * 0.28f),
        size = size,
      )
    }
  }
}

private fun DrawScope.drawTunnelBackdrop(
  w: Float,
  h: Float,
  scroll: Float,
  speed: Float,
) {
  val horizonY = h * 0.25f

  // Upper tunnel / night neon glow
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF0B071C),
        Color(0xFF1A123E),
        Color(0xFF2B1858),
      ),
      startY = 0f,
      endY = horizonY + 40f,
    ),
    size = Size(w, horizonY + 40f),
  )

  // Glowing neon horizon portal
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xFF00E5FF).copy(alpha = 0.45f),
        Color(0xFFFF2A85).copy(alpha = 0.18f),
        Color.Transparent,
      ),
      center = Offset(w * 0.5f, horizonY),
      radius = w * 0.45f,
    ),
    radius = w * 0.45f,
    center = Offset(w * 0.5f, horizonY),
  )

  // Subway ground gravel gradient
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF1A1434),
        Color(0xFF211C3F),
        Color(0xFF151228),
      ),
      startY = horizonY,
      endY = h,
    ),
    topLeft = Offset(0f, horizonY),
    size = Size(w, h - horizonY),
  )

  // Perspective brick/graffiti side walls
  val leftWall = Path().apply {
    moveTo(0f, 0f)
    lineTo(w * 0.36f, horizonY)
    lineTo(0f, h * 0.90f)
    close()
  }
  drawPath(
    path = leftWall,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF28184B), Color(0xFF16102F)),
      start = Offset(0f, 0f),
      end = Offset(w * 0.36f, horizonY),
    ),
  )

  val rightWall = Path().apply {
    moveTo(w, 0f)
    lineTo(w * 0.64f, horizonY)
    lineTo(w, h * 0.90f)
    close()
  }
  drawPath(
    path = rightWall,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF28184B), Color(0xFF16102F)),
      start = Offset(w, 0f),
      end = Offset(w * 0.64f, horizonY),
    ),
  )

  // Moving neon tunnel pillars & graffiti stripes
  val pillarPeriod = 14f
  val phase = scroll % pillarPeriod
  val neonPalette = listOf(Color(0xFF00E5FF), Color(0xFFFF6D00), Color(0xFFFF2A85), Color(0xFF76FF03))
  for (i in 0..8) {
    val z = (i * pillarPeriod) - phase + 4f
    if (z <= 1f) continue
    val pLeft = projectWorld(-1.95f, 0f, z, w, h)
    val pLeftTop = projectWorld(-1.95f, 2.3f, z, w, h)
    val pRight = projectWorld(1.95f, 0f, z, w, h)
    val pRightTop = projectWorld(1.95f, 2.3f, z, w, h)
    val strokeW = (12f * pLeft.scale).coerceIn(2f, 22f)
    val accent = neonPalette[(i + (scroll / pillarPeriod).toInt()) and 3]

    drawLine(Color(0xFF392B6B), Offset(pLeft.x, pLeft.y), Offset(pLeftTop.x, pLeftTop.y), strokeWidth = strokeW)
    drawLine(accent.copy(alpha = 0.75f), Offset(pLeft.x, pLeft.y - (pLeft.y - pLeftTop.y) * 0.25f), Offset(pLeftTop.x, pLeftTop.y), strokeWidth = strokeW * 0.35f)

    drawLine(Color(0xFF392B6B), Offset(pRight.x, pRight.y), Offset(pRightTop.x, pRightTop.y), strokeWidth = strokeW)
    drawLine(accent.copy(alpha = 0.75f), Offset(pRight.x, pRight.y - (pRight.y - pRightTop.y) * 0.25f), Offset(pRightTop.x, pRightTop.y), strokeWidth = strokeW * 0.35f)

    // Overhead tunnel arch beam
    drawLine(
      color = Color(0xFF2B2054),
      start = Offset(pLeftTop.x, pLeftTop.y),
      end = Offset(pRightTop.x, pRightTop.y),
      strokeWidth = strokeW * 0.7f,
    )
    // Ceiling neon strip
    drawLine(
      color = accent.copy(alpha = 0.6f),
      start = Offset((pLeftTop.x + pRightTop.x) * 0.5f - 28f * pLeft.scale, pLeftTop.y),
      end = Offset((pLeftTop.x + pRightTop.x) * 0.5f + 28f * pLeft.scale, pRightTop.y),
      strokeWidth = (5f * pLeft.scale).coerceAtLeast(1.5f),
      cap = StrokeCap.Round,
    )
  }

  // Speed lines at high speed
  if (speed > 34f) {
    val intensity = ((speed - 34f) / 20f).coerceIn(0f, 1f)
    for (s in 0..5) {
      val offset = ((scroll * 2.2f + s * 17f) % 100f) / 100f
      val xSide = if (s % 2 == 0) w * (0.04f + s * 0.03f) else w * (0.96f - s * 0.03f)
      val yStart = h * 0.15f + offset * h * 0.6f
      drawLine(
        color = Color(0xFF00E5FF).copy(alpha = 0.18f * intensity),
        start = Offset(xSide, yStart),
        end = Offset(xSide, yStart + 55f * intensity),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round,
      )
    }
  }
}

private fun DrawScope.drawSubwayTracks(
  w: Float,
  h: Float,
  scroll: Float,
) {
  val farZ = 115f
  val nearZ = 1.5f

  // 3 track gravel beds
  for (lane in -1..1) {
    val fL = projectWorld(lane - 0.42f, 0f, farZ, w, h)
    val fR = projectWorld(lane + 0.42f, 0f, farZ, w, h)
    val nL = projectWorld(lane - 0.42f, 0f, nearZ, w, h)
    val nR = projectWorld(lane + 0.42f, 0f, nearZ, w, h)

    val bed = Path().apply {
      moveTo(fL.x, fL.y)
      lineTo(fR.x, fR.y)
      lineTo(nR.x, nR.y)
      lineTo(nL.x, nL.y)
      close()
    }
    drawPath(bed, color = Color(0xFF28224A))
  }

  // Scrolling wooden sleepers
  val sleeperStep = 3.2f
  val sleeperPhase = scroll % sleeperStep
  for (i in 0..34) {
    val z = i * sleeperStep - sleeperPhase + 1.5f
    if (z < 1.5f) continue
    for (lane in -1..1) {
      val left = projectWorld(lane - 0.36f, 0f, z, w, h)
      val right = projectWorld(lane + 0.36f, 0f, z, w, h)
      drawLine(
        color = Color(0xFF53392D),
        start = Offset(left.x, left.y),
        end = Offset(right.x, right.y),
        strokeWidth = (8.5f * left.scale).coerceIn(1.2f, 14f),
      )
    }
  }

  // Steel rails (2 per lane)
  for (lane in -1..1) {
    for (railOffset in listOf(-0.26f, 0.26f)) {
      val far = projectWorld(lane + railOffset, 0f, farZ, w, h)
      val near = projectWorld(lane + railOffset, 0f, nearZ, w, h)
      drawLine(
        color = Color(0xFF90A4AE),
        start = Offset(far.x, far.y),
        end = Offset(near.x, near.y),
        strokeWidth = 4.5f,
      )
      drawLine(
        color = Color(0xFFE0F7FA).copy(alpha = 0.55f),
        start = Offset(far.x, far.y),
        end = Offset(near.x, near.y),
        strokeWidth = 1.5f,
      )
    }
  }
}

private fun DrawScope.drawObstacle(
  obs: TrackObstacle,
  w: Float,
  h: Float,
) {
  val zFront = obs.z.coerceAtLeast(1.2f)
  val zBack = (obs.z + obs.type.lengthZ).coerceAtLeast(1.5f)
  if (zFront > 116f) return

  when {
    obs.type.isTrain -> drawTrain(obs, zFront, zBack, w, h)
    obs.type.isRamp -> drawRamp(obs.lane, zFront, zBack, w, h)
    obs.type == ObstacleType.BARRIER_JUMP -> drawJumpBarrier(obs.lane, zFront, w, h)
    obs.type == ObstacleType.BARRIER_SLIDE -> drawSlideBarrier(obs.lane, zFront, w, h)
    obs.type == ObstacleType.BARRIER_FULL -> drawFullBarrier(obs.lane, zFront, zBack, w, h)
  }
}

private fun DrawScope.drawTrain(
  obs: TrackObstacle,
  zFront: Float,
  zBack: Float,
  w: Float,
  h: Float,
) {
  val (bodyMain, bodyTrim, roofColor) = when (obs.type) {
    ObstacleType.TRAIN_YELLOW -> Triple(Color(0xFFFFB300), Color(0xFF00BFA5), Color(0xFFFFE082))
    ObstacleType.TRAIN_TEAL -> Triple(Color(0xFF00ACC1), Color(0xFFFF6D00), Color(0xFF80DEEA))
    else -> Triple(Color(0xFFE53935), Color(0xFFFFD600), Color(0xFFFF8A80))
  }

  val halfW = 0.39f
  val topY = obs.type.topHeight

  val fBL = projectWorld(obs.lane - halfW, 0f, zFront, w, h)
  val fBR = projectWorld(obs.lane + halfW, 0f, zFront, w, h)
  val fTL = projectWorld(obs.lane - halfW, topY, zFront, w, h)
  val fTR = projectWorld(obs.lane + halfW, topY, zFront, w, h)

  val bTL = projectWorld(obs.lane - halfW, topY, zBack, w, h)
  val bTR = projectWorld(obs.lane + halfW, topY, zBack, w, h)
  val bBL = projectWorld(obs.lane - halfW, 0f, zBack, w, h)
  val bBR = projectWorld(obs.lane + halfW, 0f, zBack, w, h)

  // Visible side wall if train is in left or right lane
  if (obs.lane < 0) {
    val sidePath = Path().apply {
      moveTo(fBR.x, fBR.y)
      lineTo(bBR.x, bBR.y)
      lineTo(bTR.x, bTR.y)
      lineTo(fTR.x, fTR.y)
      close()
    }
    drawPath(sidePath, color = bodyMain.copy(alpha = 0.85f))
  } else if (obs.lane > 0) {
    val sidePath = Path().apply {
      moveTo(fBL.x, fBL.y)
      lineTo(bBL.x, bBL.y)
      lineTo(bTL.x, bTL.y)
      lineTo(fTL.x, fTL.y)
      close()
    }
    drawPath(sidePath, color = bodyMain.copy(alpha = 0.85f))
  }

  // Roof top surface
  val roofPath = Path().apply {
    moveTo(fTL.x, fTL.y)
    lineTo(fTR.x, fTR.y)
    lineTo(bTR.x, bTR.y)
    lineTo(bTL.x, bTL.y)
    close()
  }
  drawPath(roofPath, color = roofColor)
  drawPath(roofPath, color = Color.Black.copy(alpha = 0.18f), style = Stroke(width = 2f))

  // Front face of the train
  val faceW = fBR.x - fBL.x
  val faceH = fBL.y - fTL.y
  if (faceW > 2f && faceH > 2f) {
    drawRoundRect(
      color = bodyMain,
      topLeft = Offset(fTL.x, fTL.y),
      size = Size(faceW, faceH),
      cornerRadius = CornerRadius(faceW * 0.12f, faceW * 0.12f),
    )

    // Accent stripe across middle
    drawRect(
      color = bodyTrim,
      topLeft = Offset(fTL.x, fTL.y + faceH * 0.56f),
      size = Size(faceW, faceH * 0.16f),
    )

    // Dark windshield
    drawRoundRect(
      color = Color(0xFF14182D),
      topLeft = Offset(fTL.x + faceW * 0.12f, fTL.y + faceH * 0.14f),
      size = Size(faceW * 0.76f, faceH * 0.34f),
      cornerRadius = CornerRadius(faceW * 0.07f, faceW * 0.07f),
    )
    // Windshield center divider
    drawLine(
      color = bodyMain,
      start = Offset(fTL.x + faceW * 0.5f, fTL.y + faceH * 0.14f),
      end = Offset(fTL.x + faceW * 0.5f, fTL.y + faceH * 0.48f),
      strokeWidth = (faceW * 0.05f).coerceAtLeast(1.5f),
    )

    // Twin glowing headlights
    val lightR = (faceW * 0.08f).coerceAtLeast(2f)
    drawCircle(
      color = Color(0xFFFFF59D),
      radius = lightR,
      center = Offset(fTL.x + faceW * 0.24f, fTL.y + faceH * 0.80f),
    )
    drawCircle(
      color = Color(0xFFFFF59D),
      radius = lightR,
      center = Offset(fTL.x + faceW * 0.76f, fTL.y + faceH * 0.80f),
    )
  }
}

private fun DrawScope.drawRamp(
  lane: Int,
  zFront: Float,
  zBack: Float,
  w: Float,
  h: Float,
) {
  val halfW = 0.36f
  val fL = projectWorld(lane - halfW, 0f, zFront, w, h)
  val fR = projectWorld(lane + halfW, 0f, zFront, w, h)
  val bTL = projectWorld(lane - halfW, GameEngine.TRAIN_ROOF_Y, zBack, w, h)
  val bTR = projectWorld(lane + halfW, GameEngine.TRAIN_ROOF_Y, zBack, w, h)

  val rampPath = Path().apply {
    moveTo(fL.x, fL.y)
    lineTo(fR.x, fR.y)
    lineTo(bTR.x, bTR.y)
    lineTo(bTL.x, bTL.y)
    close()
  }
  drawPath(
    path = rampPath,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFFFFD600), Color(0xFFFF6D00)),
      startY = bTL.y,
      endY = fL.y,
    ),
  )
  drawPath(rampPath, color = Color(0xFF1B1543), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawJumpBarrier(
  lane: Int,
  zFront: Float,
  w: Float,
  h: Float,
) {
  val halfW = 0.38f
  val bL = projectWorld(lane - halfW, 0f, zFront, w, h)
  val bR = projectWorld(lane + halfW, 0f, zFront, w, h)
  val tL = projectWorld(lane - halfW, 0.56f, zFront, w, h)
  val tR = projectWorld(lane + halfW, 0.56f, zFront, w, h)

  val barW = bR.x - bL.x
  val barH = bL.y - tL.y
  if (barW < 3f) return

  // Legs
  drawLine(Color(0xFFCFD8DC), Offset(bL.x + barW * 0.14f, bL.y), Offset(tL.x + barW * 0.14f, tL.y), strokeWidth = (barW * 0.08f).coerceAtLeast(2f))
  drawLine(Color(0xFFCFD8DC), Offset(bR.x - barW * 0.14f, bR.y), Offset(tR.x - barW * 0.14f, tR.y), strokeWidth = (barW * 0.08f).coerceAtLeast(2f))

  // Top hazard plank (jump over)
  drawRoundRect(
    color = Color(0xFFFF3B5C),
    topLeft = Offset(tL.x, tL.y),
    size = Size(barW, barH * 0.45f),
    cornerRadius = CornerRadius(6f, 6f),
  )
  // White caution stripes
  for (s in 0..2) {
    drawRect(
      color = Color.White,
      topLeft = Offset(tL.x + barW * (0.14f + s * 0.30f), tL.y + barH * 0.06f),
      size = Size(barW * 0.14f, barH * 0.33f),
    )
  }
}

private fun DrawScope.drawSlideBarrier(
  lane: Int,
  zFront: Float,
  w: Float,
  h: Float,
) {
  val halfW = 0.40f
  val bL = projectWorld(lane - halfW, 0f, zFront, w, h)
  val bR = projectWorld(lane + halfW, 0f, zFront, w, h)
  val mL = projectWorld(lane - halfW, 0.54f, zFront, w, h)
  val tL = projectWorld(lane - halfW, 1.24f, zFront, w, h)
  val tR = projectWorld(lane + halfW, 1.24f, zFront, w, h)

  val barW = bR.x - bL.x
  if (barW < 3f) return

  // Tall side posts
  val postW = (barW * 0.09f).coerceAtLeast(2f)
  drawLine(Color(0xFFB0BEC5), Offset(bL.x + postW, bL.y), Offset(tL.x + postW, tL.y), strokeWidth = postW)
  drawLine(Color(0xFFB0BEC5), Offset(bR.x - postW, bR.y), Offset(tR.x - postW, tR.y), strokeWidth = postW)

  // Overhead sign beam (duck under!)
  val beamH = mL.y - tL.y
  drawRoundRect(
    color = Color(0xFFFF6D00),
    topLeft = Offset(tL.x, tL.y),
    size = Size(barW, beamH * 0.86f),
    cornerRadius = CornerRadius(8f, 8f),
  )
  drawRoundRect(
    color = Color(0xFF1B1543),
    topLeft = Offset(tL.x + barW * 0.08f, tL.y + beamH * 0.15f),
    size = Size(barW * 0.84f, beamH * 0.56f),
    cornerRadius = CornerRadius(5f, 5f),
  )
  // Downward neon chevrons signaling SLIDE!
  val cx = (tL.x + tR.x) * 0.5f
  val cy = tL.y + beamH * 0.43f
  val chev = Path().apply {
    moveTo(cx - barW * 0.16f, cy - beamH * 0.12f)
    lineTo(cx, cy + beamH * 0.15f)
    lineTo(cx + barW * 0.16f, cy - beamH * 0.12f)
  }
  drawPath(chev, color = Color(0xFF00E5FF), style = Stroke(width = (barW * 0.06f).coerceAtLeast(2f), cap = StrokeCap.Round))
}

private fun DrawScope.drawFullBarrier(
  lane: Int,
  zFront: Float,
  zBack: Float,
  w: Float,
  h: Float,
) {
  val halfW = 0.37f
  val bL = projectWorld(lane - halfW, 0f, zFront, w, h)
  val bR = projectWorld(lane + halfW, 0f, zFront, w, h)
  val tL = projectWorld(lane - halfW, 1.15f, zFront, w, h)
  val tR = projectWorld(lane + halfW, 1.15f, zFront, w, h)
  val backTL = projectWorld(lane - halfW, 1.15f, zBack, w, h)
  val backTR = projectWorld(lane + halfW, 1.15f, zBack, w, h)

  val boxW = bR.x - bL.x
  val boxH = bL.y - tL.y
  if (boxW < 3f) return

  val topPath = Path().apply {
    moveTo(tL.x, tL.y)
    lineTo(tR.x, tR.y)
    lineTo(backTR.x, backTR.y)
    lineTo(backTL.x, backTL.y)
    close()
  }
  drawPath(topPath, color = Color(0xFF455A64))

  // Roach-spray / dumpster crate front
  drawRoundRect(
    color = Color(0xFF2E3B4E),
    topLeft = Offset(tL.x, tL.y),
    size = Size(boxW, boxH),
    cornerRadius = CornerRadius(8f, 8f),
  )
  drawRect(
    color = Color(0xFF76FF03),
    topLeft = Offset(tL.x + boxW * 0.15f, tL.y + boxH * 0.25f),
    size = Size(boxW * 0.70f, boxH * 0.22f),
  )
}

private fun DrawScope.drawCoin(
  coin: TrackCoin,
  scroll: Float,
  w: Float,
  h: Float,
) {
  if (coin.z < 1.2f || coin.z > 115f) return
  val p = projectWorld(coin.lane, coin.yOffset, coin.z, w, h)
  val pGround = projectWorld(coin.lane, 0f, coin.z, w, h)
  val radius = (22f * p.scale).coerceIn(3f, 34f)
  val spinScale = abs(cos(scroll * 0.24f + coin.id * 0.6f)).coerceAtLeast(0.28f)

  // Shadow
  drawOval(
    color = Color.Black.copy(alpha = 0.28f),
    topLeft = Offset(pGround.x - radius * 0.7f, pGround.y - radius * 0.22f),
    size = Size(radius * 1.4f, radius * 0.44f),
  )

  // Coin rim & bright gold face
  drawOval(
    color = Color(0xFFFF8F00),
    topLeft = Offset(p.x - radius * spinScale, p.y - radius),
    size = Size(radius * 2f * spinScale, radius * 2f),
  )
  drawOval(
    color = Color(0xFFFFD600),
    topLeft = Offset(p.x - radius * 0.8f * spinScale, p.y - radius * 0.8f),
    size = Size(radius * 1.6f * spinScale, radius * 1.6f),
  )
  drawOval(
    color = Color(0xFFFFF59D),
    topLeft = Offset(p.x - radius * 0.3f * spinScale, p.y - radius * 0.45f),
    size = Size(radius * 0.55f * spinScale, radius * 0.55f),
  )
}

private fun DrawScope.drawPowerUp(
  pu: TrackPowerUp,
  scroll: Float,
  w: Float,
  h: Float,
) {
  if (pu.z < 1.2f || pu.z > 115f) return
  val bob = sin(scroll * 0.2f + pu.id) * 0.08f
  val p = projectWorld(pu.lane.toFloat(), pu.yOffset + bob, pu.z, w, h)
  val r = (30f * p.scale).coerceIn(5f, 44f)

  // Outer glowing aura
  drawCircle(
    color = pu.type.accentColor.copy(alpha = 0.30f),
    radius = r * 1.45f,
    center = Offset(p.x, p.y),
  )
  drawCircle(
    color = Color(0xFF151034),
    radius = r,
    center = Offset(p.x, p.y),
  )
  drawCircle(
    color = pu.type.accentColor,
    radius = r,
    center = Offset(p.x, p.y),
    style = Stroke(width = (r * 0.18f).coerceAtLeast(2f)),
  )

  when (pu.type) {
    PowerUpType.MAGNET -> {
      drawArc(
        color = Color(0xFFFF3B5C),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(p.x - r * 0.45f, p.y - r * 0.45f),
        size = Size(r * 0.9f, r * 0.9f),
        style = Stroke(width = r * 0.24f, cap = StrokeCap.Round),
      )
    }
    PowerUpType.JETPACK -> {
      val flame = Path().apply {
        moveTo(p.x, p.y - r * 0.55f)
        lineTo(p.x - r * 0.45f, p.y + r * 0.35f)
        lineTo(p.x + r * 0.45f, p.y + r * 0.35f)
        close()
      }
      drawPath(flame, color = Color(0xFFFF6D00))
    }
    PowerUpType.MULTIPLIER_2X -> {
      drawCircle(color = Color(0xFFFFD600), radius = r * 0.48f, center = Offset(p.x, p.y))
    }
    PowerUpType.SNEAKERS -> {
      drawRoundRect(
        color = Color(0xFF00E5FF),
        topLeft = Offset(p.x - r * 0.45f, p.y - r * 0.2f),
        size = Size(r * 0.9f, r * 0.45f),
        cornerRadius = CornerRadius(r * 0.2f, r * 0.2f),
      )
    }
  }
}

private fun DrawScope.drawPlayerAndChaser(
  snapshot: GameSnapshot,
  roachSkin: RoachSkin,
  boardSkin: HoverboardSkin,
  w: Float,
  h: Float,
) {
  // Draw Angry Chef chaser if close
  if (snapshot.chaserDistance < 0.88f) {
    val chaserZ = GameEngine.PLAYER_Z - (1.8f + snapshot.chaserDistance * 5.2f)
    if (chaserZ > 0.6f) {
      val cp = projectWorld(snapshot.visualLane * 0.92f, 0f, chaserZ, w, h)
      drawAngryChef(cp.x, cp.y, (68f * cp.scale).coerceIn(22f, 110f), snapshot.runCycle)
    }
  }

  val pGround = projectWorld(snapshot.visualLane, snapshot.groundHeight, GameEngine.PLAYER_Z, w, h)
  val pPlayer = projectWorld(snapshot.visualLane, snapshot.playerY, GameEngine.PLAYER_Z, w, h)
  val baseSize = (72f * pPlayer.scale).coerceIn(30f, 115f)

  // Ground shadow
  val heightAboveGround = (snapshot.playerY - snapshot.groundHeight).coerceAtLeast(0f)
  val shadowScale = (1f - heightAboveGround * 0.32f).coerceIn(0.35f, 1f)
  drawOval(
    color = Color.Black.copy(alpha = 0.42f * shadowScale),
    topLeft = Offset(pGround.x - baseSize * 0.65f * shadowScale, pGround.y - baseSize * 0.16f * shadowScale),
    size = Size(baseSize * 1.3f * shadowScale, baseSize * 0.32f * shadowScale),
  )

  rotate(degrees = snapshot.tiltAngle, pivot = Offset(pPlayer.x, pPlayer.y - baseSize * 0.5f)) {
    drawCockroachHero(
      centerX = pPlayer.x,
      bottomY = pPlayer.y,
      size = baseSize,
      roachSkin = roachSkin,
      boardSkin = boardSkin,
      runCycle = snapshot.runCycle,
      isSliding = snapshot.isSliding,
      isOnHoverboard = snapshot.hoverboardRemaining > 0f,
      isOnJetpack = snapshot.isOnJetpack,
      hasMagnet = snapshot.magnetRemaining > 0f,
      hasSneakers = snapshot.sneakersRemaining > 0f,
      squashStretch = snapshot.squashStretch,
    )
  }
}

fun DrawScope.drawCockroachHero(
  centerX: Float,
  bottomY: Float,
  size: Float,
  roachSkin: RoachSkin,
  boardSkin: HoverboardSkin?,
  runCycle: Float,
  isSliding: Boolean = false,
  isOnHoverboard: Boolean = false,
  isOnJetpack: Boolean = false,
  hasMagnet: Boolean = false,
  hasSneakers: Boolean = false,
  squashStretch: Float = 1f,
) {
  val bodyH = size * (if (isSliding) 0.52f else 1.05f) * squashStretch
  val bodyW = size * (if (isSliding) 0.85f else 0.56f) / squashStretch.coerceAtLeast(0.75f)
  val torsoBottomY = if (isOnHoverboard) bottomY - size * 0.22f else bottomY - size * 0.10f
  val torsoCenterY = torsoBottomY - bodyH * 0.46f

  // Magnet aura ring
  if (hasMagnet) {
    drawCircle(
      color = Color(0xFFFF3B5C).copy(alpha = 0.28f),
      radius = size * 0.95f,
      center = Offset(centerX, torsoCenterY),
      style = Stroke(width = size * 0.06f),
    )
  }

  // Hoverboard under feet
  if (isOnHoverboard && boardSkin != null) {
    drawOval(
      color = boardSkin.trailColor.copy(alpha = 0.35f),
      topLeft = Offset(centerX - size * 0.72f, bottomY - size * 0.24f),
      size = Size(size * 1.44f, size * 0.34f),
    )
    drawRoundRect(
      color = boardSkin.bodyColor,
      topLeft = Offset(centerX - size * 0.62f, bottomY - size * 0.22f),
      size = Size(size * 1.24f, size * 0.20f),
      cornerRadius = CornerRadius(size * 0.12f, size * 0.12f),
    )
    drawRoundRect(
      color = boardSkin.trimColor,
      topLeft = Offset(centerX - size * 0.52f, bottomY - size * 0.18f),
      size = Size(size * 1.04f, size * 0.08f),
      cornerRadius = CornerRadius(size * 0.06f, size * 0.06f),
    )
  }

  // Jetpack wings & thrusters
  if (isOnJetpack) {
    val wingFlap = sin(runCycle * 3.5f) * size * 0.16f
    val leftWing = Path().apply {
      moveTo(centerX - bodyW * 0.2f, torsoCenterY - bodyH * 0.1f)
      lineTo(centerX - size * 0.95f, torsoCenterY - size * 0.35f + wingFlap)
      lineTo(centerX - size * 0.70f, torsoCenterY + size * 0.25f)
      close()
    }
    val rightWing = Path().apply {
      moveTo(centerX + bodyW * 0.2f, torsoCenterY - bodyH * 0.1f)
      lineTo(centerX + size * 0.95f, torsoCenterY - size * 0.35f + wingFlap)
      lineTo(centerX + size * 0.70f, torsoCenterY + size * 0.25f)
      close()
    }
    drawPath(leftWing, color = Color(0xFFFF9100).copy(alpha = 0.72f))
    drawPath(rightWing, color = Color(0xFFFF9100).copy(alpha = 0.72f))
  }

  // 6 Animated Cockroach Legs (4 side legs + 2 main running legs with sneakers)
  val legStroke = (size * 0.065f).coerceAtLeast(2.5f)
  for (side in listOf(-1f, 1f)) {
    for (pair in 0..1) {
      val phase = runCycle + pair * 1.7f + (if (side > 0) PI.toFloat() else 0f)
      val swing = sin(phase) * size * 0.14f
      val hipX = centerX + side * bodyW * 0.38f
      val hipY = torsoCenterY - bodyH * 0.08f + pair * bodyH * 0.22f
      val kneeX = hipX + side * size * 0.26f
      val kneeY = hipY - size * 0.06f + swing * 0.4f
      val footX = hipX + side * size * 0.34f
      val footY = hipY + size * 0.18f + swing
      drawLine(roachSkin.shellPrimary, Offset(hipX, hipY), Offset(kneeX, kneeY), strokeWidth = legStroke, cap = StrokeCap.Round)
      drawLine(roachSkin.shellSecondary, Offset(kneeX, kneeY), Offset(footX, footY), strokeWidth = legStroke * 0.85f, cap = StrokeCap.Round)
    }
  }

  // Main rear running feet + glowing neon sneakers
  val strideL = sin(runCycle) * size * 0.14f
  val strideR = sin(runCycle + PI.toFloat()) * size * 0.14f
  val sneakerBoost = if (hasSneakers) 1.32f else 1.0f
  val sneakerW = size * 0.28f * sneakerBoost
  val sneakerH = size * 0.16f * sneakerBoost

  // Left leg & sneaker
  drawLine(
    roachSkin.shellPrimary,
    Offset(centerX - bodyW * 0.24f, torsoBottomY - bodyH * 0.16f),
    Offset(centerX - bodyW * 0.30f, torsoBottomY + strideL * 0.5f),
    strokeWidth = legStroke * 1.1f,
    cap = StrokeCap.Round,
  )
  drawRoundRect(
    color = roachSkin.sneakerColor,
    topLeft = Offset(centerX - bodyW * 0.30f - sneakerW * 0.5f, torsoBottomY - sneakerH * 0.3f + strideL * 0.4f),
    size = Size(sneakerW, sneakerH),
    cornerRadius = CornerRadius(sneakerH * 0.45f, sneakerH * 0.45f),
  )

  // Right leg & sneaker
  drawLine(
    roachSkin.shellPrimary,
    Offset(centerX + bodyW * 0.24f, torsoBottomY - bodyH * 0.16f),
    Offset(centerX + bodyW * 0.30f, torsoBottomY + strideR * 0.5f),
    strokeWidth = legStroke * 1.1f,
    cap = StrokeCap.Round,
  )
  drawRoundRect(
    color = roachSkin.sneakerColor,
    topLeft = Offset(centerX + bodyW * 0.30f - sneakerW * 0.5f, torsoBottomY - sneakerH * 0.3f + strideR * 0.4f),
    size = Size(sneakerW, sneakerH),
    cornerRadius = CornerRadius(sneakerH * 0.45f, sneakerH * 0.45f),
  )

  // Glossy Segmented Chitin Abdomen Shell
  drawOval(
    color = roachSkin.shellPrimary,
    topLeft = Offset(centerX - bodyW * 0.55f, torsoCenterY - bodyH * 0.45f),
    size = Size(bodyW * 1.10f, bodyH * 0.90f),
  )
  // Chitin back shine & segments
  drawOval(
    color = roachSkin.shellSecondary,
    topLeft = Offset(centerX - bodyW * 0.40f, torsoCenterY - bodyH * 0.36f),
    size = Size(bodyW * 0.70f, bodyH * 0.56f),
  )
  for (seg in -1..1) {
    val sy = torsoCenterY + seg * bodyH * 0.14f
    drawLine(
      color = Color.Black.copy(alpha = 0.24f),
      start = Offset(centerX - bodyW * 0.38f, sy),
      end = Offset(centerX + bodyW * 0.38f, sy),
      strokeWidth = (size * 0.03f).coerceAtLeast(1.5f),
    )
  }

  // Head & Pronotum Collar
  val headRadius = size * (if (isSliding) 0.22f else 0.26f)
  val headY = torsoCenterY - bodyH * 0.44f
  drawCircle(
    color = roachSkin.shellPrimary,
    radius = headRadius,
    center = Offset(centerX, headY),
  )

  // Expressive Cartoon Eyes
  val eyeR = headRadius * 0.34f
  drawCircle(Color.White, radius = eyeR, center = Offset(centerX - headRadius * 0.38f, headY - headRadius * 0.08f))
  drawCircle(Color.White, radius = eyeR, center = Offset(centerX + headRadius * 0.38f, headY - headRadius * 0.08f))
  drawCircle(Color(0xFF14112B), radius = eyeR * 0.52f, center = Offset(centerX - headRadius * 0.36f, headY - headRadius * 0.08f))
  drawCircle(Color(0xFF14112B), radius = eyeR * 0.52f, center = Offset(centerX + headRadius * 0.36f, headY - headRadius * 0.08f))

  // Twitching Long Cockroach Antennae
  val antWiggle = sin(runCycle * 1.6f) * size * 0.08f
  val antStroke = (size * 0.04f).coerceAtLeast(1.8f)
  val leftAnt = Path().apply {
    moveTo(centerX - headRadius * 0.35f, headY - headRadius * 0.7f)
    quadraticTo(
      centerX - size * 0.35f,
      headY - size * 0.48f + antWiggle,
      centerX - size * 0.48f,
      headY - size * 0.35f - antWiggle,
    )
  }
  val rightAnt = Path().apply {
    moveTo(centerX + headRadius * 0.35f, headY - headRadius * 0.7f)
    quadraticTo(
      centerX + size * 0.35f,
      headY - size * 0.48f - antWiggle,
      centerX + size * 0.48f,
      headY - size * 0.35f + antWiggle,
    )
  }
  drawPath(leftAnt, color = roachSkin.shellSecondary, style = Stroke(width = antStroke, cap = StrokeCap.Round))
  drawPath(rightAnt, color = roachSkin.shellSecondary, style = Stroke(width = antStroke, cap = StrokeCap.Round))

  // Character Accessory!
  when (roachSkin.accessory) {
    RoachAccessory.CAP -> {
      drawArc(
        color = roachSkin.accentColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(centerX - headRadius * 1.05f, headY - headRadius * 1.25f),
        size = Size(headRadius * 2.1f, headRadius * 1.2f),
      )
      drawRoundRect(
        color = roachSkin.accentColor,
        topLeft = Offset(centerX + headRadius * 0.2f, headY - headRadius * 0.72f),
        size = Size(headRadius * 1.25f, headRadius * 0.26f),
        cornerRadius = CornerRadius(4f, 4f),
      )
    }
    RoachAccessory.CYBER_VISOR -> {
      drawRoundRect(
        color = Color(0xFF00E5FF),
        topLeft = Offset(centerX - headRadius * 0.92f, headY - headRadius * 0.30f),
        size = Size(headRadius * 1.84f, headRadius * 0.44f),
        cornerRadius = CornerRadius(6f, 6f),
      )
    }
    RoachAccessory.CROWN -> {
      val crown = Path().apply {
        moveTo(centerX - headRadius * 0.75f, headY - headRadius * 0.65f)
        lineTo(centerX - headRadius * 0.65f, headY - headRadius * 1.35f)
        lineTo(centerX - headRadius * 0.22f, headY - headRadius * 0.92f)
        lineTo(centerX, headY - headRadius * 1.48f)
        lineTo(centerX + headRadius * 0.22f, headY - headRadius * 0.92f)
        lineTo(centerX + headRadius * 0.65f, headY - headRadius * 1.35f)
        lineTo(centerX + headRadius * 0.75f, headY - headRadius * 0.65f)
        close()
      }
      drawPath(crown, color = Color(0xFFFFD600))
    }
    RoachAccessory.MOHAWK -> {
      for (spike in -1..1) {
        val mPath = Path().apply {
          moveTo(centerX + spike * headRadius * 0.32f - headRadius * 0.16f, headY - headRadius * 0.70f)
          lineTo(centerX + spike * headRadius * 0.42f, headY - headRadius * 1.50f)
          lineTo(centerX + spike * headRadius * 0.32f + headRadius * 0.16f, headY - headRadius * 0.70f)
          close()
        }
        drawPath(mPath, color = Color(0xFF76FF03))
      }
    }
  }
}

private fun DrawScope.drawAngryChef(
  centerX: Float,
  bottomY: Float,
  size: Float,
  runCycle: Float,
) {
  val bob = abs(sin(runCycle * 0.9f)) * size * 0.08f
  val y = bottomY - bob

  // Chef body
  drawRoundRect(
    color = Color.White,
    topLeft = Offset(centerX - size * 0.42f, y - size * 0.95f),
    size = Size(size * 0.84f, size * 0.75f),
    cornerRadius = CornerRadius(size * 0.22f, size * 0.22f),
  )
  // Red neckerchief
  drawCircle(Color(0xFFFF3B5C), radius = size * 0.14f, center = Offset(centerX, y - size * 0.92f))
  // Chef head
  drawCircle(Color(0xFFFFCCBC), radius = size * 0.25f, center = Offset(centerX, y - size * 1.18f))
  // Puffy chef hat
  drawRoundRect(
    color = Color.White,
    topLeft = Offset(centerX - size * 0.26f, y - size * 1.62f),
    size = Size(size * 0.52f, size * 0.32f),
    cornerRadius = CornerRadius(size * 0.14f, size * 0.14f),
  )
  // Giant swinging broom!
  val broomAngle = sin(runCycle * 1.4f) * 25f
  rotate(degrees = broomAngle, pivot = Offset(centerX + size * 0.42f, y - size * 0.75f)) {
    drawLine(
      color = Color(0xFF8D6E63),
      start = Offset(centerX + size * 0.42f, y - size * 0.55f),
      end = Offset(centerX + size * 0.65f, y - size * 1.65f),
      strokeWidth = (size * 0.08f).coerceAtLeast(3f),
      cap = StrokeCap.Round,
    )
    drawRoundRect(
      color = Color(0xFFFFCA28),
      topLeft = Offset(centerX + size * 0.52f, y - size * 1.80f),
      size = Size(size * 0.28f, size * 0.24f),
      cornerRadius = CornerRadius(4f, 4f),
    )
  }
}

private fun DrawScope.drawCallout(
  textMeasurer: TextMeasurer,
  callout: FloatingCallout,
  w: Float,
  h: Float,
) {
  val alpha = (callout.life / callout.maxLife).coerceIn(0f, 1f)
  val layoutResult = textMeasurer.measure(
    text = callout.text,
    style = TextStyle(
      fontFamily = BalooDisplayFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 20.sp,
      color = callout.color.copy(alpha = alpha),
    ),
  )
  val x = (callout.xNorm * w - layoutResult.size.width * 0.5f).coerceIn(16f, w - layoutResult.size.width - 16f)
  val y = callout.yNorm * h
  drawText(
    textLayoutResult = layoutResult,
    topLeft = Offset(x, y),
  )
}
