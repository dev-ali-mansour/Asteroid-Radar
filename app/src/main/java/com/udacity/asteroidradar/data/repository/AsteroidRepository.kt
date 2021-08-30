package com.udacity.asteroidradar.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.data.api.Network
import com.udacity.asteroidradar.data.api.parseAsteroidsJsonResult
import com.udacity.asteroidradar.data.database.AsteroidDatabase
import com.udacity.asteroidradar.domain.Asteroid
import com.udacity.asteroidradar.domain.PictureOfDay
import com.udacity.asteroidradar.util.Constants.API_KEY
import com.udacity.asteroidradar.util.asDatabaseModel
import com.udacity.asteroidradar.util.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AsteroidRepository(private val database: AsteroidDatabase) {

    @RequiresApi(Build.VERSION_CODES.O)
    private val startDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)

    @RequiresApi(Build.VERSION_CODES.O)
    private val endDate = LocalDateTime.now().plusDays(6).format(DateTimeFormatter.ISO_DATE)

    val allAsteroids: LiveData<List<Asteroid>> =
        Transformations.map(database.asteroidDao.getAsteroids()) { it.asDomainModel() }

    @RequiresApi(Build.VERSION_CODES.O)
    val todayAsteroids: LiveData<List<Asteroid>> =
        Transformations.map(database.asteroidDao.getAsteroidsByDate(startDate)) {
            it.asDomainModel()
        }

    @RequiresApi(Build.VERSION_CODES.O)
    val weekAsteroids: LiveData<List<Asteroid>> =
        Transformations.map(database.asteroidDao.getAsteroidsWithinTime(startDate, endDate)) {
            it.asDomainModel()
        }

    suspend fun refreshAsteroids() {
        withContext(Dispatchers.IO) {
            runCatching {
                val data = Network.asteroidService.getAsteroids(API_KEY)
                val asteroids = parseAsteroidsJsonResult(JSONObject(data))
                database.asteroidDao.insertAll(*asteroids.asDatabaseModel())
                Timber.d("Asteroids data was refreshed successfully")
            }.onFailure {
                Timber.e("Failed to refresh asteroids data! " + it.message)
            }
        }
    }

    suspend fun getPictureOfDay(): MutableLiveData<PictureOfDay> {
        val pictureOfDay = MutableLiveData<PictureOfDay>()
        withContext(Dispatchers.IO) {
            runCatching {
                pictureOfDay.postValue(Network.asteroidService.getPictureOfTheDay(API_KEY))
            }.onFailure {
                Timber.e("Failed to get picture of today! " + it.message)
            }
        }
        return pictureOfDay
    }
}