package com.kernelflux.aether.ui.widget.recyclerNav


object NavUtils {

    @JvmStatic
    fun isValidPosition(collections: Collection<*>?, position: Int): Boolean {
        return collections != null && position >= 0 && position < collections.size
    }
}