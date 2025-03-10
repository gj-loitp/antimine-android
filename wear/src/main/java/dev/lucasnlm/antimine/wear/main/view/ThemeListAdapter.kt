package dev.lucasnlm.antimine.wear.main.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import dev.lucasnlm.antimine.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.ui.ext.toAndroidColor
import dev.lucasnlm.antimine.ui.ext.toInvertedAndroidColor
import dev.lucasnlm.antimine.ui.model.AppTheme
import dev.lucasnlm.antimine.wear.R
import dev.lucasnlm.antimine.wear.databinding.ViewThemeBinding

class ThemeListAdapter(
    private val themes: List<AppTheme>,
    private val onSelectTheme: (AppTheme) -> Unit,
    private val preferencesRepository: IPreferencesRepository,
) : RecyclerView.Adapter<ThemeListAdapter.RecyclerViewHolder>() {
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val binding = ViewThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecyclerViewHolder(
            binding = binding,
            preferencesRepository = preferencesRepository,
            onSelectTheme = onSelectTheme,
        )
    }

    override fun getItemCount(): Int {
        return themes.size
    }

    override fun getItemId(position: Int): Long {
        return themes[position].id
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val theme = themes[position]
        holder.bind(theme)
    }

    class RecyclerViewHolder(
        private val binding: ViewThemeBinding,
        private val preferencesRepository: IPreferencesRepository,
        private val onSelectTheme: (AppTheme) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(theme: AppTheme) {
            val selected = (theme.id == preferencesRepository.themeId())

            binding.covered.setBackgroundColor(theme.palette.covered.toAndroidColor())
            binding.uncovered.setBackgroundColor(theme.palette.background.toAndroidColor())

            if (theme.name != null) {
                binding.label.apply {
                    text = context.getString(theme.name!!)
                    setTextColor(theme.palette.background.toInvertedAndroidColor(200))
                    setBackgroundResource(android.R.color.transparent)
                    setCompoundDrawables(null, null, null, null)
                    visibility = View.VISIBLE
                }
            } else {
                binding.label.apply {
                    setCompoundDrawables(null, null, null, null)
                    visibility = View.GONE
                }
            }

            binding.cardTheme.apply {
                setStrokeColor(
                    MaterialColors.getColorStateListOrNull(
                        context,
                        if (selected) R.attr.colorTertiary else R.attr.backgroundColor,
                    ),
                )
                setOnClickListener {
                    onSelectTheme(theme)
                }
            }
        }
    }
}
