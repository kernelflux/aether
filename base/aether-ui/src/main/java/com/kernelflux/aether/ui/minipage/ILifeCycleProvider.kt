package com.kernelflux.aether.ui.minipage


interface ILifeCycleProvider {

    fun registerLifeCycleMonitor(lifeCycleMonitor: LifeCycleMonitor?)

    fun unregisterLifeCycleMonitor(lifeCycleMonitor: LifeCycleMonitor?)

}