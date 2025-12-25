package com.kernelflux.aether.ui.widget.recyclerNav

import android.animation.Animator
import java.lang.ref.WeakReference


class NavAnimatorListener(
    recyclerNav: RecyclerNav,
    navAnimationProgressListener: RecyclerNav.NavAnimationProgressListener?,
    position: Int
) : Animator.AnimatorListener {
    private var mRecyclerNavRef: WeakReference<RecyclerNav>? = null
    private var mAnimProgressListenerRef: WeakReference<RecyclerNav.NavAnimationProgressListener>? = null
    private val position: Int

    init {
        mRecyclerNavRef = WeakReference(recyclerNav)
        mAnimProgressListenerRef = WeakReference(navAnimationProgressListener)
        this.position = position
    }

    fun getRecyclerNavAnimProgressListener(): RecyclerNav.NavAnimationProgressListener? {
        return mAnimProgressListenerRef?.get()
    }

    fun getRecyclerNav(): RecyclerNav? {
        return mRecyclerNavRef?.get()
    }

    override fun onAnimationCancel(animation: Animator) {
        getRecyclerNavAnimProgressListener()?.onCancel(position)
    }

    override fun onAnimationEnd(animation: Animator) {
        getRecyclerNavAnimProgressListener()?.onStop(position)
    }

    override fun onAnimationRepeat(animation: Animator) {

    }

    override fun onAnimationStart(animation: Animator) {
        getRecyclerNavAnimProgressListener()?.onStart(position)
    }
}