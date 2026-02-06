package com.transgtfs.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StopEntity::class, TripEntity::class, StopTimeEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GtfsDatabase : RoomDatabase() {
    abstract fun gtfsDao(): GtfsDao
}
