package com.kernelflux.aether.ui.widget.refresh.util

import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.kernelflux.aether.ui.widget.refresh.api.RefreshKernel
import com.kernelflux.aether.ui.widget.refresh.listener.CoordinatorLayoutListener
import androidx.core.view.size

/**
 * Design 兼容包缺省尝试
 * Created by scwang on 2018/1/29.
 */
object DesignUtil {
    @JvmStatic
    fun checkCoordinatorLayout(
        content: View?,
        kernel: RefreshKernel,
        listener: CoordinatorLayoutListener
    ) {
        try { //try 不能删除，不然会出现兼容性问题
            if (content is CoordinatorLayout) {
                kernel.getRefreshLayout().setEnableNestedScroll(false)
                val layout = content as ViewGroup
                for (i in layout.size - 1 downTo 0) {
                    val view = layout.getChildAt(i)
                    if (view is AppBarLayout) {
                        view.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
                            listener.onCoordinatorUpdate(
                                verticalOffset >= 0,
                                (appBarLayout!!.getTotalScrollRange() + verticalOffset) <= 0
                            )
                        })
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
