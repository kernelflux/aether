package com.kernelflux.aether.ui.widget.recyclerNav

import com.kernelflux.aether.ui.widget.safewidget.SafeRecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import android.view.animation.AccelerateDecelerateInterpolator
import java.util.ArrayList


open class RecyclerNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SafeRecyclerView(context, attrs, defStyleAttr) {
    private var mContext: Context? = null
    private lateinit var mAdapter: NavAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private var mMoveToFocusRetryTime = -1
    private var mSelectedPosition = 0
    private var mSetFocusRetryTime = 0
    private var mValueAnimator: ValueAnimator? = null


    init {
        init(context)
    }

    interface NavFocusItemOffsetListener {
        fun getFocusItemOffset(view: View, recyclerNav: RecyclerNav): Int
    }

    interface NavItemClickListener {
        fun onNavItemClick(position: Int, data: NavItemData?, view: View?)
    }

    interface NavAnimationProgressListener {
        fun onProgress(selectedPosition: Int, animFraction: Float)
        fun onStart(position: Int)
        fun onStop(position: Int)
        fun onCancel(position: Int)
    }

    private fun init(context: Context) {
        mContext = context
        mAdapter = NavAdapter(this)
        mLayoutManager = LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        itemAnimator = null
        adapter = mAdapter
        layoutManager = mLayoutManager
    }

    private fun moveToFocusImp(
        view: View?,
        navFocusItemOffsetListener: NavFocusItemOffsetListener?,
        isSmoothScroll: Boolean
    ) {
        if (view != null && navFocusItemOffsetListener != null) {
            val left = view.left - navFocusItemOffsetListener.getFocusItemOffset(view, this)
            if (isSmoothScroll) {
                smoothScrollBy(left, 0)
            } else {
                scrollBy(left, 0)
            }
        }
    }

    fun setFocusPositionImp(
        position: Int,
        view: View?,
        navFocusItemOffsetListener: NavFocusItemOffsetListener?,
        navAnimationProgressListener: NavAnimationProgressListener?
    ) {
        mValueAnimator?.cancel()
        stopScroll()
        mValueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
        mValueAnimator?.interpolator = AccelerateDecelerateInterpolator()
        mValueAnimator?.addUpdateListener(
            NavAnimatorUpdateListener(
                this,
                view,
                navFocusItemOffsetListener,
                position,
                navAnimationProgressListener
            )
        )
        mValueAnimator?.addListener(
            NavAnimatorListener(
                this,
                navAnimationProgressListener,
                position
            )
        )
        mValueAnimator?.setDuration(200L)?.start()

    }

    fun addNavItem(position: Int, navItemData: NavItemData) {
        mAdapter.addNavItemData(position, navItemData)
    }

    fun getNavDataList(): ArrayList<NavItemData> {
        return mAdapter.getDataList()
    }

    fun getNavItemData(position: Int): NavItemData? {
        return mAdapter.getItemData(position)
    }

    fun getSelectedPosition(): Int {
        return mSelectedPosition
    }

    fun moveNavItem(fromPosition: Int, toPosition: Int) {
        mAdapter.swapNavItemData(fromPosition, toPosition)
    }

    fun moveToFocus(
        isSmoothScroll: Boolean,
        navFocusItemOffsetListener: NavFocusItemOffsetListener?
    ) {
        val selectedPosition = getSelectedPosition()
        val findFirstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition()
        val findLastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition()
        if (selectedPosition < findFirstVisibleItemPosition || selectedPosition > findLastVisibleItemPosition) {
            mLayoutManager.scrollToPosition(selectedPosition)
            mMoveToFocusRetryTime++
            if (mMoveToFocusRetryTime > 10) {
                mMoveToFocusRetryTime = 0
            } else {
                ViewCompat.postOnAnimation(
                    this,
                    NavMoveToFocusTask(this, isSmoothScroll, navFocusItemOffsetListener)
                )
            }
        } else {
            moveToFocusImp(
                mLayoutManager.findViewByPosition(selectedPosition),
                navFocusItemOffsetListener,
                isSmoothScroll
            )
        }
    }

    override fun onChildAttachedToWindow(child: View) {
        val childAdapterPosition = getChildAdapterPosition(child)
        val childViewHolder = getChildViewHolder(child)
        if (childViewHolder is NavViewHolder) {
            if (childAdapterPosition != getSelectedPosition()) {
                childViewHolder.onAttachToWindow(false, getSelectedPosition(), this)
            } else {
                childViewHolder.onAttachToWindow(true, getSelectedPosition(), this)
            }
        }
    }

    fun removeNavItem(position: Int) {
        mAdapter.removeItem(position)
    }

    fun setFocusPosition(
        position: Int,
        navFocusItemOffsetListener: NavFocusItemOffsetListener?,
        navAnimationProgressListener: NavAnimationProgressListener?
    ) {
        if (position >= 0) {
            setSelectedPosition(position)
            val findFirstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition()
            val findLastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition()
            if (position < findFirstVisibleItemPosition || position > findLastVisibleItemPosition) {
                mLayoutManager.scrollToPosition(getSelectedPosition())
                mSetFocusRetryTime++
                if (mSetFocusRetryTime > 10) {
                    mSetFocusRetryTime = 0
                    if (navAnimationProgressListener != null) {
                        navAnimationProgressListener.onCancel(getSelectedPosition())
                        return
                    }
                    return
                }
                ViewCompat.postOnAnimation(
                    this,
                    NavFocusPositionTask(
                        this,
                        navFocusItemOffsetListener,
                        navAnimationProgressListener
                    )
                )
                return
            }
            setFocusPositionImp(
                position,
                mLayoutManager.findViewByPosition(position),
                navFocusItemOffsetListener,
                navAnimationProgressListener
            )
            mSetFocusRetryTime = 0
        }
    }

    fun setOnNavItemClickListener(navItemClickListener: NavItemClickListener) {
        mAdapter.bindNavItemClickListener(navItemClickListener)
    }

    fun setSelectedPosition(position: Int) {
        mSelectedPosition = position
        mAdapter.setSelectPosition(position)
    }

    fun updateTabs(
        arrayList: ArrayList<NavItemData>,
        factory: NavViewHolderFactory
    ) {
        mAdapter.setData(arrayList)
        mAdapter.setNavViewHolderFact(factory)
    }

    override fun getAdapter(): Adapter<*> {
        return mAdapter
    }

}