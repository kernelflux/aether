package com.kernelflux.aether.ui.widget.recyclerNav

import android.view.View


class DefaultNavViewHolder(itemView: View) : NavViewHolder(itemView) {


    override fun onAnimationUpdate(
        animatedFraction: Float,
        view: View,
        position: Int,
        recyclerNav: RecyclerNav
    ) {
    }

    override fun fillDataToView(
        data: NavItemData?,
        holder: NavViewHolder?,
        selectedPosition: Int,
        recyclerNav: RecyclerNav
    ) {
    }

    override fun onAttachToWindow(
        isSelected: Boolean,
        selectedPosition: Int,
        recyclerNav: RecyclerNav
    ) {
    }


}