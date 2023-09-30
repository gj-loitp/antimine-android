package dev.lucasnlm.antimine.common.io

import android.content.Context
import dev.lucasnlm.antimine.common.io.models.StatsFile
import dev.lucasnlm.antimine.common.io.serializer.StatsSerializer
import dev.lucasnlm.antimine.common.io.serializer.StatsSerializer.readStatsFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream

/**
 * Handles the file that stores the stats
 * @param context The application context
 */
class StatsFileManagerImpl(
    private val context: Context,
    private val scope: CoroutineScope,
) : StatsFileManager {
    override suspend fun insert(stats: StatsFile) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val statsBytes = StatsSerializer.serialize(stats)
                context.filesDir.resolve(filePath).appendBytes(statsBytes)
            }
        }
    }

    override suspend fun readStats(): List<StatsFile> {
        return withContext(Dispatchers.IO) {
            runCatching {
                context.filesDir.resolve(filePath).readBytes().let { bytes ->
                    val result = mutableListOf<StatsFile>()
                    bytes.inputStream().use { inputStream ->
                        DataInputStream(inputStream).use { stream ->
                            do {
                                val stats = stream.readStatsFile()?.also {
                                    result.add(it)
                                }
                            } while (stats != null)
                        }
                    }
                    result
                }
            }.getOrNull().orEmpty()
        }
    }

    override suspend fun deleteStats() {
        withContext(Dispatchers.IO) {
            context.filesDir.resolve(filePath).delete()
        }
    }

    companion object {
        private const val filePath = "stats"
    }
}
