package com.nanzhufeng.nanfengbazi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings

/** 导航只拥有视觉语义，路由、点击与无障碍名称仍由现有消费者持有。 */
internal object BaziNavigationIcons {
    val Back get() = Icons.AutoMirrored.Filled.KeyboardArrowLeft
    val Chart get() = Icons.Filled.Home
    val Records get() = Icons.Filled.MenuBook
    val Settings get() = Icons.Filled.Settings
}
