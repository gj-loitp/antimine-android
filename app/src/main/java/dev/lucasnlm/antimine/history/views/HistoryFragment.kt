package dev.lucasnlm.antimine.history.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lucasnlm.antimine.databinding.FragmentHistoryBinding
import dev.lucasnlm.antimine.history.viewmodel.HistoryEvent
import dev.lucasnlm.antimine.history.viewmodel.HistoryViewModel
import kotlinx.coroutines.flow.collect
import org.koin.androidx.viewmodel.ext.android.viewModel

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private val historyViewModel by viewModel<HistoryViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            historyViewModel.sendEvent(HistoryEvent.LoadAllSaves)

            historyViewModel.observeState().collect {
                binding.empty.visibility = if (!it.loading && it.saveList.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                binding.loading.visibility = if (it.loading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                binding.saveHistory.apply {
                    layoutManager = LinearLayoutManager(view.context)
                    adapter = HistoryAdapter(it.saveList, historyViewModel)
                }
            }
        }
    }
}
