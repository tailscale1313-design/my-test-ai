package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NetNode Inspector", appName)
  }

  @Test
  fun `test 503 error simulation updates state`() {
    val viewModel = com.example.ui.NetworkViewModel()
    viewModel.testSimulate503()
    val state = viewModel.uiState.value
    assertEquals(503, state.lastResult?.statusCode)
    assertEquals(true, state.lastResult?.is503Error)
    assertEquals(1, state.history.size)
  }
}
