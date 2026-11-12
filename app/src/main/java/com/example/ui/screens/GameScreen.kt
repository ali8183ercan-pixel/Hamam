package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Skateboarding
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardCatalog
import com.example.data.model.CharacterCatalog
import com.example.data.model.PlayerProfile
import com.example.game.GamePhase
import com.example.game.GameSnapshot
import com.example.game.SubwayGameCanvas
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepTunnel
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.GraffitiOrange
import com.example.ui.theme.MidnightSubway
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.ToxicLime
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun GameScreen(
  profile: PlayerProfile,
  snapshot: GameSnapshot,
  isNewHighScore: Boolean,
  viewModel: MainViewModel,
  onSignOutClick: () -> Unit,
) {
  val roachSkin = remember(profile.selectedCharacterId) {
    CharacterCatalog.findById(profile.selectedCharacterId)
  }
  val boardSkin = remember(profile.selectedBoardId) {
    BoardCatalog.findById(profile.selectedBoardId)
  }

  // Handle back button during active run or pause
  BackHandler(enabled = snapshot.phase == GamePhase.RUNNING || snapshot.phase == GamePhase.PAUSED) {
    if (snapshot.phase == GamePhase.RUNNING) {
      viewModel.pauseGame()
    } else {
      viewModel.returnToMenu()
    }
  }

  // 60 FPS frame ticker
  LaunchedEffect(snapshot.phase) {
    if (snapshot.phase == GamePhase.RUNNING || snapshot.phase == GamePhase.MENU_IDLE) {
      var lastNanos = 0L
      while (true) {
        withFrameNanos { frameNanos ->
          if (lastNanos != 0L) {
            val dt = (frameNanos - lastNanos) / 1_000_000_000f
            viewModel.stepFrame(dt)
          }
          lastNanos = frameNanos
        }
      }
    }
  }

  val swipeThresholdPx = with(LocalDensity.current) { 26.dp.toPx() }
  var lastTapTimeMs by remember { mutableLongStateOf(0L) }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightSubway)
  ) {
    // Full-bleed 3D Subway Canvas + responsive swipe gestures
    SubwayGameCanvas(
      snapshot = snapshot,
      roachSkin = roachSkin,
      boardSkin = boardSkin,
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(snapshot.phase) {
          if (snapshot.phase == GamePhase.RUNNING) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              var totalDx = 0f
              var totalDy = 0f
              var gestureHandled = false
              val startPosition = down.position

              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                  // Tap without drag -> double-tap hoverboard check
                  if (!gestureHandled) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTimeMs < 290L) {
                      viewModel.onActivateHoverboard()
                      lastTapTimeMs = 0L
                    } else {
                      lastTapTimeMs = now
                    }
                  }
                  break
                }
                if (!gestureHandled) {
                  totalDx = change.position.x - startPosition.x
                  totalDy = change.position.y - startPosition.y
                  if (abs(totalDx) > swipeThresholdPx || abs(totalDy) > swipeThresholdPx) {
                    gestureHandled = true
                    if (abs(totalDx) > abs(totalDy)) {
                      if (totalDx > 0f) viewModel.onSwipeRight() else viewModel.onSwipeLeft()
                    } else {
                      if (totalDy < 0f) viewModel.onSwipeUp() else viewModel.onSwipeDown()
                    }
                    change.consume()
                  }
                }
              }
            }
          }
        },
    )

    // Overlay HUD according to GamePhase
    when (snapshot.phase) {
      GamePhase.MENU_IDLE -> {
        MenuIdleOverlay(
          profile = profile,
          onPlayClick = { viewModel.startGame() },
          onShopClick = { viewModel.navigateTo(AppDestination.SHOP) },
          onHistoryClick = { viewModel.navigateTo(AppDestination.HISTORY) },
          onToggleSound = { viewModel.toggleSound() },
          onSignOutClick = onSignOutClick,
        )
      }
      GamePhase.RUNNING -> {
        RunningHudOverlay(
          snapshot = snapshot,
          onPauseClick = { viewModel.pauseGame() },
          onActivateBoard = { viewModel.onActivateHoverboard() },
        )
      }
      GamePhase.PAUSED -> {
        RunningHudOverlay(
          snapshot = snapshot,
          onPauseClick = { viewModel.resumeGame() },
          onActivateBoard = {},
        )
        PauseModal(
          onResume = { viewModel.resumeGame() },
          onRestart = { viewModel.startGame() },
          onMenu = { viewModel.returnToMenu() },
        )
      }
      GamePhase.GAME_OVER -> {
        GameOverModal(
          snapshot = snapshot,
          highScore = maxOf(profile.highScore, snapshot.score),
          isNewHighScore = isNewHighScore,
          onPlayAgain = { viewModel.startGame() },
          onShop = { viewModel.navigateTo(AppDestination.SHOP) },
          onMenu = { viewModel.returnToMenu() },
        )
      }
    }
  }
}

@Composable
private fun MenuIdleOverlay(
  profile: PlayerProfile,
  onPlayClick: () -> Unit,
  onShopClick: () -> Unit,
  onHistoryClick: () -> Unit,
  onToggleSound: () -> Unit,
  onSignOutClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(16.dp)
  ) {
    // Top Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatPill(
          icon = Icons.Rounded.EmojiEvents,
          value = formatNumber(profile.highScore),
          accent = NeonCyan,
        )
        StatPill(
          icon = Icons.Rounded.MonetizationOn,
          value = formatNumber(profile.totalCoins),
          accent = CoinGold,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
          color = DeepTunnel.copy(alpha = 0.88f),
          shape = CircleShape,
          modifier = Modifier
            .size(48.dp)
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
        ) {
          IconButton(
            onClick = onToggleSound,
            modifier = Modifier.testTag("sound_toggle_button"),
          ) {
            Icon(
              imageVector = if (profile.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
              contentDescription = if (profile.soundEnabled) "Sesi kapat" else "Sesi aç",
              tint = SoftWhite,
            )
          }
        }
        Surface(
          color = DeepTunnel.copy(alpha = 0.88f),
          shape = CircleShape,
          modifier = Modifier
            .size(48.dp)
            .border(1.dp, DangerRed.copy(alpha = 0.45f), CircleShape),
        ) {
          IconButton(
            onClick = onSignOutClick,
            modifier = Modifier.testTag("sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Logout,
              contentDescription = "Çıkış yap",
              tint = DangerRed,
            )
          }
        }
      }
    }

    // Center Title & Tap-to-Start Banner
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 64.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "ROACH RUSH",
        style = MaterialTheme.typography.displayLarge,
        color = CoinGold,
      )
      Surface(
        color = DeepTunnel.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .padding(top = 6.dp)
          .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
      ) {
        Text(
          text = "Merhaba ${profile.displayName} • Çarpan x${profile.scoreMultiplierLevel}",
          style = MaterialTheme.typography.labelLarge,
          color = NeonCyan,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }
    }

    // Bottom Dock
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .align(Alignment.BottomCenter),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        color = MidnightSubway.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.padding(bottom = 12.dp),
      ) {
        Text(
          text = "Sağa/Sola: Şerit • Yukarı: Zıpla • Aşağı: Kay • Çift Dokun: Kaykay",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onShopClick,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ElevatedCard,
            contentColor = SoftWhite,
          ),
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .border(1.5.dp, GraffitiOrange.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .testTag("shop_button"),
        ) {
          Icon(Icons.Rounded.Storefront, contentDescription = null, tint = GraffitiOrange)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Mağaza", style = MaterialTheme.typography.titleMedium)
        }

        Button(
          onClick = onPlayClick,
          shape = RoundedCornerShape(20.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ToxicLime,
            contentColor = MidnightSubway,
          ),
          modifier = Modifier
            .weight(1.35f)
            .height(64.dp)
            .testTag("play_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "KOŞ!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
          )
        }

        Button(
          onClick = onHistoryClick,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ElevatedCard,
            contentColor = SoftWhite,
          ),
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .border(1.5.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .testTag("history_button"),
        ) {
          Icon(Icons.Rounded.History, contentDescription = null, tint = NeonCyan)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Geçmiş", style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}

@Composable
private fun RunningHudOverlay(
  snapshot: GameSnapshot,
  onPauseClick: () -> Unit,
  onActivateBoard: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(14.dp)
  ) {
    // Top HUD
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      // Score + Multiplier + Distance
      Surface(
        color = MidnightSubway.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formatNumber(snapshot.score),
              style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
              color = SoftWhite,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = CoinGold,
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = "x${snapshot.activeMultiplier}",
                style = MaterialTheme.typography.labelLarge,
                color = MidnightSubway,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text = "${snapshot.distanceMeters} m",
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan,
          )
        }
      }

      // Coins + Pause
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StatPill(
          icon = Icons.Rounded.MonetizationOn,
          value = formatNumber(snapshot.runCoins),
          accent = CoinGold,
        )
        Surface(
          color = MidnightSubway.copy(alpha = 0.82f),
          shape = CircleShape,
          modifier = Modifier
            .size(48.dp)
            .border(1.5.dp, SoftWhite.copy(alpha = 0.35f), CircleShape),
        ) {
          IconButton(
            onClick = onPauseClick,
            modifier = Modifier.testTag("pause_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Pause,
              contentDescription = "Duraklat",
              tint = SoftWhite,
            )
          }
        }
      }
    }

    // Active Power-Up Bars on Left
    Column(
      modifier = Modifier
        .align(Alignment.CenterStart)
        .padding(top = 40.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (snapshot.hoverboardRemaining > 0f) {
        PowerUpTimerBar("Kaykay", snapshot.hoverboardRemaining / snapshot.hoverboardMax, NeonCyan)
      }
      if (snapshot.jetpackRemaining > 0f) {
        PowerUpTimerBar("Jetpack", snapshot.jetpackRemaining / snapshot.jetpackMax, GraffitiOrange)
      }
      if (snapshot.magnetRemaining > 0f) {
        PowerUpTimerBar("Mıknatıs", snapshot.magnetRemaining / snapshot.magnetMax, DangerRed)
      }
      if (snapshot.multiplier2xRemaining > 0f) {
        PowerUpTimerBar("2x Çarpan", snapshot.multiplier2xRemaining / snapshot.multiplier2xMax, CoinGold)
      }
      if (snapshot.sneakersRemaining > 0f) {
        PowerUpTimerBar("Ayakkabı", snapshot.sneakersRemaining / snapshot.sneakersMax, ToxicLime)
      }
    }

    // Quick Hoverboard Trigger Button on Bottom Right
    if (snapshot.availableHoverboards > 0L && snapshot.hoverboardRemaining <= 0f) {
      Surface(
        color = ElevatedCard.copy(alpha = 0.92f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .clip(RoundedCornerShape(18.dp))
          .border(2.dp, NeonCyan, RoundedCornerShape(18.dp))
          .clickable(onClick = onActivateBoard)
          .testTag("hoverboard_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Skateboarding,
            contentDescription = "Kaykay kullan",
            tint = NeonCyan,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "x${snapshot.availableHoverboards}",
            style = MaterialTheme.typography.titleMedium,
            color = SoftWhite,
          )
        }
      }
    }
  }
}

@Composable
private fun PowerUpTimerBar(
  label: String,
  progress: Float,
  color: Color,
) {
  Surface(
    color = MidnightSubway.copy(alpha = 0.80f),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .width(126.dp)
      .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = SoftWhite,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = color,
        trackColor = color.copy(alpha = 0.22f),
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(4.dp)),
      )
    }
  }
}

@Composable
private fun PauseModal(
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onMenu: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MidnightSubway.copy(alpha = 0.72f)),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      color = DeepTunnel,
      shape = RoundedCornerShape(26.dp),
      modifier = Modifier
        .padding(24.dp)
        .widthIn(max = 360.dp)
        .fillMaxWidth()
        .border(2.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(26.dp)),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "OYUN DURAKLATILDI",
          style = MaterialTheme.typography.headlineMedium,
          color = CoinGold,
          textAlign = TextAlign.Center,
        )
        Button(
          onClick = onResume,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ToxicLime, contentColor = MidnightSubway),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("resume_button"),
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Devam Et", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedButton(
          onClick = onRestart,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, tint = NeonCyan)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Baştan Başla", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
          onClick = onMenu,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        ) {
          Text("Ana Menü", color = SoftWhite, style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}

@Composable
private fun GameOverModal(
  snapshot: GameSnapshot,
  highScore: Long,
  isNewHighScore: Boolean,
  onPlayAgain: () -> Unit,
  onShop: () -> Unit,
  onMenu: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(MidnightSubway.copy(alpha = 0.78f), Color(0xCC1E091A))
        )
      ),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedVisibility(
      visible = true,
      enter = fadeIn() + scaleIn(initialScale = 0.9f),
      exit = fadeOut(),
    ) {
      Surface(
        color = DeepTunnel,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
          .padding(20.dp)
          .widthIn(max = 400.dp)
          .fillMaxWidth()
          .border(2.dp, if (isNewHighScore) CoinGold else GraffitiOrange, RoundedCornerShape(28.dp)),
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (isNewHighScore) {
            Surface(
              color = CoinGold,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.padding(bottom = 8.dp),
            ) {
              Text(
                text = "YENİ REKOR!",
                style = MaterialTheme.typography.labelLarge,
                color = MidnightSubway,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              )
            }
          }

          Text(
            text = "YAKALANDIN!",
            style = MaterialTheme.typography.displayMedium,
            color = DangerRed,
          )
          Text(
            text = "Şef süpürgeyi kaptı ama hamam böcekleri asla pes etmez.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            color = ElevatedCard,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              SummaryRow("Skor", formatNumber(snapshot.score), CoinGold)
              SummaryRow("Mesafe", "${snapshot.distanceMeters} m", NeonCyan)
              SummaryRow("Toplanan Altın", "+${formatNumber(snapshot.runCoins)}", CoinGold)
              SummaryRow("En Yüksek Skor", formatNumber(highScore), ToxicLime)
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = onPlayAgain,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ToxicLime, contentColor = MidnightSubway),
            modifier = Modifier
              .fillMaxWidth()
              .height(58.dp)
              .testTag("play_again_button"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tekrar Koş", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onShop,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp),
            ) {
              Icon(Icons.Rounded.Storefront, contentDescription = null, tint = GraffitiOrange, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Mağaza", color = GraffitiOrange)
            }
            OutlinedButton(
              onClick = onMenu,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp),
            ) {
              Text("Ana Menü", color = SoftWhite)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
  valueColor: Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor)
  }
}

@Composable
fun StatPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  value: String,
  accent: Color,
) {
  Surface(
    color = MidnightSubway.copy(alpha = 0.85f),
    shape = RoundedCornerShape(50),
    modifier = Modifier.border(1.5.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(50)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(text = value, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
    }
  }
}

fun formatNumber(value: Long): String {
  return NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR")).format(value)
}
