package com.kernelflux.aether.ui.widget.refresh.constant

/**
 * 刷新状态
 */
@Suppress("unused")
enum class RefreshState(
    role: Int, // 正在拖动状态：PullDownToRefresh PullUpToLoad ReleaseToRefresh ReleaseToLoad ReleaseToTwoLevel
    @JvmField val isDragging: Boolean, // 正在刷新状态：Refreshing Loading TwoLevel
    @JvmField val isOpening: Boolean, //正在完成状态：RefreshFinish LoadFinish TwoLevelFinish
    @JvmField val isFinishing: Boolean, // 二级刷新 ReleaseToTwoLevel TwoLevelReleased TwoLevel
    val isTwoLevel: Boolean, // 释放立马打开 ReleaseToRefresh ReleaseToLoad ReleaseToTwoLevel
    @JvmField var isReleaseToOpening: Boolean
) {
    None(0, false, false, false, false, false),
    PullDownToRefresh(1, true, false, false, false, false), PullUpToLoad(
        2,
        true,
        false,
        false,
        false,
        false
    ),
    PullDownCanceled(1, false, false, false, false, false), PullUpCanceled(
        2,
        false,
        false,
        false,
        false,
        false
    ),
    ReleaseToRefresh(1, true, false, false, false, true), ReleaseToLoad(
        2,
        true,
        false,
        false,
        false,
        true
    ),
    ReleaseToTwoLevel(1, true, false, false, true, true), TwoLevelReleased(
        1,
        false,
        false,
        false,
        true,
        false
    ),
    RefreshReleased(1, false, false, false, false, false), LoadReleased(
        2,
        false,
        false,
        false,
        false,
        false
    ),
    Refreshing(1, false, true, false, false, false), Loading(
        2,
        false,
        true,
        false,
        false,
        false
    ),
    TwoLevel(1, false, true, false, true, false),
    RefreshFinish(1, false, false, true, false, false), LoadFinish(
        2,
        false,
        false,
        true,
        false,
        false
    ),
    TwoLevelFinish(1, false, false, true, true, false);

    @JvmField
    val isHeader: Boolean = role == 1

    @JvmField
    val isFooter: Boolean = role == 2

    init {
        this.isReleaseToOpening = isReleaseToOpening
    }

    fun toFooter(): RefreshState {
        if (isHeader && !isTwoLevel) {
            return RefreshState.entries[ordinal + 1]
        }
        return this
    }

    fun toHeader(): RefreshState {
        if (isFooter && !isTwoLevel) {
            return RefreshState.entries[ordinal - 1]
        }
        return this
    }
}