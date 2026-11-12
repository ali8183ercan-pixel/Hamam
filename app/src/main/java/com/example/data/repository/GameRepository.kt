package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BoardCatalog
import com.example.data.model.CharacterCatalog
import com.example.data.model.GameRun
import com.example.data.model.PlayerProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

enum class UpgradeType(
  val titleTr: String,
  val subtitleTr: String,
  val maxLevel: Long,
  val baseCost: Long,
) {
  MULTIPLIER("Skor Çarpanı", "Kalıcı çarpanı +1x artırır", 6L, 120L),
  MAGNET("Mıknatıs Süresi", "Altın çekim süresini uzatır", 5L, 90L),
  JETPACK("Jetpack Uçuşu", "Böcek kanadı uçuşunu uzatır", 5L, 100L),
  SNEAKERS("Süper Zıplama", "Yüksek zıplama süresini uzatır", 5L, 90L),
}

class GameRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Firestore erişimi için Google ile giriş yapılmalıdır.")
  }

  private fun sanitizeDisplayName(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (trimmed.isEmpty()) "Böcek Koşucusu" else trimmed.take(40)
  }

  suspend fun ensureProfileInitialized(displayNameHint: String? = null): PlayerProfile {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        snapshot.toObject(
          PlayerProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: PlayerProfile(id = uid, userId = uid, displayName = sanitizeDisplayName(displayNameHint))
      } else {
        val cleanName = sanitizeDisplayName(displayNameHint ?: auth.currentUser?.displayName)
        val payload = mapOf(
          "userId" to uid,
          "displayName" to cleanName,
          "highScore" to 0L,
          "totalCoins" to 100L,
          "totalRuns" to 0L,
          "bestDistanceMeters" to 0L,
          "scoreMultiplierLevel" to 1L,
          "magnetLevel" to 1L,
          "jetpackLevel" to 1L,
          "sneakersLevel" to 1L,
          "hoverboardCount" to 3L,
          "selectedCharacterId" to CharacterCatalog.DEFAULT_ID,
          "selectedBoardId" to BoardCatalog.DEFAULT_ID,
          "unlockedCharacters" to listOf(CharacterCatalog.DEFAULT_ID),
          "unlockedBoards" to listOf(BoardCatalog.DEFAULT_ID),
          "soundEnabled" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        val createdSnap = docRef.get().await()
        createdSnap.toObject(
          PlayerProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: PlayerProfile(id = uid, userId = uid, displayName = cleanName)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  fun observeProfile(userId: String): Flow<PlayerProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              PlayerProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observeRuns(userId: String): Flow<List<GameRun>> = flow {
    val path = "users/$userId/runs"
    emitAll(
      db.collection("users").document(userId).collection("runs")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(25)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            GameRun::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun recordFinishedRun(
    currentProfile: PlayerProfile,
    score: Long,
    distanceMeters: Long,
    coinsCollected: Long,
    hoverboardsUsed: Long,
  ) {
    val uid = requireUserId()
    val userDocRef = db.collection("users").document(uid)
    val runId = "run_${UUID.randomUUID().toString().replace("-", "").take(20)}"
    val runDocRef = userDocRef.collection("runs").document(runId)

    val safeScore = score.coerceIn(0L, 100_000_000L)
    val safeDist = distanceMeters.coerceIn(0L, 10_000_000L)
    val safeCoins = coinsCollected.coerceIn(0L, 1_000_000L)
    val newHoverboards = (currentProfile.hoverboardCount - hoverboardsUsed).coerceIn(0L, 9999L)

    val runPayload = mapOf(
      "userId" to uid,
      "score" to safeScore,
      "distanceMeters" to safeDist,
      "coinsCollected" to safeCoins,
      "characterId" to currentProfile.selectedCharacterId,
      "boardId" to currentProfile.selectedBoardId,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    val profileUpdates = mapOf(
      "highScore" to maxOf(currentProfile.highScore, safeScore),
      "totalCoins" to (currentProfile.totalCoins + safeCoins).coerceAtMost(100_000_000L),
      "totalRuns" to (currentProfile.totalRuns + 1L).coerceAtMost(10_000_000L),
      "bestDistanceMeters" to maxOf(currentProfile.bestDistanceMeters, safeDist),
      "hoverboardCount" to newHoverboards,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    try {
      db.runBatch { batch ->
        batch.set(runDocRef, runPayload)
        batch.update(userDocRef, profileUpdates)
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
      throw e
    }
  }

  suspend fun buyOrEquipCharacter(currentProfile: PlayerProfile, characterId: String): Boolean {
    val uid = requireUserId()
    val skin = CharacterCatalog.findById(characterId)
    val userDocRef = db.collection("users").document(uid)
    val alreadyUnlocked = currentProfile.unlockedCharacters.contains(skin.id)

    if (!alreadyUnlocked && currentProfile.totalCoins < skin.price) {
      return false
    }

    val updates = if (alreadyUnlocked) {
      mapOf(
        "selectedCharacterId" to skin.id,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    } else {
      mapOf(
        "totalCoins" to (currentProfile.totalCoins - skin.price),
        "unlockedCharacters" to (currentProfile.unlockedCharacters + skin.id).distinct(),
        "selectedCharacterId" to skin.id,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }

    return try {
      userDocRef.update(updates).await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  suspend fun buyOrEquipBoard(currentProfile: PlayerProfile, boardId: String): Boolean {
    val uid = requireUserId()
    val board = BoardCatalog.findById(boardId)
    val userDocRef = db.collection("users").document(uid)
    val alreadyUnlocked = currentProfile.unlockedBoards.contains(board.id)

    if (!alreadyUnlocked && currentProfile.totalCoins < board.price) {
      return false
    }

    val updates = if (alreadyUnlocked) {
      mapOf(
        "selectedBoardId" to board.id,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    } else {
      mapOf(
        "totalCoins" to (currentProfile.totalCoins - board.price),
        "unlockedBoards" to (currentProfile.unlockedBoards + board.id).distinct(),
        "selectedBoardId" to board.id,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }

    return try {
      userDocRef.update(updates).await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  suspend fun purchaseUpgrade(currentProfile: PlayerProfile, upgradeType: UpgradeType): Boolean {
    val uid = requireUserId()
    val currentLevel = when (upgradeType) {
      UpgradeType.MULTIPLIER -> currentProfile.scoreMultiplierLevel
      UpgradeType.MAGNET -> currentProfile.magnetLevel
      UpgradeType.JETPACK -> currentProfile.jetpackLevel
      UpgradeType.SNEAKERS -> currentProfile.sneakersLevel
    }
    if (currentLevel >= upgradeType.maxLevel) return false

    val cost = upgradeType.baseCost * currentLevel
    if (currentProfile.totalCoins < cost) return false

    val fieldName = when (upgradeType) {
      UpgradeType.MULTIPLIER -> "scoreMultiplierLevel"
      UpgradeType.MAGNET -> "magnetLevel"
      UpgradeType.JETPACK -> "jetpackLevel"
      UpgradeType.SNEAKERS -> "sneakersLevel"
    }

    val userDocRef = db.collection("users").document(uid)
    val updates = mapOf(
      "totalCoins" to (currentProfile.totalCoins - cost),
      fieldName to (currentLevel + 1L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      userDocRef.update(updates).await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  suspend fun purchaseHoverboardPack(currentProfile: PlayerProfile, quantity: Long = 3L, cost: Long = 75L): Boolean {
    val uid = requireUserId()
    if (currentProfile.totalCoins < cost) return false
    val userDocRef = db.collection("users").document(uid)
    val updates = mapOf(
      "totalCoins" to (currentProfile.totalCoins - cost),
      "hoverboardCount" to (currentProfile.hoverboardCount + quantity).coerceAtMost(9999L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      userDocRef.update(updates).await()
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }

  suspend fun setSoundEnabled(enabled: Boolean) {
    val uid = requireUserId()
    val userDocRef = db.collection("users").document(uid)
    try {
      userDocRef.update(
        mapOf(
          "soundEnabled" to enabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      throw e
    }
  }
}
