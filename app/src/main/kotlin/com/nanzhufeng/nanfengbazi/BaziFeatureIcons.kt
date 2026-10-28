package com.nanzhufeng.nanfengbazi

import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WatchLater

/** Shared in-app feature glyphs. Consumers own semantic color; glyphs have no background tile. */
internal object BaziFeatureIcons {
    val Font get() = Icons.Rounded.TextFields
    val Cloud get() = Icons.Rounded.Cloud
    val Ai get() = Icons.Rounded.AutoAwesome
    val Backup get() = Icons.Rounded.Save
    val Restore get() = Icons.Rounded.SettingsBackupRestore
    val Screenshot get() = Icons.Rounded.PhotoLibrary
    val Web get() = Icons.Rounded.Public
    val Cases get() = Icons.Rounded.PeopleAlt
    val Import get() = Icons.Rounded.FolderOpen
    val DayBoundary get() = Icons.Rounded.WatchLater
    val About get() = Icons.Rounded.Info
    val Compatibility get() = Icons.Rounded.PeopleAlt
    val Calendar get() = Icons.Rounded.CalendarMonth
    val History get() = Icons.Rounded.History
    val ModelSettings get() = Icons.Rounded.Tune
}

internal object BaziFeatureColors {
    val Web = Color(0xFF527FA4)
    val DayBoundary = Color(0xFF806BA2)
}
