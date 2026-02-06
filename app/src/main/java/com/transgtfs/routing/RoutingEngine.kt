package com.transgtfs.routing

import com.transgtfs.data.db.GtfsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class RoutingEngine(
    private val gtfsDao: GtfsDao
) {
    suspend fun findRoute(startStopId: String, endStopId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (startStopId == endStopId) {
                    return@runCatching listOf(startStopId)
                }

                val visited = mutableSetOf<String>()
                val predecessor = mutableMapOf<String, String>()
                val queue = ArrayDeque<String>()
                queue.add(startStopId)
                visited.add(startStopId)

                while (queue.isNotEmpty()) {
                    val currentStop = queue.removeFirst()
                    val tripIds = gtfsDao.tripsForStop(currentStop)
                    for (tripId in tripIds) {
                        val stopTimes = gtfsDao.stopTimesForTrip(tripId)
                        val nextStops = nextStopsForTrip(currentStop, stopTimes)
                        for (nextStop in nextStops) {
                            if (visited.add(nextStop)) {
                                predecessor[nextStop] = currentStop
                                if (nextStop == endStopId) {
                                    return@runCatching buildPath(predecessor, startStopId, endStopId)
                                }
                                queue.add(nextStop)
                            }
                        }
                    }
                }
                emptyList()
            }
        }

    private fun nextStopsForTrip(
        stopId: String,
        stopTimes: List<com.transgtfs.data.db.StopTimeEntity>
    ): List<String> {
        val ordered = stopTimes.sortedBy { it.stopSequence }
        val currentIndex = ordered.indexOfFirst { it.stopId == stopId }
        return if (currentIndex >= 0 && currentIndex + 1 < ordered.size) {
            listOf(ordered[currentIndex + 1].stopId)
        } else {
            emptyList()
        }
    }

    private fun buildPath(
        predecessor: Map<String, String>,
        startStopId: String,
        endStopId: String
    ): List<String> {
        val path = mutableListOf<String>()
        var current = endStopId
        while (current != startStopId) {
            path.add(current)
            current = predecessor[current] ?: return emptyList()
        }
        path.add(startStopId)
        return path.reversed()
    }
}
