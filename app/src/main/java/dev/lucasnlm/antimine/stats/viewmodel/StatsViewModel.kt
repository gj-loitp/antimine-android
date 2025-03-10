package dev.lucasnlm.antimine.stats.viewmodel

import dev.lucasnlm.antimine.R
import dev.lucasnlm.antimine.common.level.database.models.Stats
import dev.lucasnlm.antimine.common.level.repository.IMinefieldRepository
import dev.lucasnlm.antimine.common.level.repository.IStatsRepository
import dev.lucasnlm.antimine.core.repository.IDimensionRepository
import dev.lucasnlm.antimine.core.viewmodel.IntentViewModel
import dev.lucasnlm.antimine.preferences.IPreferencesRepository
import dev.lucasnlm.antimine.preferences.models.Minefield
import dev.lucasnlm.antimine.stats.model.StatsModel
import dev.lucasnlm.antimine.stats.model.StatsState
import kotlinx.coroutines.flow.flow

class StatsViewModel(
    private val statsRepository: IStatsRepository,
    private val preferenceRepository: IPreferencesRepository,
    private val minefieldRepository: IMinefieldRepository,
    private val dimensionRepository: IDimensionRepository,
) : IntentViewModel<StatsEvent, StatsState>() {
    private suspend fun loadStatsModel(): List<StatsModel> {
        val minId = preferenceRepository.getStatsBase()
        val stats = statsRepository.getAllStats(minId)
        val standardSize = minefieldRepository.baseStandardSize(dimensionRepository, 0)

        return with(stats) {
            listOf(
                // General
                fold().copy(title = R.string.general),

                // Progressive
                filterStandard(standardSize).fold().copy(title = R.string.progressive),

                // Fixed Size
                filter { isFixedSize(standardSize, it) }.fold().copy(title = R.string.fixed_size),

                // Legend
                filter(::isLegend).fold().copy(title = R.string.legend),

                // Master
                filter(::isMaster).fold().copy(title = R.string.master),

                // Expert
                filter(::isExpert).fold().copy(title = R.string.expert),

                // Intermediate
                filter(::isIntermediate).fold().copy(title = R.string.intermediate),

                // Beginner
                filter(::isBeginner).fold().copy(title = R.string.beginner),

                // Custom
                filterNot(::isExpert)
                    .filterNot(::isIntermediate)
                    .filterNot(::isBeginner)
                    .filterNotStandard(standardSize)
                    .fold()
                    .copy(title = R.string.custom),
            ).filter {
                it.totalGames > 0
            }
        }
    }

    private suspend fun deleteAll() {
        statsRepository.getAllStats(0).lastOrNull()?.let {
            preferenceRepository.updateStatsBase(it.uid + 1)
        }
    }

    private fun List<Stats>.fold(): StatsModel {
        return if (isNotEmpty()) {
            fold(
                StatsModel(
                    title = 0,
                    totalGames = size,
                    totalTime = 0,
                    victoryTime = 0,
                    averageTime = 0,
                    shortestTime = 0,
                    mines = 0,
                    victory = 0,
                    openArea = 0,
                ),
            ) { acc, value ->
                StatsModel(
                    title = 0,
                    totalGames = acc.totalGames,
                    totalTime = acc.totalTime + value.duration,
                    victoryTime = acc.victoryTime + if (value.victory != 0) {
                        value.duration
                    } else {
                        0
                    },
                    averageTime = 0,
                    shortestTime = if (value.victory != 0) {
                        if (acc.shortestTime == 0L) {
                            value.duration
                        } else {
                            acc.shortestTime.coerceAtMost(value.duration)
                        }
                    } else {
                        acc.shortestTime
                    },
                    mines = acc.mines + value.mines,
                    victory = acc.victory + value.victory,
                    openArea = acc.openArea + value.openArea,
                )
            }.run {
                if (victory > 0) {
                    copy(averageTime = victoryTime / victory)
                } else {
                    this
                }
            }
        } else {
            StatsModel(
                title = 0,
                totalGames = 0,
                totalTime = 0,
                victoryTime = 0,
                averageTime = 0,
                shortestTime = 0,
                mines = 0,
                victory = 0,
                openArea = 0,
            )
        }
    }

    override fun initialState() = StatsState(
        stats = listOf(),
    )

    override suspend fun mapEventToState(event: StatsEvent) = flow {
        when (event) {
            is StatsEvent.LoadStats -> {
                emit(state.copy(stats = loadStatsModel()))
            }
            is StatsEvent.DeleteStats -> {
                deleteAll()
                emit(state.copy(stats = loadStatsModel()))
            }
        }
    }

    private fun isExpert(stats: Stats): Boolean {
        return stats.mines == 99 && stats.width == 24 && stats.height == 24
    }

    private fun isMaster(stats: Stats): Boolean {
        return (stats.mines == 200 || stats.mines == 300 || stats.mines == 400) &&
            stats.width == 50 && stats.height == 50
    }

    private fun isLegend(stats: Stats): Boolean {
        return stats.mines == 2000 && stats.width == 100 && stats.height == 100
    }

    private fun isFixedSize(standardSize: Minefield, stats: Stats): Boolean {
        return stats.width == standardSize.width &&
            stats.height == standardSize.height &&
            stats.mines == standardSize.mines
    }

    private fun isIntermediate(stats: Stats): Boolean {
        return stats.mines == 40 && stats.width == 16 && stats.height == 16
    }

    private fun isBeginner(stats: Stats): Boolean {
        return stats.mines == 10 && stats.width == 9 && stats.height == 9
    }

    private fun List<Stats>.filterStandard(standardSize: Minefield) = filter {
        val baseWidth = (it.width - standardSize.width)
        val baseHeight = (it.height - standardSize.height)
        val baseWidthInv = (it.height - standardSize.width)
        val baseHeightInv = (it.width - standardSize.height)

        val baseCheck = (baseWidth >= 0 && baseWidth % 2 == 0 && baseHeight >= 0 && baseHeight % 2 == 0)
        val baseInvCheck = (baseWidthInv >= 0 && baseWidthInv % 2 == 0 && baseHeightInv >= 0 && baseHeightInv % 2 == 0)

        (baseCheck || baseInvCheck) &&
            listOf(::isExpert, ::isMaster, ::isLegend, ::isIntermediate, ::isBeginner)
                .any { func -> func.invoke(it) }
                .not()
    }

    private fun List<Stats>.filterNotStandard(standardSize: Minefield) = filterNot {
        (it.width == standardSize.width && it.height == standardSize.height) ||
            (it.width == standardSize.height && it.height == standardSize.width)
    }
}
