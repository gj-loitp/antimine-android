package dev.lucasnlm.antimine.stats

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.lucasnlm.antimine.R
import dev.lucasnlm.antimine.databinding.ActivityStatsBinding
import dev.lucasnlm.antimine.stats.view.StatsAdapter
import dev.lucasnlm.antimine.stats.viewmodel.StatsEvent
import dev.lucasnlm.antimine.stats.viewmodel.StatsViewModel
import dev.lucasnlm.antimine.ui.ext.ThemedActivity
import dev.lucasnlm.antimine.ui.model.TopBarAction
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatsActivity : ThemedActivity() {
    private val statsViewModel by viewModel<StatsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.stats.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        lifecycleScope.launchWhenResumed {
            statsViewModel.sendEvent(StatsEvent.LoadStats)

            statsViewModel.observeState().collect {
                if (it.stats.isNotEmpty()) {
                    setTopBarAction(
                        TopBarAction(
                            name = R.string.delete_all,
                            icon = R.drawable.delete,
                            action = { confirmAndDelete() },
                        ),
                    )
                }

                binding.stats.adapter = StatsAdapter(it.stats)
                binding.empty.visibility = if (it.stats.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        bindToolbar(binding.toolbar)
    }

    private fun confirmAndDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.are_you_sure)
            .setMessage(R.string.delete_all_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_all) { _, _ ->
                lifecycleScope.launch {
                    statsViewModel.sendEvent(StatsEvent.DeleteStats)
                }
                setTopBarAction(null)
            }
            .show()
    }
}
