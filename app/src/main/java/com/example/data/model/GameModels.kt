package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Böcek Koşucusu",
  val highScore: Long = 0L,
  val totalCoins: Long = 100L,
  val totalRuns: Long = 0L,
  val bestDistanceMeters: Long = 0L,
  val scoreMultiplierLevel: Long = 1L,
  val magnetLevel: Long = 1L,
  val jetpackLevel: Long = 1L,
  val sneakersLevel: Long = 1L,
  val hoverboardCount: Long = 3L,
  val selectedCharacterId: String = CharacterCatalog.DEFAULT_ID,
  val selectedBoardId: String = BoardCatalog.DEFAULT_ID,
  val unlockedCharacters: List<String> = listOf(CharacterCatalog.DEFAULT_ID),
  val unlockedBoards: List<String> = listOf(BoardCatalog.DEFAULT_ID),
  val soundEnabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class GameRun(
  @DocumentId val id: String = "",
  val userId: String = "",
  val score: Long = 0L,
  val distanceMeters: Long = 0L,
  val coinsCollected: Long = 0L,
  val characterId: String = CharacterCatalog.DEFAULT_ID,
  val boardId: String = BoardCatalog.DEFAULT_ID,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class RoachAccessory {
  CAP,
  CYBER_VISOR,
  CROWN,
  MOHAWK,
}

data class RoachSkin(
  val id: String,
  val nameTr: String,
  val subtitleTr: String,
  val price: Long,
  val shellPrimary: Color,
  val shellSecondary: Color,
  val sneakerColor: Color,
  val accentColor: Color,
  val accessory: RoachAccessory,
)

data class HoverboardSkin(
  val id: String,
  val nameTr: String,
  val subtitleTr: String,
  val price: Long,
  val bodyColor: Color,
  val trimColor: Color,
  val trailColor: Color,
)

object CharacterCatalog {
  const val DEFAULT_ID = "street_roach"

  val all: List<RoachSkin> = listOf(
    RoachSkin(
      id = "street_roach",
      nameTr = "Sokak Hamamı",
      subtitleTr = "Klasik hızlı metro koşucusu",
      price = 0L,
      shellPrimary = Color(0xFF7B421D),
      shellSecondary = Color(0xFFB56935),
      sneakerColor = Color(0xFF00E5FF),
      accentColor = Color(0xFFFF3B5C),
      accessory = RoachAccessory.CAP,
    ),
    RoachSkin(
      id = "neon_roach",
      nameTr = "Siber Böcek",
      subtitleTr = "Neon devre zırhlı gece avcısı",
      price = 150L,
      shellPrimary = Color(0xFF1F274D),
      shellSecondary = Color(0xFF00E5FF),
      sneakerColor = Color(0xFFFF2A85),
      accentColor = Color(0xFF00E5FF),
      accessory = RoachAccessory.CYBER_VISOR,
    ),
    RoachSkin(
      id = "gold_roach",
      nameTr = "Altın Kral",
      subtitleTr = "24 ayar parlak kabuk ve taç",
      price = 400L,
      shellPrimary = Color(0xFFD49B00),
      shellSecondary = Color(0xFFFFE55C),
      sneakerColor = Color(0xFF76FF03),
      accentColor = Color(0xFFFFD600),
      accessory = RoachAccessory.CROWN,
    ),
    RoachSkin(
      id = "punk_roach",
      nameTr = "Punk Kanalizasyon",
      subtitleTr = "Asi yeşil saçlı hız tutkunu",
      price = 750L,
      shellPrimary = Color(0xFF43215A),
      shellSecondary = Color(0xFF9C4DCC),
      sneakerColor = Color(0xFFFF6D00),
      accentColor = Color(0xFF76FF03),
      accessory = RoachAccessory.MOHAWK,
    ),
  )

  fun findById(id: String): RoachSkin = all.firstOrNull { it.id == id } ?: all.first()
}

object BoardCatalog {
  const val DEFAULT_ID = "leaf_board"

  val all: List<HoverboardSkin> = listOf(
    HoverboardSkin(
      id = "leaf_board",
      nameTr = "Yaprak Kaykay",
      subtitleTr = "Doğal ve çevik başlangıç tahtası",
      price = 0L,
      bodyColor = Color(0xFF2E7D32),
      trimColor = Color(0xFF76FF03),
      trailColor = Color(0xFF76FF03),
    ),
    HoverboardSkin(
      id = "bottlecap_board",
      nameTr = "Gazoz Kapağı",
      subtitleTr = "Metalik retro metro sörfçüsü",
      price = 120L,
      bodyColor = Color(0xFFD50000),
      trimColor = Color(0xFFFFD600),
      trailColor = Color(0xFFFFAB00),
    ),
    HoverboardSkin(
      id = "cyber_board",
      nameTr = "Siber Neon",
      subtitleTr = "Plazma ışık izli yüksek teknoloji",
      price = 300L,
      bodyColor = Color(0xFF15193B),
      trimColor = Color(0xFF00E5FF),
      trailColor = Color(0xFF00E5FF),
    ),
    HoverboardSkin(
      id = "flame_board",
      nameTr = "Alev Jet",
      subtitleTr = "Köz kıvılcımları saçan süper tahta",
      price = 550L,
      bodyColor = Color(0xFFE65100),
      trimColor = Color(0xFFFFEA00),
      trailColor = Color(0xFFFF6D00),
    ),
  )

  fun findById(id: String): HoverboardSkin = all.firstOrNull { it.id == id } ?: all.first()
}
