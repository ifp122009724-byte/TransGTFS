package com.transgtfs.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    @ColumnInfo(name = "trip_id")
    val tripId: String,
    @ColumnInfo(name = "route_id")
    val routeId: String?,
    @ColumnInfo(name = "service_id")
    val serviceId: String?
)
