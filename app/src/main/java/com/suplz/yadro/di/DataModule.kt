package com.suplz.yadro.di

import android.content.Context
import com.suplz.yadro.data.repository.ContactsRepositoryImpl
import com.suplz.yadro.domain.repository.ContactsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideContactsRepository(
        @ApplicationContext context: Context
    ): ContactsRepository {
        return ContactsRepositoryImpl(context)
    }
}