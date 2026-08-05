package com.fueru.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueru.app.data.entity.Charity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharityDao {

    @Query("SELECT * FROM charity ORDER BY name")
    fun observeAll(): Flow<List<Charity>>

    @Query("SELECT * FROM charity WHERE sentiment = :sentiment")
    suspend fun getBySentiment(sentiment: String): List<Charity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charity: Charity): Long

    @Delete
    suspend fun delete(charity: Charity)
}
