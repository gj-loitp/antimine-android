package dev.lucasnlm.antimine.themes

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.lucasnlm.antimine.core.cloud.CloudSaveManager
import dev.lucasnlm.antimine.core.models.Analytics
import dev.lucasnlm.antimine.core.repository.IDimensionRepository
import dev.lucasnlm.antimine.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.themes.databinding.ActivityThemeBinding
import dev.lucasnlm.antimine.themes.view.SkinAdapter
import dev.lucasnlm.antimine.themes.view.ThemeAdapter
import dev.lucasnlm.antimine.themes.viewmodel.ThemeEvent
import dev.lucasnlm.antimine.themes.viewmodel.ThemeViewModel
import dev.lucasnlm.antimine.ui.ext.ThemedActivity
import dev.lucasnlm.antimine.ui.view.SpaceItemDecoration
import dev.lucasnlm.external.IAdsManager
import dev.lucasnlm.external.IAnalyticsManager
import dev.lucasnlm.external.IBillingManager
import dev.lucasnlm.external.model.PurchaseInfo
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ThemeActivity : ThemedActivity() {
    private lateinit var binding: ActivityThemeBinding

    private val themeViewModel by viewModel<ThemeViewModel>()

    private val dimensionRepository: IDimensionRepository by inject()
    private val cloudSaveManager by inject<CloudSaveManager>()
    private val preferencesRepository: IPreferencesRepository by inject()
    private val billingManager: IBillingManager by inject()
    private val adsManager: IAdsManager by inject()
    private val analyticsManager: IAnalyticsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analyticsManager.sentEvent(Analytics.OpenThemes)

        if (!preferencesRepository.isPremiumEnabled()) {
            adsManager.start(this)
        }

        bindToolbar(binding.toolbar)

        if (preferencesRepository.isPremiumEnabled()) {
            binding.unlockAll.visibility = View.GONE
        } else {
            binding.unlockAll.bind(
                theme = usingTheme,
                invert = true,
                text = getString(R.string.unlock_all),
                onAction = {
                    lifecycleScope.launch {
                        billingManager.charge(this@ThemeActivity)
                    }
                },
            )

            lifecycleScope.launchWhenResumed {
                billingManager.getPriceFlow().collect {
                    binding.unlockAll.bind(
                        theme = usingTheme,
                        invert = true,
                        text = getString(R.string.unlock_all),
                        price = it.price,
                        showOffer = it.offer,
                        onAction = {
                            lifecycleScope.launch {
                                billingManager.charge(this@ThemeActivity)
                            }
                        },
                    )
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            val size = dimensionRepository.displaySize()
            val themesColumns = if (size.width > size.height) { 5 } else { 3 }
            val skinsColumns = if (size.width > size.height) { 2 } else { 5 }

            val themeAdapter = ThemeAdapter(
                themeViewModel = themeViewModel,
                preferencesRepository = preferencesRepository,
                onSelectTheme = { theme ->
                    themeViewModel.sendEvent(ThemeEvent.ChangeTheme(theme))
                },
                onRequestPurchase = {
                    lifecycleScope.launch {
                        billingManager.charge(this@ThemeActivity)
                    }
                },
            )

            val skinAdapter = SkinAdapter(
                themeRepository = themeRepository,
                themeViewModel = themeViewModel,
                preferencesRepository = preferencesRepository,
                onSelectSkin = { skin ->
                    themeViewModel.sendEvent(ThemeEvent.ChangeSkin(skin))
                },
                onRequestPurchase = {
                    lifecycleScope.launch {
                        billingManager.charge(this@ThemeActivity)
                    }
                },
            )

            binding.themes.apply {
                addItemDecoration(SpaceItemDecoration(R.dimen.theme_divider))
                setHasFixedSize(true)
                layoutManager = GridLayoutManager(context, themesColumns)
                adapter = themeAdapter
            }

            binding.skins.apply {
                addItemDecoration(SpaceItemDecoration(R.dimen.theme_divider))
                setHasFixedSize(true)
                layoutManager = object : GridLayoutManager(context, skinsColumns) {
                    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
                        val lpSize = width / (skinsColumns + 1)
                        lp?.height = lpSize
                        lp?.width = lpSize
                        return true
                    }
                }
                adapter = skinAdapter
            }

            if (!preferencesRepository.isPremiumEnabled()) {
                lifecycleScope.launchWhenResumed {
                    billingManager.listenPurchases().collect {
                        if (it is PurchaseInfo.PurchaseResult && it.unlockStatus) {
                            themeAdapter.notifyItemRangeChanged(0, themeAdapter.itemCount)
                        }
                    }
                }
            }

            launch {
                themeViewModel.observeEvent().collect {
                    if (it is ThemeEvent.Unlock) {
                        billingManager.charge(this@ThemeActivity)
                    }
                }
            }

            launch {
                themeViewModel.observeState().collect {
                    if (usingTheme != it.currentTheme || usingSkin != it.currentAppSkin) {
                        recreate()
                        cloudSaveManager.uploadSave()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val themes = binding.themes
        outState.putIntArray(SCROLL_VIEW_STATE, intArrayOf(themes.scrollX, themes.scrollY))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getIntArray(SCROLL_VIEW_STATE)?.let { position ->
            val themes = binding.themes
            themes.post { themes.scrollTo(position[0], position[1]) }
        }
    }

    companion object {
        const val SCROLL_VIEW_STATE = "SCROLL_VIEW_POSITION"
    }
}
