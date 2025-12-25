package com.kernelflux.aether.ui.minipage


interface IActivityHostLifeCycleMonitor : LifeCycleMonitor {

    open class DefaultActivityHostLifeCycleMonitor : LifeCycleMonitor.Stub(),
        IActivityHostLifeCycleMonitor {
        override fun show(page: MiniPage) {

        }

        override fun dismiss(page: MiniPage) {
        }
    }

    fun show(page: MiniPage)

    fun dismiss(page: MiniPage)

}