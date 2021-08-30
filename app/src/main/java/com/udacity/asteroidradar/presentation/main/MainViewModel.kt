package com.udacity.asteroidradar.presentation.main

import android.app.Application
import androidx.lifecycle.*
import com.udacity.asteroidradar.data.database.getDatabase
import com.udacity.asteroidradar.data.repository.AsteroidRepository
import com.udacity.asteroidradar.domain.Asteroid
import com.udacity.asteroidradar.domain.PictureOfDay
import com.udacity.asteroidradar.util.AsteroidFilter
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : ViewModel() {

    private val database = getDatabase(application)
    private val repository = AsteroidRepository(database)

    private var _pictureOfDay = MutableLiveData<PictureOfDay>()
    val pictureOfDay: LiveData<PictureOfDay>
        get() = _pictureOfDay

    private val _navigateToDetailsAsteroid = MutableLiveData<Asteroid?>()
    val navigateToDetailsAsteroid: LiveData<Asteroid?>
        get() = _navigateToDetailsAsteroid

    private val _steroidFilter = MutableLiveData(AsteroidFilter.ALL)

    val asteroidList = Transformations.switchMap(_steroidFilter) {
        when (it!!) {
            AsteroidFilter.TODAY -> repository.todayAsteroids
            AsteroidFilter.WEEK -> repository.weekAsteroids
            else -> repository.allAsteroids
        }
    }

    init {
        viewModelScope.launch {
            _pictureOfDay.value = repository.getPictureOfDay().value
            repository.refreshAsteroids()
        }
    }

    fun onAsteroidClicked(asteroid: Asteroid) {
        _navigateToDetailsAsteroid.value = asteroid
    }

    fun onAsteroidNavigated() {
        _navigateToDetailsAsteroid.value = null
    }

    fun onFilterChanged(filter: AsteroidFilter) {
        _steroidFilter.value = filter
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct ViewModel")
        }
    }
}