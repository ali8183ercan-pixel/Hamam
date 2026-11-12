package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepTunnel
import com.example.ui.theme.ElevatedCard
import com.example.ui.theme.GraffitiOrange
import com.example.ui.theme.MidnightSubway
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.ToxicLime

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(MidnightSubway, DeepTunnel, Color(0xFF0E0A1E))
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    val isWide = maxWidth >= 600.dp

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = if (isWide) 560.dp else 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Hero Arcade Artwork
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(if (isWide) 250.dp else 210.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(2.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_roach),
            contentDescription = "Roach Rush metro sörfçüsü hamam böceği çizimi",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, MidnightSubway.copy(alpha = 0.85f)),
                  startY = 120f,
                )
              )
          )
          Surface(
            color = GraffitiOrange,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(12.dp),
          ) {
            Text(
              text = "METRO KOŞUSU",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "ROACH RUSH",
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
          color = CoinGold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Raylarda sörf yap, kızgın şeften kaç, efsane hamam böceklerini aç!",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Feature Highlights
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FeatureMiniCard(
            icon = Icons.AutoMirrored.Rounded.DirectionsRun,
            title = "3 Şerit",
            subtitle = "Kay, zıpla, tren üstüne çık",
            accent = NeonCyan,
            modifier = Modifier.weight(1f),
          )
          FeatureMiniCard(
            icon = Icons.Rounded.Bolt,
            title = "Güçler",
            subtitle = "Jetpack, Mıknatıs, Kaykay",
            accent = GraffitiOrange,
            modifier = Modifier.weight(1f),
          )
          FeatureMiniCard(
            icon = Icons.Rounded.EmojiEvents,
            title = "Bulut Kayıt",
            subtitle = "Skor & karakterlerin güvende",
            accent = ToxicLime,
            modifier = Modifier.weight(1f),
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
          ) {
            Text(
              text = errorMessage!!,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp),
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isLoading = false
              },
            )
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = MidnightSubway,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MidnightSubway,
              strokeWidth = 2.5.dp,
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Login,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Google ile Giriş Yap",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureMiniCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = ElevatedCard,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = accent,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = SoftWhite,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
