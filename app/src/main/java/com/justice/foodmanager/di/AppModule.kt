package com.justice.foodmanager.di

import com.justice.foodmanager.data.StudentsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideStudentRepo(): StudentsRepository {
        return StudentsRepository()
    }


}

