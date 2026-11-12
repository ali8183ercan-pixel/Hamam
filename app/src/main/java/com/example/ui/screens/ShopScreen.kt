package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Skateboarding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BoardCatalog
import com.example.data.model.CharacterCatalog
import com.example.data.model.HoverboardSkin
import com.example.data.model.PlayerProfile
import com.example.data.model.RoachSkin
import com.example.data.repository.UpgradeType
import com.example.game.drawCockroachHero
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepTunnel
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.GraffitiOrange
import com.example.ui.theme.MidnightSubway
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.ToxicLime
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ShopScreen(
  profile: PlayerProfile,
  shopMessage: String?,
  viewModel: MainViewModel,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedTab by remember { mutableIntStateOf(0) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(shopMessage) {
    if (shopMessage != null) {
      snackbarHostState.showSnackbar(shopMessage)
      viewModel.clearShopMessage()
    }
  }

  Scaffold(
    containerColor = MidnightSubway,
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding()
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 620.dp).fillMaxSize()) {
        // Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(48.dp)
                .testTag("shop_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = SoftWhite,
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Böcek Mağazası",
              style = MaterialTheme.typography.headlineMedium,
              color = SoftWhite,
            )
          }

          StatPill(
            icon = Icons.Rounded.MonetizationOn,
            value = formatNumber(profile.totalCoins),
            accent = CoinGold,
          )
        }

        val tabs = listOf("Böcekler", "Kaykaylar", "Güçler")
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DeepTunnel,
          contentColor = NeonCyan,
        ) {
          tabs.forEachIndexed { idx, title ->
            Tab(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              text = { Text(title, style = MaterialTheme.typography.titleMedium) },
            )
          }
        }

        when (selectedTab) {
          0 -> CharactersTab(profile = profile, onAction = { viewModel.buyOrEquipCharacter(it) })
          1 -> BoardsTab(profile = profile, onAction = { viewModel.buyOrEquipBoard(it) })
          else -> UpgradesTab(
            profile = profile,
            onUpgrade = { viewModel.upgradePower(it) },
            onBuyBoards = { viewModel.buyHoverboardPack() },
          )
        }
      }
    }
  }
}

@Composable
private fun CharactersTab(
  profile: PlayerProfile,
  onAction: (String) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(CharacterCatalog.all, key = { it.id }) { skin ->
      val unlocked = profile.unlockedCharacters.contains(skin.id)
      val equipped = profile.selectedCharacterId == skin.id
      val canAfford = profile.totalCoins >= skin.price

      Surface(
        color = ElevatedCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (equipped) 2.dp else 1.dp,
            color = if (equipped) ToxicLime else skin.accentColor.copy(alpha = 0.35f),
            shape = RoundedCornerShape(20.dp),
          ),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CockroachAvatarPreview(skin = skin)
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(skin.nameTr, style = MaterialTheme.typography.titleLarge, color = SoftWhite)
            Text(
              skin.subtitleTr,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(10.dp))

          ShopActionButton(
            unlocked = unlocked,
            equipped = equipped,
            price = skin.price,
            canAfford = canAfford,
            onClick = { onAction(skin.id) },
            tag = "buy_char_${skin.id}",
          )
        }
      }
    }
  }
}

@Composable
private fun BoardsTab(
  profile: PlayerProfile,
  onAction: (String) -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(BoardCatalog.all, key = { it.id }) { board ->
      val unlocked = profile.unlockedBoards.contains(board.id)
      val equipped = profile.selectedBoardId == board.id
      val canAfford = profile.totalCoins >= board.price

      Surface(
        color = ElevatedCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (equipped) 2.dp else 1.dp,
            color = if (equipped) ToxicLime else board.trimColor.copy(alpha = 0.4f),
            shape = RoundedCornerShape(20.dp),
          ),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BoardPreview(board = board)
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(board.nameTr, style = MaterialTheme.typography.titleLarge, color = SoftWhite)
            Text(
              board.subtitleTr,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(10.dp))

          ShopActionButton(
            unlocked = unlocked,
            equipped = equipped,
            price = board.price,
            canAfford = canAfford,
            onClick = { onAction(board.id) },
            tag = "buy_board_${board.id}",
          )
        }
      }
    }
  }
}

@Composable
private fun UpgradesTab(
  profile: PlayerProfile,
  onUpgrade: (UpgradeType) -> Unit,
  onBuyBoards: () -> Unit,
) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Consumable Hoverboard 3-pack
    item {
      Surface(
        color = ElevatedCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(DeepTunnel),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Rounded.Skateboarding, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(30.dp))
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("3x Kaykay Paketi", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
            Text(
              "Mevcut: ${profile.hoverboardCount} adet • Çarpışmadan korur",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = onBuyBoards,
            enabled = profile.totalCoins >= 75L,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoinGold, contentColor = MidnightSubway),
            modifier = Modifier.height(48.dp),
          ) {
            Text("75 Altın", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    items(UpgradeType.entries, key = { it.name }) { upgrade ->
      val level = when (upgrade) {
        UpgradeType.MULTIPLIER -> profile.scoreMultiplierLevel
        UpgradeType.MAGNET -> profile.magnetLevel
        UpgradeType.JETPACK -> profile.jetpackLevel
        UpgradeType.SNEAKERS -> profile.sneakersLevel
      }
      val isMax = level >= upgrade.maxLevel
      val cost = upgrade.baseCost * level
      val canAfford = !isMax && profile.totalCoins >= cost

      Surface(
        color = ElevatedCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, GraffitiOrange.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(DeepTunnel),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = GraffitiOrange, modifier = Modifier.size(28.dp))
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("${upgrade.titleTr} (Sv. $level/${upgrade.maxLevel})", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
            Text(
              upgrade.subtitleTr,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { (level.toFloat() / upgrade.maxLevel.toFloat()).coerceIn(0f, 1f) },
              color = GraffitiOrange,
              trackColor = DeepTunnel,
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Button(
            onClick = { onUpgrade(upgrade) },
            enabled = canAfford,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isMax) ToxicLime else CoinGold,
              contentColor = MidnightSubway,
            ),
            modifier = Modifier.height(48.dp),
          ) {
            Text(
              text = if (isMax) "MAKS" else "$cost Altın",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ShopActionButton(
  unlocked: Boolean,
  equipped: Boolean,
  price: Long,
  canAfford: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Button(
    onClick = onClick,
    enabled = equipped || unlocked || canAfford,
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = when {
        equipped -> ToxicLime
        unlocked -> NeonCyan
        else -> CoinGold
      },
      contentColor = MidnightSubway,
    ),
    modifier = Modifier
      .height(48.dp)
      .testTag(tag),
  ) {
    if (equipped) {
      Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("Seçili", style = MaterialTheme.typography.labelLarge)
    } else if (unlocked) {
      Text("Seç", style = MaterialTheme.typography.labelLarge)
    } else {
      Text("$price Altın", style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
private fun CockroachAvatarPreview(skin: RoachSkin) {
  Box(
    modifier = Modifier
      .size(68.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(DeepTunnel)
      .border(1.dp, skin.accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCockroachHero(
        centerX = size.width * 0.5f,
        bottomY = size.height * 0.88f,
        size = size.minDimension * 0.56f,
        roachSkin = skin,
        boardSkin = null,
        runCycle = 0.6f,
      )
    }
  }
}

@Composable
private fun BoardPreview(board: HoverboardSkin) {
  Box(
    modifier = Modifier
      .size(68.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(DeepTunnel)
      .border(1.dp, board.trimColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val bw = size.width * 0.72f
      val bh = size.height * 0.24f
      val left = (size.width - bw) * 0.5f
      val top = (size.height - bh) * 0.5f
      drawOval(
        color = board.trailColor.copy(alpha = 0.30f),
        topLeft = Offset(left - 6f, top - 6f),
        size = Size(bw + 12f, bh + 12f),
      )
      drawRoundRect(
        color = board.bodyColor,
        topLeft = Offset(left, top),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(bh * 0.5f, bh * 0.5f),
      )
      drawRoundRect(
        color = board.trimColor,
        topLeft = Offset(left + 6f, top + bh * 0.32f),
        size = Size(bw - 12f, bh * 0.36f),
        cornerRadius = CornerRadius(bh * 0.25f, bh * 0.25f),
      )
    }
  }
}
