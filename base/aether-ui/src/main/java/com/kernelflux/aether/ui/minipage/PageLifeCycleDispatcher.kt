package com.kernelflux.aether.ui.minipage


class PageLifeCycleDispatcher : LifeCycleDispatcher() {

    fun dispatchOnShow(miniPage: MiniPage?) {
        miniPage?.also {
            mMonitors?.forEach {
                if (it is IActivityHostLifeCycleMonitor) {
                    it.show(miniPage)
                }
            }
        }
    }

    fun dispatchOnDismiss(miniPage: MiniPage?) {
        miniPage?.also {
            mMonitors?.forEach {
                if (it is IActivityHostLifeCycleMonitor) {
                    it.dismiss(miniPage)
                }
            }
            mMonitors?.clear()
        }
    }

    override fun clearMonitorsWhenDestroy(): Boolean {
        return false
    }
}