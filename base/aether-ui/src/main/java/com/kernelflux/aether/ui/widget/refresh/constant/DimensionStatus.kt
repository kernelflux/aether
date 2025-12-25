package com.kernelflux.aether.ui.widget.refresh.constant

/**
 * 尺寸值的定义状态，用于在值覆盖的时候决定优先级
 * 越往下优先级越高
 */
class DimensionStatus private constructor(@JvmField val ordinal: Int, @JvmField val notified: Boolean) {
    /**
     * 转换为未通知状态
     * @return 未通知状态
     */
    fun unNotify(): DimensionStatus {
        if (notified) {
            val prev: DimensionStatus = values[ordinal - 1]
            if (!prev.notified) {
                return prev
            }
            return DefaultUnNotify
        }
        return this
    }

    /**
     * 转换为通知状态
     * @return 通知状态
     */
    fun notified(): DimensionStatus {
        if (!notified) {
            return values[ordinal + 1]
        }
        return this
    }

    /**
     * 是否可以被新的状态替换
     * @param status 新转台
     * @return 小于等于
     */
    fun canReplaceWith(status: DimensionStatus): Boolean {
        return ordinal < status.ordinal || ((!notified || CodeExact === this) && ordinal == status.ordinal)
    }

    companion object {
        @JvmField
        val DefaultUnNotify: DimensionStatus = DimensionStatus(0, false) //默认值，但是还没通知确认
        val Default: DimensionStatus = DimensionStatus(1, true) //默认值
        @JvmField
        val XmlWrapUnNotify: DimensionStatus = DimensionStatus(2, false) //Xml计算，但是还没通知确认
        val XmlWrap: DimensionStatus = DimensionStatus(3, true) //Xml计算
        @JvmField
        val XmlExactUnNotify: DimensionStatus = DimensionStatus(4, false) //Xml 的view 指定，但是还没通知确认
        val XmlExact: DimensionStatus = DimensionStatus(5, true) //Xml 的view 指定
        @JvmField
        val XmlLayoutUnNotify: DimensionStatus =
            DimensionStatus(6, false) //Xml 的layout 中指定，但是还没通知确认
        val XmlLayout: DimensionStatus = DimensionStatus(7, true) //Xml 的layout 中指定
        @JvmField
        val CodeExactUnNotify: DimensionStatus = DimensionStatus(8, false) //代码指定，但是还没通知确认
        @JvmField
        val CodeExact: DimensionStatus = DimensionStatus(9, true) //代码指定
        val DeadLockUnNotify: DimensionStatus = DimensionStatus(10, false) //锁死，但是还没通知确认
        val DeadLock: DimensionStatus = DimensionStatus(10, true) //锁死

        val values: Array<DimensionStatus> = arrayOf<DimensionStatus>(
            DefaultUnNotify,
            Default,
            XmlWrapUnNotify,
            XmlWrap,
            XmlExactUnNotify,
            XmlExact,
            XmlLayoutUnNotify,
            XmlLayout,
            CodeExactUnNotify,
            CodeExact,
            DeadLockUnNotify,
            DeadLock
        )
    }
}