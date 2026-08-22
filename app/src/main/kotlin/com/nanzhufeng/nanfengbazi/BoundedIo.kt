package com.nanzhufeng.nanfengbazi

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Reads untrusted UTF-8 data without allocating past the declared boundary. */
internal fun InputStream.readUtf8Bounded(maxBytes: Int): String {
    require(maxBytes > 0) { "读取上限必须为正数" }
    val output = ByteArrayOutputStream(minOf(DEFAULT_BUFFER_SIZE, maxBytes))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        if (count == 0) continue
        total += count
        require(total <= maxBytes) { "文件或服务响应超过允许大小。" }
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}
