package com.kernelflux.aether.ui.widget.recyclerNav


class NavFocusPositionTask(
    private val recyclerNav: RecyclerNav,
    private val navFocusItemOffsetListener: RecyclerNav.NavFocusItemOffsetListener?,
    private val navAnimationProgressListener: RecyclerNav.NavAnimationProgressListener?
) : Runnable {
    override fun run() {
        recyclerNav.setFocusPosition(
            recyclerNav.getSelectedPosition(),
            navFocusItemOffsetListener,
            navAnimationProgressListener
        )
    }
}