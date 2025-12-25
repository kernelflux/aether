package com.kernelflux.aether.ui.minipage

import android.view.View


const val DIRECTION_LEFT = 1
const val DIRECTION_TOP = 2
const val DIRECTION_RIGHT = 3
const val DIRECTION_BOTTOM = 4


fun getLeft(extView: View?, intView: View?): Int {
    return getPositionInternal(DIRECTION_LEFT, extView, intView)
}

fun getTop(extView: View?, intView: View?): Int {
    return getPositionInternal(DIRECTION_TOP, extView, intView)
}


fun getRight(extView: View?, intView: View?): Int {
    return getPositionInternal(DIRECTION_RIGHT, extView, intView)
}

fun getBottom(extView: View?, intView: View?): Int {
    return getPositionInternal(DIRECTION_BOTTOM, extView, intView)
}

private fun getPositionInternal(
    direction: Int,
    extView: View?,
    intView: View?
): Int {
    if (extView == null || intView == null) {
        return 0
    }
    var positionInternal = 0

    if (direction == DIRECTION_RIGHT || direction == DIRECTION_LEFT) {
        val w = if (direction == DIRECTION_RIGHT) intView.width else 0
        val intViewScrollX = intView.scrollX
        val left = intView.left
        positionInternal = left + (w - intViewScrollX)
    } else if (direction == DIRECTION_BOTTOM || direction == DIRECTION_TOP) {
        val h = if (direction == DIRECTION_BOTTOM) intView.height else 0
        val intViewScrollY = intView.scrollY
        val intViewTop = intView.top
        positionInternal = intViewTop + (h - intViewScrollY)
    }

    val intViewParent = intView.parent
    if (intViewParent !is View) {
        return positionInternal
    }
    val intViewParentView = intViewParent
    if (intViewParentView !== extView) {
        getPositionInternal(
            direction,
            extView,
            intViewParentView
        )
    }
    return positionInternal
}