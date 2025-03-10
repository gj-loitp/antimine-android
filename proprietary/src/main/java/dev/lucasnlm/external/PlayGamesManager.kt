package dev.lucasnlm.external

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PlayGamesAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PlayGamesManager(
    private val context: Context,
    private val crashReporter: CrashReporter,
) : IPlayGamesManager {
    private var account: GoogleSignInAccount? = null
    private var requestLogin: Boolean = true
    private val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private fun setupPopUp(activity: Activity, account: GoogleSignInAccount) {
        Games.getGamesClient(context, account).apply {
            setViewForPopups(activity.findViewById(android.R.id.content))
            setGravityForPopups(Gravity.TOP or Gravity.END)
        }
    }

    override suspend fun playerId(): String? {
        return account?.let {
            try {
                Games.getPlayersClient(context, it).currentPlayerId.await()
            } catch (exception: Exception) {
                exception.message?.let { message ->
                    crashReporter.sendError(message)
                }

                Log.e(TAG, "Fail to request current player id", exception)
                account = null
                null
            }
        }
    }

    override fun showPlayPopUp(activity: Activity) {
        if (!activity.isFinishing) {
            account?.let {
                setupPopUp(activity, it)
            }
        }
    }

    override fun hasGooglePlayGames(): Boolean = true

    override suspend fun silentLogin(): Boolean {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build()
        val lastAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)
        return try {
            val client = GoogleSignIn.getClient(context, signInOptions)
            account = lastAccount ?: client.silentSignIn().await()
            account != null
        } catch (e: Exception) {
            account = null
            false
        }
    }

    override fun getLoginIntent(): Intent? {
        return GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).signInIntent
    }

    override fun handleLoginResult(data: Intent?) {
        if (data != null) {
            Auth.GoogleSignInApi.getSignInResultFromIntent(data)?.let { result ->
                if (result.isSuccess) {
                    account = result.signInAccount
                } else {
                    result.status.statusMessage?.let { message ->
                        if (message.isNotBlank()) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun isLogged(): Boolean = account != null

    override fun openAchievements(activity: Activity) {
        account?.let {
            Games.getAchievementsClient(context, it)
                .achievementsIntent
                .addOnSuccessListener { intent ->
                    try {
                        activity.startActivityForResult(intent, 0)
                    } catch (e: ActivityNotFoundException) {
                        // Google Play Games error
                        // TODO add warning
                    }
                }
        }
    }

    override fun openLeaderboards(activity: Activity) {
        account?.let {
            Games.getLeaderboardsClient(context, it)
                .allLeaderboardsIntent
                .addOnSuccessListener { intent ->
                    try {
                        activity.startActivityForResult(intent, 0)
                    } catch (e: ActivityNotFoundException) {
                        // Google Play Games error
                        // TODO add warning
                    }
                }
        }
    }

    override suspend fun unlockAchievement(achievement: Achievement) {
        account?.let {
            try {
                Games.getAchievementsClient(context, it).unlockImmediate(achievement.value).await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override suspend fun incrementAchievement(achievement: Achievement, value: Int) {
        account?.let {
            try {
                Games.getAchievementsClient(context, it).incrementImmediate(achievement.value, value).await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override suspend fun setAchievementSteps(achievement: Achievement, value: Int) {
        account?.let {
            try {
                Games.getAchievementsClient(context, it).setStepsImmediate(achievement.value, value).await()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun submitLeaderboard(leaderboard: Leaderboard, value: Long) {
        account?.let {
            Games.getLeaderboardsClient(context, it).submitScore(leaderboard.value, value)
        }
    }

    override fun keepRequestingLogin(status: Boolean) {
        requestLogin = status
    }

    override fun shouldRequestLogin(): Boolean {
        return requestLogin
    }

    override fun signInToFirebase(activity: Activity) {
        val googleAccount = account
        val serverAuthCode = googleAccount?.serverAuthCode

        if (googleAccount != null && serverAuthCode != null) {
            val credential = PlayGamesAuthProvider.getCredential(serverAuthCode)

            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential:success")
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Fail to signIn with firebase", it)
                    crashReporter.sendError("Fail to  signIn with firebase. $it")
                }
        }
    }

    companion object {
        val TAG = PlayGamesManager::class.simpleName
    }
}
