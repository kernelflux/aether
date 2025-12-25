package com.kernelflux.aether.ui.minipage


open class LifeCycleDispatcher : ILifeCycleProvider {
    var mMonitors: WeakContainer<LifeCycleMonitor>? = null
    var mResumed: Boolean = false

    open fun clearMonitorsWhenDestroy(): Boolean {
        return false
    }

    fun getCurResumeState(): Boolean {
        return mResumed
    }

    fun dispatchOnCreate(obj: Any?) {
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (weakContainer != null && !weakContainer.isEmpty()) {
            for (next: LifeCycleMonitor? in weakContainer) {
                next?.onCreate(obj)
            }
        }
    }


    fun dispatchOnDestroy() {
        this.mResumed = false
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (weakContainer != null && !weakContainer.isEmpty()) {
            for (next: LifeCycleMonitor? in weakContainer) {
                next?.onDestroy()
            }
            if (clearMonitorsWhenDestroy()) {
                weakContainer.clear()
            }
        }
    }


    fun dispatchOnResume() {
        this.mResumed = true
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (weakContainer != null && !weakContainer.isEmpty()) {
            for (next: LifeCycleMonitor? in weakContainer) {
                next?.onResume()
            }
        }
    }


    fun dispatchOnPause() {
        this.mResumed = false
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (weakContainer != null && !weakContainer.isEmpty()) {
            for (next: LifeCycleMonitor? in weakContainer) {
                next?.onPause()
            }
        }
    }


    fun dispatchOnStart() {
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (weakContainer != null && !weakContainer.isEmpty()) {
            for (next: LifeCycleMonitor? in weakContainer) {
                next?.onStart()
            }
        }
    }

    fun dispatchOnStop() {
        this.mResumed = false
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (weakContainer != null && !weakContainer.isEmpty()) {
            for (next: LifeCycleMonitor? in weakContainer) {
                next?.onStop()
            }
        }
    }

    override fun registerLifeCycleMonitor(lifeCycleMonitor: LifeCycleMonitor?) {
        if (lifeCycleMonitor != null) {
            if (this.mMonitors == null) {
                this.mMonitors = WeakContainer()
            }
            this.mMonitors?.add(lifeCycleMonitor)
        }
    }

    override fun unregisterLifeCycleMonitor(lifeCycleMonitor: LifeCycleMonitor?) {
        val weakContainer: WeakContainer<LifeCycleMonitor>? = this.mMonitors
        if (lifeCycleMonitor != null && weakContainer != null) {
            weakContainer.remove(lifeCycleMonitor)
        }
    }
}