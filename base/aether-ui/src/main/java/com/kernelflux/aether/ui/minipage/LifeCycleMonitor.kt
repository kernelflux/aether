package com.kernelflux.aether.ui.minipage


interface LifeCycleMonitor {
    open class Stub : LifeCycleMonitor {
        override fun onCreate(obj: Any?) {
        }

        override fun onStart() {
        }

        override fun onResume() {
        }

        override fun onPause() {
        }

        override fun onStop() {
        }

        override fun onDestroy() {
        }
    }

    fun onCreate(obj: Any?)

    fun onStart()

    fun onResume()

    fun onPause()

    fun onStop()

    fun onDestroy()
}