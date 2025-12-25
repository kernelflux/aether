package com.kernelflux.aether.ui.widget.tab

import android.graphics.drawable.Drawable


data class HomeTabData(
    var lottieUrl: String = "",
    var pageType: Int = 0,
    var selectColor: String = "",
    var selectIcon: String = "",
    var tabName: String = "",
    var unSelectColor: String = "",
    var unSelectIcon: String = ""
)

class HomeTabDataWrapper(
    private val mHomeTabData: HomeTabData,
    private val mDrawableWrapper: DrawableWrapper
) {

    fun getHomeTabData(): HomeTabData {
        return mHomeTabData
    }

    fun getHomeTabIcon(): DrawableWrapper {
        return mDrawableWrapper
    }

    class DrawableWrapper {
        var selectedDrawable: Drawable? = null
        var normalDrawable: Drawable? = null
        var width: Int = 0
        var height: Int = 0
    }
}