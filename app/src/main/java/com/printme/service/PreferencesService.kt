package com.printme.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.printme.model.LayoutType
import com.printme.model.MarginConfig
import com.printme.model.PaperSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "print_me_settings")

/**
 * Service for storing and retrieving user preferences
 */
class PreferencesService(private val context: Context) {

    companion object {
        private val LAYOUT_TYPE = stringPreferencesKey("layout_type")
        private val PAPER_SIZE = stringPreferencesKey("paper_size")
        private val MARGIN_TOP = floatPreferencesKey("margin_top")
        private val MARGIN_BOTTOM = floatPreferencesKey("margin_bottom")
        private val MARGIN_LEFT = floatPreferencesKey("margin_left")
        private val MARGIN_RIGHT = floatPreferencesKey("margin_right")
        private val COPIES = intPreferencesKey("copies")
    }

    val layoutType: Flow<LayoutType> = context.dataStore.data.map { prefs ->
        val name = prefs[LAYOUT_TYPE] ?: LayoutType.FOUR_PER_PAGE.name
        try {
            LayoutType.valueOf(name)
        } catch (e: Exception) {
            LayoutType.FOUR_PER_PAGE
        }
    }

    val paperSize: Flow<PaperSize> = context.dataStore.data.map { prefs ->
        val name = prefs[PAPER_SIZE] ?: PaperSize.A4.name
        try {
            PaperSize.valueOf(name)
        } catch (e: Exception) {
            PaperSize.A4
        }
    }

    val marginConfig: Flow<MarginConfig> = context.dataStore.data.map { prefs ->
        MarginConfig(
            topMm = prefs[MARGIN_TOP] ?: MarginConfig.INSTANT_CAMERA.topMm,
            bottomMm = prefs[MARGIN_BOTTOM] ?: MarginConfig.INSTANT_CAMERA.bottomMm,
            leftMm = prefs[MARGIN_LEFT] ?: MarginConfig.INSTANT_CAMERA.leftMm,
            rightMm = prefs[MARGIN_RIGHT] ?: MarginConfig.INSTANT_CAMERA.rightMm
        )
    }

    val copies: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COPIES] ?: 1
    }

    suspend fun saveLayoutType(layoutType: LayoutType) {
        context.dataStore.edit { prefs ->
            prefs[LAYOUT_TYPE] = layoutType.name
        }
    }

    suspend fun savePaperSize(paperSize: PaperSize) {
        context.dataStore.edit { prefs ->
            prefs[PAPER_SIZE] = paperSize.name
        }
    }

    suspend fun saveMarginConfig(marginConfig: MarginConfig) {
        context.dataStore.edit { prefs ->
            prefs[MARGIN_TOP] = marginConfig.topMm
            prefs[MARGIN_BOTTOM] = marginConfig.bottomMm
            prefs[MARGIN_LEFT] = marginConfig.leftMm
            prefs[MARGIN_RIGHT] = marginConfig.rightMm
        }
    }

    suspend fun saveCopies(copies: Int) {
        context.dataStore.edit { prefs ->
            prefs[COPIES] = copies
        }
    }
}
