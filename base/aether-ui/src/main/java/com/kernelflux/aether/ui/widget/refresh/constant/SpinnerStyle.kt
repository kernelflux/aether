package com.kernelflux.aether.ui.widget.refresh.constant

/**
 * 顶部和底部的组件在拖动时候的变换方式
 * Created by scwang on 2017/5/26.
 */
open class SpinnerStyle protected constructor(
    @JvmField val ordinal: Int,
    @JvmField val front: Boolean,
    @JvmField val scale: Boolean
) {
    companion object {
        @JvmField
        val Translate: SpinnerStyle = SpinnerStyle(0, front = true, scale = false)

        @JvmField
        val FixedBehind: SpinnerStyle = SpinnerStyle(2, front = false, scale = false)

        @JvmField
        val FixedFront: SpinnerStyle = SpinnerStyle(3, front = true, scale = false)

        @JvmField
        val MatchLayout: SpinnerStyle = SpinnerStyle(4, front = true, scale = false)

        @JvmField
        val values: Array<SpinnerStyle> = arrayOf<SpinnerStyle>(
            Translate,  //平行移动        特点: HeaderView高度不会改变，
            FixedBehind,  //固定在背后    特点：HeaderView高度不会改变，
            FixedFront,  //固定在前面     特点：HeaderView高度不会改变，
            MatchLayout //填满布局        特点：HeaderView高度不会改变，尺寸充满 RefreshLayout
        )
    }
}
