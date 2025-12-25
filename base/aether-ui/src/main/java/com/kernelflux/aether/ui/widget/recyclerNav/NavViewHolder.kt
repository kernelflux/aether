package com.kernelflux.aether.ui.widget.recyclerNav

import android.view.View
import androidx.recyclerview.widget.RecyclerView


abstract class NavViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    abstract fun onAnimationUpdate(
        animatedFraction: Float,
        view: View,
        position: Int,
        recyclerNav: RecyclerNav
    )

    abstract fun fillDataToView(
        data: NavItemData?,
        holder: NavViewHolder?,
        selectedPosition: Int,
        recyclerNav: RecyclerNav
    )

    abstract fun onAttachToWindow(
        isSelected: Boolean,
        selectedPosition: Int,
        recyclerNav: RecyclerNav
    )

    fun onFocusPositionChange(view: View, hasFocus: Boolean) {

    }

}