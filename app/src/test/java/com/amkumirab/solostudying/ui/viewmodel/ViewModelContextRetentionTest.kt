package com.amkumirab.solostudying.ui.viewmodel

import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amkumirab.solostudying.data.database.SoloStudyingDatabase
import com.amkumirab.solostudying.data.repository.SoloStudyingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ViewModelContextRetentionTest {
    @Test fun `long lived view models retain only the application context`() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val screenContext = ContextWrapper(application)
        val database = Room.inMemoryDatabaseBuilder(application, SoloStudyingDatabase::class.java).build()
        val repository = SoloStudyingRepository(database)
        val models = ViewModelStore()
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            listOf(BattleViewModel(repository, screenContext), StatusViewModel(repository, screenContext),
                TutorialViewModel(repository, screenContext)).forEachIndexed { index, model ->
                models.put("model_$index", model)
                model.javaClass.declaredFields.filter { Context::class.java.isAssignableFrom(it.type) }
                    .forEach { field ->
                        field.isAccessible = true
                        assertSame("${model.javaClass.simpleName} must not retain a screen context",
                            application, field.get(model))
                    }
            }
        } finally {
            models.clear()
            database.close()
            Dispatchers.resetMain()
        }
    }
}
