package com.transgtfs.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.transgtfs.data.db.GtfsDao
import com.transgtfs.data.db.StopEntity
import com.transgtfs.data.db.StopTimeEntity
import com.transgtfs.data.db.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class GtfsRepository(
    private val contentResolver: ContentResolver,
    private val gtfsDao: GtfsDao
) {
    suspend fun importGtfsZip(gtfsZipUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(gtfsZipUri).use { inputStream ->
                requireNotNull(inputStream) { "Unable to open GTFS zip from $gtfsZipUri" }
                parseZipAndInsert(inputStream)
            }
        }
    }

    private suspend fun parseZipAndInsert(zipStream: InputStream) {
        ZipInputStream(BufferedInputStream(zipStream)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name.lowercase()) {
                    "stops.txt" -> parseStops(zis)
                    "trips.txt" -> parseTrips(zis)
                    "stop_times.txt" -> parseStopTimes(zis)
                }
                entry = zis.nextEntry
            }
        }
    }

    private suspend fun parseStops(inputStream: InputStream) {
        val parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(inputStream.reader())
        val stops = parser.mapNotNull { record ->
            val stopId = record.get("stop_id")?.trim().orEmpty()
            val name = record.get("stop_name")?.trim().orEmpty()
            val lat = record.get("stop_lat")?.toDoubleOrNull()
            val lon = record.get("stop_lon")?.toDoubleOrNull()
            if (stopId.isBlank() || name.isBlank() || lat == null || lon == null) {
                null
            } else {
                StopEntity(
                    stopId = stopId,
                    stopName = name,
                    stopLat = lat,
                    stopLon = lon
                )
            }
        }
        if (stops.isNotEmpty()) {
            gtfsDao.insertStops(stops)
        }
    }

    private suspend fun parseTrips(inputStream: InputStream) {
        val parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(inputStream.reader())
        val trips = parser.mapNotNull { record ->
            val tripId = record.get("trip_id")?.trim().orEmpty()
            if (tripId.isBlank()) {
                null
            } else {
                TripEntity(
                    tripId = tripId,
                    routeId = record.get("route_id")?.trim(),
                    serviceId = record.get("service_id")?.trim()
                )
            }
        }
        if (trips.isNotEmpty()) {
            gtfsDao.insertTrips(trips)
        }
    }

    private suspend fun parseStopTimes(inputStream: InputStream) {
        val parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(inputStream.reader())
        val buffer = mutableListOf<StopTimeEntity>()
        parser.forEach { record ->
            val tripId = record.get("trip_id")?.trim().orEmpty()
            val stopId = record.get("stop_id")?.trim().orEmpty()
            val sequence = record.get("stop_sequence")?.toIntOrNull()
            if (tripId.isNotBlank() && stopId.isNotBlank() && sequence != null) {
                buffer.add(
                    StopTimeEntity(
                        tripId = tripId,
                        stopId = stopId,
                        stopSequence = sequence,
                        arrivalTime = record.get("arrival_time")?.trim(),
                        departureTime = record.get("departure_time")?.trim()
                    )
                )
            }
            if (buffer.size >= 1000) {
                gtfsDao.insertStopTimes(buffer.toList())
                buffer.clear()
            }
        }
        if (buffer.isNotEmpty()) {
            gtfsDao.insertStopTimes(buffer)
        }
    }
}
