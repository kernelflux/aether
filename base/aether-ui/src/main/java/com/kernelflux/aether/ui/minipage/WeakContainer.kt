package com.kernelflux.aether.ui.minipage

import java.util.*
import kotlin.collections.ArrayList


class WeakContainer<T> : Iterable<T> {
    private val mMap: WeakHashMap<T, Any> = WeakHashMap()
    private val mValue: Any = Any()


    fun add(t: T?) {
        t?.also { this.mMap[it] = this.mValue }
    }

    fun clear() {
        this.mMap.clear()
    }

    fun contains(t: T?): Boolean {
        return this.mMap.containsKey(t)
    }

    fun isEmpty(): Boolean {
        return this.mMap.isEmpty()
    }

    override
    fun iterator(): Iterator<T> {
        val arrayList = ArrayList<T>(this.mMap.size)
        for (next in this.mMap.keys) {
            next?.also { arrayList.add(it) }
        }
        return arrayList.iterator()
    }

    fun peek(): T? {
        var t: T? = null
        if (this.mMap.size == 0) {
            return null
        }
        val iterator: MutableIterator<T> = this.mMap.keys.iterator()
        while (true) {
            if (iterator.hasNext()) {
                val next = iterator.next()
                if (next != null) {
                    t = next;
                    break
                }
            } else {
                break
            }
        }
        this.mMap.remove(t);
        return t;
    }

    fun remove(t: T?) {
        if (t == null) {
            this.mMap.size
        } else {
            this.mMap.remove(t)
        }
    }

    fun size(): Int {
        return this.mMap.size
    }
}