package com.ozgurzorlu.travelbook.roomdb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ozgurzorlu.travelbook.model.Place

@Database(entities = arrayOf(Place::class), version = 1)
abstract class PlaceDB : RoomDatabase() {
    abstract fun placeDao() : Dao
}