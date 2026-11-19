package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.system.SystemCompatibilityInspector
import com.example.ui.CleanMasterApp
import com.example.util.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name and package id match Clean Master AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Clean Master AI", appName)
        assertEquals("com.cleanmaster.ai", context.packageName)
    }

    @Test
    fun `byte formatter handles unavailable and sizes accurately`() {
        assertEquals("No disponible", ByteFormatter.formatBytes(null))
        assertEquals("No disponible", ByteFormatter.formatBytes(-1L))
        assertEquals("512 B", ByteFormatter.formatBytes(512L))
        assertTrue(ByteFormatter.formatBytes(1024L * 1024L).contains("MB"))
    }

    @Test
    fun `compatibility inspector adapts guidance for major OEMs`() {
        val samsungHint = SystemCompatibilityInspector.getOemStoragePathHint("Samsung")
        val xiaomiHint = SystemCompatibilityInspector.getOemStoragePathHint("Xiaomi")
        val motorolaHint = SystemCompatibilityInspector.getOemStoragePathHint("Motorola")
        assertTrue(samsungHint.contains("Samsung"))
        assertTrue(xiaomiHint.contains("Xiaomi"))
        assertTrue(motorolaHint.contains("Motorola"))
    }

    @Test
    fun `dashboard renders and navigates to apps and clean tabs`() {
        composeTestRule.setContent {
            CleanMasterApp()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Clean Master AI").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("dashboard_analyze_button"))
        composeTestRule.onNodeWithTag("dashboard_analyze_button").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("dashboard_multi_clean_button"))
        composeTestRule.onNodeWithTag("dashboard_multi_clean_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_apps").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Administrador de Apps").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_clean").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Centro de Limpieza Oficial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clean_tab_clear_data").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            "Esta operación puede eliminar cuentas iniciadas, configuraciones, archivos locales y datos guardados de la aplicación"
        ).assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Historial de Limpiezas").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Configuración").assertIsDisplayed()
    }
}
