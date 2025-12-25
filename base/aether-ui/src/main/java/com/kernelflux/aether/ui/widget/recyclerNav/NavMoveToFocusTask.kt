package com.kernelflux.aether.ui.widget.recyclerNav


class NavMoveToFocusTask(
    private val recyclerNav: RecyclerNav,
    private val smoothScroll: Boolean,
    private val navItemFocusItemOffsetListener: RecyclerNav.NavFocusItemOffsetListener?
) : Runnable {

    override fun run() {
        recyclerNav.moveToFocus(
            smoothScroll,
            navItemFocusItemOffsetListener
        )
    }
}