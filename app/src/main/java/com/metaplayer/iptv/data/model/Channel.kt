package com.metaplayer.iptv.data.model

import com.metaplayer.iptv.data.util.CategoryDetector

data class Channel(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val tvgLogo: String? = null,
    val tvgShift: String? = null,
    val radio: Boolean = false,
    val catchup: String? = null,
    val category: ChannelCategory = CategoryDetector.detectCategory(url, group, name)
)
