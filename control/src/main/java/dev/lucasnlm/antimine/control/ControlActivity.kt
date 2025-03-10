package dev.lucasnlm.antimine.control

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.slider.Slider
import dev.lucasnlm.antimine.control.databinding.ActivityControlBinding
import dev.lucasnlm.antimine.control.view.ControlAdapter
import dev.lucasnlm.antimine.control.viewmodel.ControlEvent
import dev.lucasnlm.antimine.control.viewmodel.ControlViewModel
import dev.lucasnlm.antimine.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.preferences.models.ControlStyle
import dev.lucasnlm.antimine.ui.ext.ThemedActivity
import dev.lucasnlm.antimine.ui.model.TopBarAction
import org.koin.android.ext.android.inject

class ControlActivity : ThemedActivity(), Slider.OnChangeListener {
    private lateinit var binding: ActivityControlBinding

    private val viewModel: ControlViewModel by inject()
    private val preferencesRepository: IPreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val controlAdapter = ControlAdapter(
            controls = mutableListOf(),
            selected = preferencesRepository.controlStyle(),
            onControlSelected = { controlStyle ->
                viewModel.sendEvent(ControlEvent.SelectControlStyle(controlStyle))
            },
        )

        binding.recyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(context)
            adapter = controlAdapter
        }

        binding.touchSensibility.addOnChangeListener(this)
        binding.longPress.addOnChangeListener(this)
        binding.doubleClick.addOnChangeListener(this)
        binding.hapticLevel.addOnChangeListener(this)

        lifecycleScope.launchWhenCreated {
            viewModel.observeState().collect {
                controlAdapter.bindControlStyleList(it.selected, it.controls)
                val longPress: Slider = binding.longPress
                val touchSensibility: Slider = binding.touchSensibility
                val hapticLevel: Slider = binding.hapticLevel

                longPress.value = (it.longPress.toFloat() / longPress.stepSize).toInt() * longPress.stepSize
                touchSensibility.value =
                    (it.touchSensibility.toFloat() / touchSensibility.stepSize).toInt() * touchSensibility.stepSize

                val longPressVisible = when (it.selected) {
                    ControlStyle.Standard, ControlStyle.FastFlag -> View.VISIBLE
                    else -> View.GONE
                }
                longPress.visibility = longPressVisible
                binding.longPressLabel.visibility = longPressVisible

                val doubleClickVisible = when (it.selected) {
                    ControlStyle.DoubleClick, ControlStyle.DoubleClickInverted -> View.VISIBLE
                    else -> View.GONE
                }
                binding.doubleClick.visibility = doubleClickVisible
                binding.doubleClickLabel.visibility = doubleClickVisible
                binding.doubleClick.value = it.doubleClick.toFloat()

                hapticLevel.value =
                    (it.hapticFeedbackLevel.toFloat() / hapticLevel.stepSize).toInt() * hapticLevel.stepSize

                if (it.showReset) {
                    setTopBarAction(
                        TopBarAction(
                            name = R.string.delete_all,
                            icon = R.drawable.undo,
                            action = {
                                viewModel.sendEvent(ControlEvent.Reset)
                            },
                        ),
                    )
                } else {
                    setTopBarAction(null)
                }
            }
        }

        bindToolbar(binding.toolbar)
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        if (fromUser) {
            val progress = value.toInt()
            when (slider) {
                binding.touchSensibility -> {
                    viewModel.sendEvent(ControlEvent.UpdateTouchSensibility(progress))
                }
                binding.longPress -> {
                    viewModel.sendEvent(ControlEvent.UpdateLongPress(progress))
                }
                binding.doubleClick -> {
                    viewModel.sendEvent(ControlEvent.UpdateDoubleClick(progress))
                }
                binding.hapticLevel -> {
                    viewModel.sendEvent(ControlEvent.UpdateHapticFeedbackLevel(progress))
                }
            }
        }
    }
}
