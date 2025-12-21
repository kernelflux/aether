package com.kernelflux.aether.imageloader.glide

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

/**
 * Lifecycle safety utilities for preventing crashes when loading images
 * on destroyed or invalid lifecycle states
 * 
 * @author kernelflux
 */
internal object LifecycleSafetyUtils {

    /**
     * Check if Activity is safe for image loading
     */
    fun isActivitySafe(activity: Activity?): Boolean {
        if (activity == null) return false
        
        return try {
            // Check if activity is finishing or destroyed
            !activity.isFinishing && !activity.isDestroyed
        } catch (_: Exception) {
            // If any exception occurs, consider it unsafe
            false
        }
    }

    /**
     * Check if Fragment is safe for image loading
     */
    fun isFragmentSafe(fragment: Fragment?): Boolean {
        if (fragment == null) return false
        
        return try {
            // Check if fragment is added and not detached
            val isAdded = fragment.isAdded && !fragment.isDetached && fragment.view != null
            
            // Also check lifecycle state if available
            val lifecycle = fragment.lifecycle
            val isLifecycleSafe = lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)
            
            isAdded && isLifecycleSafe
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if View is safe for image loading
     */
    fun isViewSafe(view: View?): Boolean {
        if (view == null) return false
        
        return try {
            // Check if view is attached to window
            view.isAttachedToWindow
            
            // Also check if view has a valid context
            val context = view.context
            context != null && context !is Activity || isActivitySafe(context)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Get safe context for image loading
     * Returns Application context if the provided context is unsafe
     */
    fun getSafeContext(context: Context?): Context? {
        if (context == null) return null
        
        return try {
            when {
                // Application context is always safe
                context.applicationContext == context -> context
                
                // If Activity is unsafe, use application context
                context is Activity && !isActivitySafe(context) -> context.applicationContext
                
                // Otherwise use the provided context
                else -> context
            }
        } catch (_: Exception) {
            // Fallback to application context on any error
            context.applicationContext
        }
    }

}
