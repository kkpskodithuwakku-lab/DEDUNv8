package com.example

import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertEquals("DEDUN", appName)
  }

  @Test
  fun `remote views layout inflation test for widgets`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val viewsLarge = RemoteViews(context.packageName, R.layout.widget_large)
    val inflatedLarge = viewsLarge.apply(context, FrameLayout(context))
    assertNotNull(inflatedLarge)

    val viewsSmall = RemoteViews(context.packageName, R.layout.widget_small)
    val inflatedSmall = viewsSmall.apply(context, FrameLayout(context))
    assertNotNull(inflatedSmall)
  }

  @Test
  fun `test widget provider update with mock data`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    DedunWidgetProvider.updateAllWidgets(context)
    DedunWidgetSmallProvider.updateAllWidgets(context)
  }
}
