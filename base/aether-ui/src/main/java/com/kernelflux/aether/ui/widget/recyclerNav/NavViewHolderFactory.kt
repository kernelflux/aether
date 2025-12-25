package com.kernelflux.aether.ui.widget.recyclerNav

import android.view.ViewGroup

abstract class NavViewHolderFactory {

    abstract fun createViewHolder(viewGroup: ViewGroup, viewType: Int): NavViewHolder

}