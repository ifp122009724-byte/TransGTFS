package com.transgtfs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GtfsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrips(trips: List<TripEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopTimes(stopTimes: List<StopTimeEntity>)

    @Query("SELECT DISTINCT trip_id FROM stop_times WHERE stop_id = :stopId")
    suspend fun tripsForStop(stopId: String): List<String>

    @Query(
        """
        SELECT trip_id, stop_id, stop_sequence, arrival_time, departure_time, id
        FROM stop_times
        WHERE trip_id = :tripId
        ORDER BY stop_sequence
        """
    )
    suspend fun stopTimesForTrip(tripId: String): List<StopTimeEntity>

    @Query("SELECT * FROM stops WHERE stop_id IN (:stopIds)")
    suspend fun stopsByIds(stopIds: List<String>): List<StopEntity>
}
