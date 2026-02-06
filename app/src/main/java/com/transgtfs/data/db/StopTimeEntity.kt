package com.transgtfs.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stop_times")
data class StopTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "trip_id")
    val tripId: String,
    @ColumnInfo(name = "stop_id")
    val stopId: String,
    @ColumnInfo(name = "stop_sequence")
    val stopSequence: Int,
    @ColumnInfo(name = "arrival_time")
    val arrivalTime: String?,
    @ColumnInfo(name = "departure_time")
    val departureTime: String?
)
