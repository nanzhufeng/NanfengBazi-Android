package com.nanzhufeng.nanfengbazi

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StageTwoSystemStateRestoreTest {
    @Test
    fun systemTaskRestoresCreateDraftAfterBackgroundActivityDestruction() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val oldAlwaysFinish = device.executeShellCommand(
            "settings get global always_finish_activities",
        ).trim()
        val alias = "StateRestore${System.currentTimeMillis()}"
        try {
            device.executeShellCommand(
                "am start -W -n com.nanzhufeng.nanfengbazi/.MainActivity",
            )
            val chartEntry = requireNotNull(
                device.wait(Until.findObject(By.text("排盘")), 10_000),
            )
            chartEntry.click()
            val aliasField = requireNotNull(
                device.wait(Until.findObject(By.text("命例别名 *")), 10_000),
            )
            aliasField.click()
            device.executeShellCommand("input text $alias")
            check(device.wait(Until.hasObject(By.text(alias)), 5_000)) {
                "新建页应显示刚输入的未提交别名"
            }

            device.executeShellCommand(
                "settings put global always_finish_activities 1",
            )
            device.pressHome()
            device.wait(Until.hasObject(By.pkg("com.google.android.apps.nexuslauncher")), 5_000)
            device.executeShellCommand(
                "am start -W -n com.nanzhufeng.nanfengbazi/.MainActivity",
            )

            check(device.wait(Until.hasObject(By.text(alias)), 10_000)) {
                "系统销毁后台 Activity 后应恢复新建页未提交别名"
            }
            check(device.hasObject(By.text("排盘"))) {
                "系统销毁后台 Activity 后应恢复原页面"
            }
        } finally {
            if (oldAlwaysFinish == "null" || oldAlwaysFinish.isBlank()) {
                device.executeShellCommand(
                    "settings delete global always_finish_activities",
                )
            } else {
                device.executeShellCommand(
                    "settings put global always_finish_activities $oldAlwaysFinish",
                )
            }
        }
    }
}
