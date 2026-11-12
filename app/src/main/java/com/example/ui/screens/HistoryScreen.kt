package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterCatalog
import com.example.data.model.GameRun
import com.example.data.model.PlayerProfile
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepTunnel
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.GraffitiOrange
import com.example.ui.theme.MidnightSubway
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.ToxicLime
import com.example.ui.viewmodel.UiState

@Composable
fun HistoryScreen(
  profile: PlayerProfile,
  runsState: UiState<List<GameRun>>,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  Scaffold(containerColor = MidnightSubway) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding()
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 620.dp).fillMaxSize()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("history_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri", tint = SoftWhite)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text("Koşu Geçmişi", style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
        }

        // Top career summary cards
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          CareerCard("En İyi Skor", formatNumber(profile.highScore), CoinGold, Modifier.weight(1f))
          CareerCard("En Uzun", "${profile.bestDistanceMeters} m", NeonCyan, Modifier.weight(1f))
          CareerCard("Toplam Koşu", "${profile.totalRuns}", ToxicLime, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (runsState) {
          is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = NeonCyan)
            }
          }
          is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
              Text(runsState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
          }
          is UiState.Success -> {
            val runs = runsState.data
            if (runs.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(56.dp))
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    "Henüz tamamlanmış koşu yok.\nRaylara çıkıp ilk rekorunu kır!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            } else {
              LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                items(runs, key = { it.id }) { run ->
                  val charName = CharacterCatalog.findById(run.characterId).nameTr
                  Surface(
                    color = ElevatedCard,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                  ) {
                    Row(
                      modifier = Modifier.padding(14.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = CoinGold, modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            text = "${formatNumber(run.score)} Puan",
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftWhite,
                          )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                          text = "$charName • ${run.distanceMeters} m",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = CoinGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = "+${run.coinsCollected}",
                          style = MaterialTheme.typography.titleMedium,
                          color = CoinGold,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CareerCard(
  title: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = DeepTunnel,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(4.dp))
      Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
    }
  }
}
