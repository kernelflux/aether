package com.kernelflux.aether.ui.widget.recyclerNav

import android.view.View


class NavDefaultFocusItemOffsetListener(gravity: Int) : RecyclerNav.NavFocusItemOffsetListener {
    private var mGravity = VERTICAL

    init {
        mGravity = gravity
    }

    override fun getFocusItemOffset(view: View, recyclerNav: RecyclerNav): Int {
        if (mGravity == VERTICAL) {
            return (recyclerNav.width - view.width) / 2
        }
        return if (mGravity == HORIZONTAL) view.left else 0
    }

    companion object {
        val HORIZONTAL = 1
        val VERTICAL: Int = 2
    }
}