package dev.lucasnlm.antimine.wear.main

import android.content.Intent
import android.os.Bundle
import androidx.wear.widget.WearableLinearLayoutManager
import dev.lucasnlm.antimine.core.models.Difficulty
import dev.lucasnlm.antimine.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.ui.ext.ThemedActivity
import dev.lucasnlm.antimine.wear.R
import dev.lucasnlm.antimine.wear.databinding.ActivityMainBinding
import dev.lucasnlm.antimine.wear.game.GameActivity
import dev.lucasnlm.antimine.wear.main.models.MenuItem
import dev.lucasnlm.antimine.wear.main.view.MainMenuAdapter
import dev.lucasnlm.antimine.wear.tutorial.TutorialActivity
import org.koin.android.ext.android.inject

class MainActivity : ThemedActivity() {
    private lateinit var binding: ActivityMainBinding
    private val preferencesRepository: IPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val menuList = listOf(
            MenuItem(
                id = 0L,
                label = if (preferencesRepository.showContinueGame()) {
                    R.string.continue_game
                } else {
                    R.string.start
                },
                icon = R.drawable.play,
                onClick = {
                    continueGame()
                },
            ),
            MenuItem(
                id = 1L,
                label = R.string.minefield,
                icon = R.drawable.add,
                onClick = {
                    startDifficultyScreen()
                },
            ),
            MenuItem(
                id = 2L,
                label = R.string.control_types,
                icon = R.drawable.control,
                onClick = {
                    startControlScreen()
                },
            ),
            MenuItem(
                id = 3L,
                label = R.string.themes,
                icon = R.drawable.themes,
                onClick = {
                    startThemeScreen()
                },
            ),
            MenuItem(
                id = 4L,
                label = R.string.tutorial,
                icon = R.drawable.tutorial,
                onClick = {
                    startTutorial()
                },
            ),
            MenuItem(
                id = 6L,
                label = R.string.quit,
                icon = R.drawable.close,
                onClick = {
                    finishAffinity()
                },
            ),
        )

        binding.recyclerView.apply {
            setHasFixedSize(true)
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(this@MainActivity)
            adapter = MainMenuAdapter(menuList)
        }
    }

    private fun continueGame(difficulty: Difficulty? = null) {
        val context = application.applicationContext
        val intent = Intent(context, GameActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            difficulty?.let {
                val bundle = Bundle().apply {
                    putSerializable(GameActivity.DIFFICULTY, it)
                }
                putExtras(bundle)
            }
        }
        context.startActivity(intent)
    }

    private fun startDifficultyScreen() {
        val intent = Intent(this, DifficultyActivity::class.java)
        startActivity(intent)
    }

    private fun startControlScreen() {
        val intent = Intent(this, ControlTypeActivity::class.java)
        startActivity(intent)
    }

    private fun startThemeScreen() {
        val intent = Intent(this, ThemeActivity::class.java)
        startActivity(intent)
    }

    private fun startTutorial() {
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }
}
