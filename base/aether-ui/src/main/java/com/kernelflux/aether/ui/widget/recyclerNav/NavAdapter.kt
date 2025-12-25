package com.kernelflux.aether.ui.widget.recyclerNav

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList
import kotlin.math.min


class NavAdapter(recyclerNav: RecyclerNav) : RecyclerView.Adapter<NavViewHolder>() {
    private var mRecyclerNav: RecyclerNav? = null
    var navItemDataList = ArrayList<NavItemData>()
    private var viewHolderFactory: NavViewHolderFactory? = null
    var navItemClickListener: RecyclerNav.NavItemClickListener? = null
    private var mSelectedPosition = -1

    init {
        this.mRecyclerNav = recyclerNav
    }

    override fun getItemCount(): Int {
        return navItemDataList.size
    }


    fun getItemData(position: Int): NavItemData? {
        return if (NavUtils.isValidPosition(navItemDataList, position)) {
            navItemDataList[position]
        } else null
    }

    override fun getItemViewType(position: Int): Int {
        return if (NavUtils.isValidPosition(navItemDataList, position)) {
            navItemDataList[position].viewType
        } else super.getItemViewType(position)
    }

    fun getSelectedPosition(): Int {
        return mSelectedPosition
    }

    fun removeItem(position: Int) {
        if (NavUtils.isValidPosition(navItemDataList, position)) {
            navItemDataList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setSelectPosition(position: Int) {
        mSelectedPosition = position
    }

    override fun onBindViewHolder(holder: NavViewHolder, position: Int) {
        if (NavUtils.isValidPosition(navItemDataList, position)) {
            holder.fillDataToView(
                navItemDataList[position],
                holder,
                getSelectedPosition(),
                mRecyclerNav!!
            )
        }
    }

    fun setNavItemData(position: Int, navItemData: NavItemData) {
        if (NavUtils.isValidPosition(navItemDataList, position)) {
            navItemDataList[position] = navItemData
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): NavViewHolder {
        if (viewHolderFactory == null) {
            return DefaultNavViewHolder(View(viewGroup.context))
        }
        val viewHolder: NavViewHolder = viewHolderFactory!!.createViewHolder(viewGroup, viewType)
        viewHolder.itemView.setOnClickListener(NavItemClickListenerWrapper(this, viewHolder))

        return viewHolder
    }

    fun addNavItemData(position: Int, navItemData: NavItemData) {
        if (position <= navItemDataList.size && position >= 0) {
            navItemDataList.add(position, navItemData)
            notifyItemInserted(position)
        }
    }

    fun swapNavItemData(fromPosition: Int, toPosition: Int) {
        if (fromPosition != toPosition &&
            NavUtils.isValidPosition(navItemDataList, fromPosition) &&
            NavUtils.isValidPosition(navItemDataList, toPosition)
        ) {
            navItemDataList.add(
                min(toPosition, navItemDataList.size),
                navItemDataList.removeAt(fromPosition)
            )
            notifyItemMoved(fromPosition, toPosition)
        }
    }


    fun setNavViewHolderFact(factory: NavViewHolderFactory) {
        this.viewHolderFactory = factory
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(arrayList: ArrayList<NavItemData>) {
        navItemDataList.clear()
        navItemDataList.addAll(arrayList)
        notifyDataSetChanged()
    }


    fun getDataList(): ArrayList<NavItemData> {
        return navItemDataList
    }

    fun bindNavItemClickListener(listener: RecyclerNav.NavItemClickListener) {
        navItemClickListener = listener
    }
}