package com.kernelflux.aether.ui.widget.recyclerNav

import android.view.View


class NavItemClickListenerWrapper(
    private val adapter: NavAdapter,
    private val viewHolder: NavViewHolder
) : View.OnClickListener {

    override fun onClick(v: View?) {
        if (adapter.navItemClickListener != null) {
            val adapterPosition = viewHolder.adapterPosition
            var navItemData: NavItemData? = null
            if (NavUtils.isValidPosition(adapter.navItemDataList, adapterPosition)) {
                navItemData = adapter.navItemDataList.get(adapterPosition)
            }
            adapter.navItemClickListener?.onNavItemClick(adapterPosition, navItemData, v)
        }
    }
}