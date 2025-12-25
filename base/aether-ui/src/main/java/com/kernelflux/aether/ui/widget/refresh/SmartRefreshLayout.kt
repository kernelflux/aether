package com.kernelflux.aether.ui.widget.refresh

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.Scroller
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.NestedScrollingParent
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import com.kernelflux.aether.ui.widget.refresh.api.RefreshComponent
import com.kernelflux.aether.ui.widget.refresh.api.RefreshContent
import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader
import com.kernelflux.aether.ui.widget.refresh.api.RefreshKernel
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.constant.DimensionStatus
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState
import com.kernelflux.aether.ui.widget.refresh.constant.SpinnerStyle
import com.kernelflux.aether.ui.widget.refresh.listener.DefaultRefreshFooterCreator
import com.kernelflux.aether.ui.widget.refresh.listener.DefaultRefreshHeaderCreator
import com.kernelflux.aether.ui.widget.refresh.listener.DefaultRefreshInitializer
import com.kernelflux.aether.ui.widget.refresh.listener.OnLoadMoreListener
import com.kernelflux.aether.ui.widget.refresh.listener.OnMultiListener
import com.kernelflux.aether.ui.widget.refresh.listener.OnRefreshListener
import com.kernelflux.aether.ui.widget.refresh.listener.OnRefreshLoadMoreListener
import com.kernelflux.aether.ui.widget.refresh.listener.OnStateChangedListener
import com.kernelflux.aether.ui.widget.refresh.listener.ScrollBoundaryDecider
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.dp2px
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.fling
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.isContentView
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil.Companion.px2dp
import com.kernelflux.aether.ui.widget.refresh.wrapper.RefreshContentWrapper
import com.kernelflux.aether.ui.widget.refresh.wrapper.RefreshFooterWrapper
import com.kernelflux.aether.ui.widget.refresh.wrapper.RefreshHeaderWrapper
import kotlin.Any
import kotlin.Char
import kotlin.Float
import kotlin.Int
import kotlin.IntArray
import kotlin.Long
import kotlin.RuntimeException
import kotlin.Suppress
import kotlin.Throwable
import kotlin.also
import kotlin.intArrayOf
import kotlin.let
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import androidx.core.view.size
import androidx.core.view.isGone
import androidx.core.content.withStyledAttributes
import com.kernelflux.aether.ui.BuildConfig
import com.kernelflux.aether.ui.R

/**
 * 智能刷新布局
 * Intelligent RefreshLayout
 * Created by scwang on 2017/5/26.
 */
@Suppress("unused")
@SuppressLint("RestrictedApi")
open class SmartRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) :
    ViewGroup(context, attrs), RefreshLayout, NestedScrollingParent {
    protected var mTouchSlop: Int
    protected var mSpinner: Int = 0 //当前的 Spinner 大于0表示下拉,小于零表示上拉
    protected var mLastSpinner: Int = 0 //最后的，的Spinner
    protected var mTouchSpinner: Int = 0 //触摸时候，的Spinner
    protected var mFloorDuration: Int = 300 //二楼展开时长
    protected var mReboundDuration: Int = 300 //回弹动画时长
    protected var mScreenHeightPixels: Int //屏幕高度
    protected var mTouchX: Float = 0f
    protected var mTouchY: Float = 0f
    protected var mLastTouchX: Float = 0f //用于实现Header的左右拖动效果
    protected var mLastTouchY: Float = 0f //用于实现多点触摸
    protected var mDragRate: Float = .5f
    protected var mDragDirection: Char = 'n' //拖动的方向 none-n horizontal-h vertical-v
    protected var mIsBeingDragged: Boolean = false //是否正在拖动
    protected var mSuperDispatchTouchEvent: Boolean = false //父类是否处理触摸事件
    protected var mEnableDisallowIntercept: Boolean = false //是否允许拦截事件
    protected var mFixedHeaderViewId: Int = NO_ID //固定在头部的视图Id
    protected var mFixedFooterViewId: Int = NO_ID //固定在底部的视图Id
    protected var mHeaderTranslationViewId: Int = NO_ID //下拉Header偏移的视图Id
    protected var mFooterTranslationViewId: Int = NO_ID //下拉Footer偏移的视图Id

    protected var mMinimumVelocity: Int
    protected var mMaximumVelocity: Int
    protected var mCurrentVelocity: Int = 0
    protected var mScroller: Scroller
    protected var mVelocityTracker: VelocityTracker
    protected var mReboundInterpolator: Interpolator?

    //</editor-fold>
    //<editor-fold desc="功能属性">
    protected var mPrimaryColors: IntArray? = null
    protected var mEnableRefresh: Boolean = true
    protected var mEnableLoadMore: Boolean = false
    protected var mEnableClipHeaderWhenFixedBehind: Boolean =
        true //当 Header FixedBehind 时候是否剪裁遮挡 Header
    protected var mEnableClipFooterWhenFixedBehind: Boolean =
        true //当 Footer FixedBehind 时候是否剪裁遮挡 Footer
    protected var mEnableHeaderTranslationContent: Boolean = true //是否启用内容视图拖动效果
    protected var mEnableFooterTranslationContent: Boolean = true //是否启用内容视图拖动效果
    protected var mEnableFooterFollowWhenNoMoreData: Boolean = false //是否在全部加载结束之后Footer跟随内容 1.0.4-6
    protected var mEnablePreviewInEditMode: Boolean = true //是否在编辑模式下开启预览功能
    protected var mEnableOverScrollBounce: Boolean = true //是否启用越界回弹
    protected var mEnableOverScrollDrag: Boolean = false //是否启用越界拖动（仿苹果效果）1.0.4-6
    protected var mEnableAutoLoadMore: Boolean = true //是否在列表滚动到底部时自动加载更多
    protected var mEnablePureScrollMode: Boolean = false //是否开启纯滚动模式
    protected var mEnableScrollContentWhenLoaded: Boolean = true //是否在加载更多完成之后滚动内容显示新数据
    protected var mEnableScrollContentWhenRefreshed: Boolean = true //是否在刷新完成之后滚动内容显示新数据
    protected var mEnableLoadMoreWhenContentNotFull: Boolean = true //在内容不满一页的时候，是否可以上拉加载更多
    protected var mEnableNestedScrolling: Boolean = true //是否启用潜逃滚动功能
    protected var mDisableContentWhenRefresh: Boolean = false //是否开启在刷新时候禁止操作内容视图
    protected var mDisableContentWhenLoading: Boolean = false //是否开启在刷新时候禁止操作内容视图
    protected var mFooterNoMoreData: Boolean = false //数据是否全部加载完成，如果完成就不能在触发加载事件
    protected var mFooterNoMoreDataEffective: Boolean = false //是否 NoMoreData 生效(有的 Footer 可能不支持)

    protected var mManualLoadMore: Boolean = false //是否手动设置过LoadMore，用于智能开启
    protected var mManualHeaderTranslationContent: Boolean = false //是否手动设置过内容视图拖动效果
    protected var mManualFooterTranslationContent: Boolean = false //是否手动设置过内容视图拖动效果

    //</editor-fold>
    //<editor-fold desc="监听属性">
    protected var mRefreshListener: OnRefreshListener? = null
    protected var mLoadMoreListener: OnLoadMoreListener? = null
    protected var mOnMultiListener: OnMultiListener? = null
    protected var mScrollBoundaryDecider: ScrollBoundaryDecider? = null

    //</editor-fold>
    //<editor-fold desc="嵌套滚动">
    protected var mTotalUnconsumed: Int = 0
    protected var mNestedInProgress: Boolean = false
    protected var mParentOffsetInWindow: IntArray = IntArray(2)
    protected var mNestedChild: NestedScrollingChildHelper = NestedScrollingChildHelper(this)
    protected var mNestedParent: NestedScrollingParentHelper = NestedScrollingParentHelper(this)


    protected var mHeaderHeight: Int //头部高度 和 头部高度状态
    protected var mHeaderHeightStatus: DimensionStatus = DimensionStatus.DefaultUnNotify
    protected var mFooterHeight: Int //底部高度 和 底部高度状态
    protected var mFooterHeightStatus: DimensionStatus = DimensionStatus.DefaultUnNotify

    protected var mHeaderInsetStart: Int = 0 // Header 起始位置偏移
    protected var mFooterInsetStart: Int = 0 // Footer 起始位置偏移

    protected var mHeaderMaxDragRate: Float = 2.5f //最大拖动比率(最大高度/Header高度)
    protected var mFooterMaxDragRate: Float = 2.5f //最大拖动比率(最大高度/Footer高度)
    protected var mHeaderTriggerRate: Float = 1.0f //触发刷新距离 与 HeaderHeight 的比率
    protected var mFooterTriggerRate: Float = 1.0f //触发加载距离 与 FooterHeight 的比率

    protected var mTwoLevelBottomPullUpToCloseRate: Float = 1 / 6f //二级刷新打开时，再底部上划关闭区域所占的比率

    protected var mRefreshHeader: RefreshComponent? = null //下拉头部视图
    protected var mRefreshFooter: RefreshComponent? = null //上拉底部视图

    /**
     * 显示内容视图
     * 根据开发设计，本变量最终不可能为空，因为在 onAttachedToWindow 中判断如果未设置内容视图
     * 会自动创建一个 TextView 提示必须创建，并且 TextView 会变为 内容视图
     */
    protected var mRefreshContent: RefreshContent? = null

    //</editor-fold>
    protected var mPaint: Paint? = null
    protected var mHandler: Handler?
    protected var mKernel: RefreshKernel = RefreshKernelImpl()

    /**
     * 【主要状态】
     * 面对 SmartRefresh 外部的滚动状态
     */
    protected var mState: RefreshState = RefreshState.None //主状态

    /**
     * 【附加状态】
     * 用于主状态 mState 为 Refreshing 或 Loading 时的滚动状态
     * 1.mState=Refreshing|Loading 时 mViceState 有可能与 mState 不同
     * 2.mState=None,开启越界拖动 时 mViceState 有可能与 mState 不同
     * 3.其他状态时与主状态相等 mViceState=mState
     * 4.SmartRefresh 外部无法察觉 mViceState
     */
    protected var mViceState: RefreshState = RefreshState.None //副状态（主状态刷新时候的滚动状态）

    protected var mLastOpenTime: Long = 0 //上一次 刷新或者加载 时间

    protected var mHeaderBackgroundColor: Int = 0 //为Header绘制纯色背景
    protected var mFooterBackgroundColor: Int = 0

    protected var mHeaderNeedTouchEventWhenRefreshing: Boolean = false //为游戏Header提供独立事件
    protected var mFooterNeedTouchEventWhenLoading: Boolean = false

    protected var mAttachedToWindow: Boolean = false //是否添加到Window

    protected var mFooterLocked: Boolean = false //Footer 正在loading 的时候是否锁住 列表不能向上滚动

    /*
     * https://github.com/scwang90/SmartRefreshLayout/issues/1540
     * 问题修复辅助变量，记录关闭动画执行时手势事件 ACTION_DOWN 的触发时间 和 按下的坐标
     */
    protected var mLastTimeOnActionDown: Long = 0
    protected var mLastTouchXOnActionDown: Float = 0f
    protected var mLastTouchYOnActionDown: Float = 0f


    /**
     * 重写 onFinishInflate 来完成 smart 的特定功能
     * 1.智能寻找 Xml 中定义的 Content、Header、Footer
     */
    @SuppressLint("UseKtx")
    override fun onFinishInflate() {
        super.onFinishInflate()
        val count = super.getChildCount()
        if (count > 3) {
            throw RuntimeException("最多只支持3个子View，Most only support three sub view")
        }

        var contentLevel = 0 // 当前找到内容布局的级别（0还没找到 1普通内容 2可滚动内容）
        var indexContent = -1 // 内容布局所在的序号
        for (i in 0..<count) {
            val view = super.getChildAt(i)
            if (isContentView(view) && (contentLevel < 2 || i == 1)) {
                // 可滚动内容
                indexContent = i
                // 标记为可滚动内容，不会再被替换
                contentLevel = 2
            } else if (view !is RefreshComponent && contentLevel < 1) {
                // 普通内容
                indexContent = i
                // 如果是第一个标记为：未找到（第一个有可能是自定义Header），否则标记为：普通内容
                contentLevel = if (i > 0) 1 else 0
            }
        }

        var indexHeader = -1
        var indexFooter = -1
        if (indexContent >= 0) {
            mRefreshContent = RefreshContentWrapper(super.getChildAt(indexContent))
            if (indexContent == 1) {
                indexHeader = 0
                if (count == 3) {
                    indexFooter = 2
                }
            } else if (count == 2) {
                indexFooter = 1
            }
        }

        for (i in 0..<count) {
            val view = super.getChildAt(i)
            if (i == indexHeader || (i != indexFooter && indexHeader == -1 && mRefreshHeader == null && view is RefreshHeader)) {
                mRefreshHeader =
                    if (view is RefreshHeader) view as RefreshHeader else RefreshHeaderWrapper(view)
            } else if (i == indexFooter || (indexFooter == -1 && view is RefreshFooter)) {
                mEnableLoadMore = (mEnableLoadMore || !mManualLoadMore)
                mRefreshFooter =
                    if (view is RefreshFooter) view as RefreshFooter else RefreshFooterWrapper(view)
            }
        }
    }

    /**
     * 重写 onAttachedToWindow 来完成 smart 的特定功能 （在 onFinishInflate 之后执行）
     * 1.添加默认或者全局设置的 Header 和 Footer （缺省情况下才会）
     * 2.做 Content 为空时的 TextView 提示
     * 3.智能开启 嵌套滚动 NestedScrollingEnabled
     * 4.初始化 主题颜色 和 调整 Header Footer Content 的显示顺序
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mAttachedToWindow = true

        val thisView: View = this
        // 在非编辑模式下，如果 onFinishInflate 初始化组件未成功，onAttachedToWindow 可以进行补充处理
        if (!thisView.isInEditMode) {
            if (mRefreshHeader == null) {
                if (sHeaderCreator != null) {
                    val header: RefreshHeader? =
                        sHeaderCreator?.createRefreshHeader(thisView.context, this)
                    if (header == null) {
                        if (BuildConfig.DEBUG) {
                            throw RuntimeException("DefaultRefreshHeaderCreator can not return null")
                        }
                    }
                    header?.let { setRefreshHeader(it) }
                }
            }
            if (mRefreshFooter == null) {
                if (sFooterCreator != null) {
                    val footer: RefreshFooter? =
                        sFooterCreator?.createRefreshFooter(thisView.context, this)
                    if (footer == null) {
                        if (BuildConfig.DEBUG) {
                            throw RuntimeException("DefaultRefreshFooterCreator can not return null")
                        }
                    }
                    footer?.let { setRefreshFooter(it) }
                }
            } else {
                mEnableLoadMore = mEnableLoadMore || !mManualLoadMore
            }

            if (mRefreshContent == null) {
                var i = 0
                val len = size
                while (i < len) {
                    val view = getChildAt(i)
                    if ((mRefreshHeader == null || view !== mRefreshHeader?.getView()) &&
                        (mRefreshFooter == null || view !== mRefreshFooter?.getView())
                    ) {
                        mRefreshContent = RefreshContentWrapper(view)
                    }
                    i++
                }
            }
            if (mRefreshContent == null) {
                val padding = dp2px(20f)
                val errorView = TextView(thisView.context)
                errorView.setTextColor(-0x9a00)
                errorView.setGravity(Gravity.CENTER)
                errorView.textSize = 20f
                errorView.setText(R.string.srl_content_empty)
                super.addView(
                    errorView,
                    0,
                    LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                mRefreshContent = RefreshContentWrapper(errorView)
                mRefreshContent?.getView()?.setPadding(padding, padding, padding, padding)
            }

            val fixedHeaderView = thisView.findViewById<View?>(mFixedHeaderViewId)
            val fixedFooterView = thisView.findViewById<View?>(mFixedFooterViewId)

            mRefreshContent?.apply {
                mScrollBoundaryDecider?.let {
                    setScrollBoundaryDecider(it)
                }
                setEnableLoadMoreWhenContentNotFull(mEnableLoadMoreWhenContentNotFull)
                setUpComponent(mKernel, fixedHeaderView, fixedFooterView)
            }

            if (mSpinner != 0) {
                notifyStateChanged(RefreshState.None)
                mRefreshContent?.moveSpinner(
                    0,
                    mHeaderTranslationViewId,
                    mFooterTranslationViewId
                )
            }
        }

        mPrimaryColors?.also {
            mRefreshHeader?.setPrimaryColors(*it)
            mRefreshFooter?.setPrimaryColors(*it)
        }

        //重新排序
        mRefreshContent?.also {
            super.bringChildToFront(it.getView())
        }
        mRefreshHeader?.also {
            if (it.getSpinnerStyle().front) {
                super.bringChildToFront(it.getView())
            }
        }

        mRefreshFooter?.also {
            if (it.getSpinnerStyle().front) {
                super.bringChildToFront(it.getView())
            }
        }
    }

    /**
     * 测量 Header Footer Content
     * 1.测量代码看起来很复杂，时因为 Header Footer 有四种拉伸变换样式 [SpinnerStyle]，每一种样式有自己的测量方法
     * 2.提供预览测量，可以在编辑 XML 的时候直接预览 （isInEditMode）
     * 3.恢复水平触摸位置缓存 mLastTouchX 到屏幕中央
     * @param widthMeasureSpec 水平测量参数
     * @param heightMeasureSpec 竖直测量参数
     */
    @SuppressLint("UseKtx")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var minimumWidth = 0
        var minimumHeight = 0
        val thisView: View = this
        val needPreview = thisView.isInEditMode && mEnablePreviewInEditMode

        var i = 0
        val len = super.getChildCount()
        while (i < len) {
            val child = super.getChildAt(i)

            if (child.isGone || "GONE" == child.getTag(R.id.srl_tag)) {
                i++
                continue
            }

            mRefreshHeader?.also {
                if (it.getView() === child) {
                    val headerView: View = it.getView()
                    val lp = headerView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val widthSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        mlp.leftMargin + mlp.rightMargin,
                        lp.width
                    )
                    var height = mHeaderHeight

                    if (mHeaderHeightStatus.ordinal < DimensionStatus.XmlLayoutUnNotify.ordinal) {
                        if (lp.height > 0) {
                            height = lp.height + mlp.bottomMargin + mlp.topMargin
                            if (mHeaderHeightStatus.canReplaceWith(DimensionStatus.XmlExactUnNotify)) {
                                mHeaderHeight = lp.height + mlp.bottomMargin + mlp.topMargin
                                mHeaderHeightStatus = DimensionStatus.XmlExactUnNotify
                            }
                        } else if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                            (mRefreshHeader?.getSpinnerStyle() !== SpinnerStyle.MatchLayout || !mHeaderHeightStatus.notified)
                        ) {
                            val maxHeight = max(
                                MeasureSpec.getSize(heightMeasureSpec) - mlp.bottomMargin - mlp.topMargin,
                                0
                            )
                            headerView.measure(
                                widthSpec,
                                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                            )
                            val measuredHeight = headerView.measuredHeight
                            if (measuredHeight > 0) {
                                height = -1
                                if (measuredHeight != (maxHeight) && mHeaderHeightStatus.canReplaceWith(
                                        DimensionStatus.XmlWrapUnNotify
                                    )
                                ) {
                                    mHeaderHeight =
                                        measuredHeight + mlp.bottomMargin + mlp.topMargin
                                    mHeaderHeightStatus = DimensionStatus.XmlWrapUnNotify
                                }
                            }
                        }
                    }

                    if (mRefreshHeader?.getSpinnerStyle() === SpinnerStyle.MatchLayout) {
                        height = MeasureSpec.getSize(heightMeasureSpec)
                    } else if (mRefreshHeader?.getSpinnerStyle()?.scale == true && !needPreview) {
                        height =
                            max(0, if (isEnableRefreshOrLoadMore(mEnableRefresh)) mSpinner else 0)
                    }

                    if (height != -1) {
                        headerView.measure(
                            widthSpec,
                            MeasureSpec.makeMeasureSpec(
                                max(
                                    height - mlp.bottomMargin - mlp.topMargin,
                                    0
                                ), MeasureSpec.EXACTLY
                            )
                        )
                    }

                    if (!mHeaderHeightStatus.notified) {
                        val maxDragHeight =
                            if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate
                        mHeaderHeightStatus = mHeaderHeightStatus.notified()
                        mRefreshHeader?.onInitialized(
                            mKernel,
                            mHeaderHeight,
                            maxDragHeight.toInt()
                        )
                    }

                    if (needPreview && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        minimumWidth += headerView.measuredWidth
                        minimumHeight += headerView.measuredHeight
                    }
                }
            }

            mRefreshFooter?.also {
                if (it.getView() === child) {
                    val footerView: View = it.getView()
                    val lp = footerView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val widthSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        mlp.leftMargin + mlp.rightMargin,
                        lp.width
                    )
                    var height = mFooterHeight

                    if (mFooterHeightStatus.ordinal < DimensionStatus.XmlLayoutUnNotify.ordinal) {
                        if (lp.height > 0) {
                            height = lp.height + mlp.topMargin + mlp.bottomMargin
                            if (mFooterHeightStatus.canReplaceWith(DimensionStatus.XmlExactUnNotify)) {
                                mFooterHeight = lp.height + mlp.topMargin + mlp.bottomMargin
                                mFooterHeightStatus = DimensionStatus.XmlExactUnNotify
                            }
                        } else if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT && (it.getSpinnerStyle() !== SpinnerStyle.MatchLayout || !mFooterHeightStatus.notified)) {
                            val maxHeight = max(
                                MeasureSpec.getSize(heightMeasureSpec) - mlp.bottomMargin - mlp.topMargin,
                                0
                            )
                            footerView.measure(
                                widthSpec,
                                MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                            )
                            val measuredHeight = footerView.measuredHeight
                            if (measuredHeight > 0) {
                                height = -1
                                if (measuredHeight != (maxHeight) && mFooterHeightStatus.canReplaceWith(
                                        DimensionStatus.XmlWrapUnNotify
                                    )
                                ) {
                                    mFooterHeight =
                                        measuredHeight + mlp.topMargin + mlp.bottomMargin
                                    mFooterHeightStatus = DimensionStatus.XmlWrapUnNotify
                                }
                            }
                        }
                    }

                    if (it.getSpinnerStyle() === SpinnerStyle.MatchLayout) {
                        height = MeasureSpec.getSize(heightMeasureSpec)
                    } else if (it.getSpinnerStyle().scale && !needPreview) {
                        height =
                            max(0, if (isEnableRefreshOrLoadMore(mEnableLoadMore)) -mSpinner else 0)
                    }

                    if (height != -1) {
                        footerView.measure(
                            widthSpec,
                            MeasureSpec.makeMeasureSpec(
                                max(
                                    height - mlp.bottomMargin - mlp.topMargin,
                                    0
                                ), MeasureSpec.EXACTLY
                            )
                        )
                    }

                    if (!mFooterHeightStatus.notified) {
                        val maxDragHeight =
                            if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
                        mFooterHeightStatus = mFooterHeightStatus.notified()
                        it.onInitialized(mKernel, mFooterHeight, maxDragHeight.toInt())
                    }

                    if (needPreview && isEnableRefreshOrLoadMore(mEnableLoadMore)) {
                        minimumWidth += footerView.measuredWidth
                        minimumHeight += footerView.measuredHeight
                    }
                }
            }


            mRefreshContent?.also {
                if (it.getView() === child) {
                    val contentView: View = it.getView()
                    val lp = contentView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val showHeader =
                        (mRefreshHeader != null && isEnableRefreshOrLoadMore(mEnableRefresh) && isEnableTranslationContent(
                            mEnableHeaderTranslationContent,
                            mRefreshHeader
                        ))
                    val showFooter =
                        (mRefreshFooter != null && isEnableRefreshOrLoadMore(mEnableLoadMore) && isEnableTranslationContent(
                            mEnableFooterTranslationContent,
                            mRefreshFooter
                        ))
                    val widthSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        thisView.getPaddingLeft() + thisView.getPaddingRight() + mlp.leftMargin + mlp.rightMargin,
                        lp.width
                    )
                    val heightSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        thisView.paddingTop + thisView.paddingBottom + mlp.topMargin + mlp.bottomMargin +
                                (if (needPreview && showHeader) mHeaderHeight else 0) +
                                (if (needPreview && showFooter) mFooterHeight else 0),
                        lp.height
                    )
                    contentView.measure(widthSpec, heightSpec)
                    minimumWidth += contentView.measuredWidth + mlp.leftMargin + mlp.rightMargin
                    minimumHeight += contentView.measuredHeight + mlp.topMargin + mlp.bottomMargin
                }
            }

            i++
        }
        minimumWidth += thisView.getPaddingLeft() + thisView.getPaddingRight()
        minimumHeight += thisView.paddingTop + thisView.paddingBottom
        super.setMeasuredDimension(
            resolveSize(max(minimumWidth, super.getSuggestedMinimumWidth()), widthMeasureSpec),
            resolveSize(
                max(minimumHeight, super.getSuggestedMinimumHeight()),
                heightMeasureSpec
            )
        )

        mLastTouchX = thisView.measuredWidth / 2f
    }

    /**
     * 布局 Header Footer Content
     * 1.布局代码看起来相对简单，时因为测量的时候，已经做了复杂的计算，布局的时候，直接按照测量结果，布局就可以了
     * @param changed 是否改变
     * @param l 左
     * @param t 上
     * @param r 右
     * @param b 下
     */
    @SuppressLint("UseKtx")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val thisView: View = this
        val paddingLeft = thisView.getPaddingLeft()
        val paddingTop = thisView.paddingTop
        var i = 0
        val len = super.getChildCount()
        while (i < len) {
            val child = super.getChildAt(i)

            if (child.visibility == GONE || "GONE" == child.getTag(R.id.srl_tag)) {
                i++
                continue
            }

            mRefreshContent?.also {
                if (it.getView() === child) {
                    val isPreviewMode =
                        thisView.isInEditMode && mEnablePreviewInEditMode && isEnableRefreshOrLoadMore(
                            mEnableRefresh
                        ) && mRefreshHeader != null
                    val contentView: View = it.getView()
                    val lp = contentView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val left = paddingLeft + mlp.leftMargin
                    var top = paddingTop + mlp.topMargin
                    val right = left + contentView.measuredWidth
                    var bottom = top + contentView.measuredHeight
                    if (isPreviewMode && (isEnableTranslationContent(
                            mEnableHeaderTranslationContent,
                            mRefreshHeader
                        ))
                    ) {
                        top += mHeaderHeight
                        bottom += mHeaderHeight
                    }
                    contentView.layout(left, top, right, bottom)
                }
            }

            mRefreshHeader?.also {
                if (it.getView() === child) {
                    val isPreviewMode =
                        thisView.isInEditMode && mEnablePreviewInEditMode && isEnableRefreshOrLoadMore(
                            mEnableRefresh
                        )
                    val headerView: View = it.getView()
                    val lp = headerView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val left = mlp.leftMargin
                    var top = mlp.topMargin + mHeaderInsetStart
                    val right = left + headerView.measuredWidth
                    var bottom = top + headerView.measuredHeight
                    if (!isPreviewMode) {
                        if (it.getSpinnerStyle() === SpinnerStyle.Translate) {
                            top = top - mHeaderHeight
                            bottom = bottom - mHeaderHeight
                        }
                    }
                    headerView.layout(left, top, right, bottom)
                }
            }

            mRefreshFooter?.also {
                if (it.getView() === child) {
                    val isPreviewMode =
                        thisView.isInEditMode && mEnablePreviewInEditMode && isEnableRefreshOrLoadMore(
                            mEnableLoadMore
                        )
                    val footerView: View = it.getView()
                    val lp = footerView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val style: SpinnerStyle = it.getSpinnerStyle()
                    val left = mlp.leftMargin
                    var top = mlp.topMargin + thisView.measuredHeight - mFooterInsetStart
                    if (mFooterNoMoreData &&
                        mFooterNoMoreDataEffective &&
                        mEnableFooterFollowWhenNoMoreData &&
                        it.getSpinnerStyle() === SpinnerStyle.Translate &&
                        isEnableRefreshOrLoadMore(mEnableLoadMore)
                    ) {
                        mRefreshContent?.apply {
                            val contentView: View = getView()
                            val clp = contentView.layoutParams
                            val topMargin = if (clp is MarginLayoutParams) clp.topMargin else 0
                            top = paddingTop + paddingTop + topMargin + contentView.measuredHeight
                        }
                    }

                    if (style === SpinnerStyle.MatchLayout) {
                        top = mlp.topMargin - mFooterInsetStart
                    } else if (isPreviewMode
                        || style === SpinnerStyle.FixedFront || style === SpinnerStyle.FixedBehind
                    ) {
                        top -= mFooterHeight
                    } else if (style.scale && mSpinner < 0) {
                        top -= max(
                            if (isEnableRefreshOrLoadMore(mEnableLoadMore)) -mSpinner else 0,
                            0
                        )
                    }
                    val right = left + footerView.measuredWidth
                    val bottom = top + footerView.measuredHeight
                    footerView.layout(left, top, right, bottom)
                }
            }

            i++
        }
    }

    /**
     * 重写 onDetachedFromWindow 来完成 smart 的特定功能
     * 1.恢复原始状态
     * 2.清除动画数据 （防止内存泄露）
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mAttachedToWindow = false
        mManualLoadMore = true
        animationRunnable = null
        if (reboundAnimator != null) {
            val animator: Animator? = reboundAnimator
            animator?.removeAllListeners()
            reboundAnimator?.removeAllUpdateListeners()
            reboundAnimator?.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
            reboundAnimator?.cancel() //会触发 cancel 和 end 调用
            reboundAnimator = null
        }
        /*
         * 2020-5-27
         * https://github.com/scwang90/SmartRefreshLayout/issues/1166
         * 修复 Fragment 脱离屏幕再回到时，菊花转圈，无法关闭的问题。
         * Smart 脱离屏幕时，必须重置状态，清空mHandler，否则动画等效果会导致 APP 内存泄露
         */
        if (mState == RefreshState.Refreshing) {
            mRefreshHeader?.onFinish(this, false)
        }
        if (mState == RefreshState.Loading) {
            mRefreshFooter?.onFinish(this, false)
        }
        if (mSpinner != 0) {
            mKernel.moveSpinner(0, true)
        }
        if (mState != RefreshState.None) {
            notifyStateChanged(RefreshState.None)
        }
        mHandler?.removeCallbacksAndMessages(null)
        /*
         * https://github.com/scwang90/SmartRefreshLayout/issues/716
         * 在一些特殊情况下，当触发上拉加载更多后，
         * 如果 onDetachedFromWindow 在 finishLoadMore 的 Runnable 执行之前被调用，
         * 将会导致 mFooterLocked 一直为 true，再也无法上滑列表，
         * 建议在 onDetachedFromWindow 方法中重置 mFooterLocked = false
         */
        mFooterLocked = false
    }

    /**
     * 重写 drawChild 来完成 smart 的特定功能
     * 1.为 Header 和 Footer 绘制背景 （设置了背景才绘制）
     * 2.为 Header 和 Footer 在 FixedBehind 样式时，做剪裁功能 （mEnableClipHeaderWhenFixedBehind=true 才做）
     * @param canvas 绘制发布
     * @param child 需要绘制的子View
     * @param drawingTime 绘制耗时
     */
    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val thisView: View = this
        val contentView: View? = mRefreshContent?.getView()
        mRefreshHeader?.also {
            if (it.getView() === child) {
                if (!isEnableRefreshOrLoadMore(mEnableRefresh) || (!mEnablePreviewInEditMode && thisView.isInEditMode)) {
                    return true
                }
                if (contentView != null) {
                    var bottom = max(contentView.top + contentView.paddingTop + mSpinner, child.top)
                    if (mHeaderBackgroundColor != 0) {
                        if (it.getSpinnerStyle().scale) {
                            bottom = child.bottom
                        } else if (it.getSpinnerStyle() === SpinnerStyle.Translate) {
                            bottom = child.bottom + mSpinner
                        }

                        mPaint?.apply {
                            setColor(mHeaderBackgroundColor)
                            canvas.drawRect(
                                0f,
                                child.top.toFloat(),
                                thisView.width.toFloat(),
                                bottom.toFloat(),
                                this
                            )
                        }

                    }

                    /*
                     * 2019-12-24
                     * 修复 经典头拉伸状态下显示异常的问题
                     * 导致的原因 1.1.0 版本之后 Smart 不推荐 Scale 模式，主推 FixedBehind 模式
                     * 并且取消了对 child 的绘制裁剪，所以经典组件需要重写 dispatchDraw 自行裁剪
                     */
                    if ((mEnableClipHeaderWhenFixedBehind && it.getSpinnerStyle() === SpinnerStyle.FixedBehind) || it.getSpinnerStyle().scale) {
                        canvas.save()
                        canvas.clipRect(child.left, child.top, child.right, bottom)
                        val ret = super.drawChild(canvas, child, drawingTime)
                        canvas.restore()
                        return ret
                    }
                }
            }
        }
        mRefreshFooter?.also {
            if (mRefreshFooter != null && mRefreshFooter!!.getView() === child) {
                if (!isEnableRefreshOrLoadMore(mEnableLoadMore) || (!mEnablePreviewInEditMode && thisView.isInEditMode())) {
                    return true
                }
                if (contentView != null) {
                    var top = min(
                        contentView.getBottom() - contentView.getPaddingBottom() + mSpinner,
                        child.getBottom()
                    )
                    if (mFooterBackgroundColor != 0 && mPaint != null) {
                        mPaint!!.setColor(mFooterBackgroundColor)
                        if (mRefreshFooter!!.getSpinnerStyle().scale) {
                            top = child.getTop()
                        } else if (mRefreshFooter!!.getSpinnerStyle() === SpinnerStyle.Translate) {
                            top = child.getTop() + mSpinner
                        }
                        canvas.drawRect(
                            0f,
                            top.toFloat(),
                            thisView.getWidth().toFloat(),
                            child.getBottom().toFloat(),
                            mPaint!!
                        )
                    }
                    /*
                     * 2019-12-24
                     * 修复 经典头拉伸状态下显示异常的问题
                     * 导致的原因 1.1.0 版本之后 Smart 不推荐 Scale 模式，主推 FixedBehind 模式
                     * 并且取消了对 child 的绘制裁剪，所以经典组件需要重写 dispatchDraw 自行裁剪
                     */
                    if ((mEnableClipFooterWhenFixedBehind && mRefreshFooter?.getSpinnerStyle() === SpinnerStyle.FixedBehind) || mRefreshFooter?.getSpinnerStyle()?.scale == true) {
                        canvas.save()
                        canvas.clipRect(child.getLeft(), top, child.getRight(), child.getBottom())
                        val ret = super.drawChild(canvas, child, drawingTime)
                        canvas.restore()
                        return ret
                    }
                }
            }
        }
        return super.drawChild(canvas, child, drawingTime)
    }

    protected var mVerticalPermit: Boolean = false //竖直通信证（用于特殊事件的权限判定）

    /**
     * 重写 computeScroll 来完成 smart 的特定功能
     * 1.越界回弹
     * 2.边界碰撞
     */
    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val finalY = mScroller.finalY
            if ((finalY < 0 && (mEnableRefresh || mEnableOverScrollDrag) && mRefreshContent?.canRefresh() == true)
                || (finalY > 0 && (mEnableLoadMore || mEnableOverScrollDrag) && mRefreshContent?.canLoadMore() == true)
            ) {
                if (mVerticalPermit) {
                    val velocity: Float =
                        if (finalY > 0) -mScroller.currVelocity else mScroller.currVelocity
                    animSpinnerBounce(velocity)
                }
                mScroller.forceFinished(true)
            } else {
                mVerticalPermit = true //打开竖直通行证
                val thisView: View = this
                thisView.invalidate()
            }
        }
    }

    protected var mFalsifyEvent: MotionEvent? = null

    /**
     * 事件分发 （手势核心）
     * 1.多点触摸
     * 2.无缝衔接内容滚动
     * @param e 事件
     */
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        //<editor-fold desc="多点触摸计算代码">
        //---------------------------------------------------------------------------
        //多点触摸计算代码
        //---------------------------------------------------------------------------
        val action = e.actionMasked
        val pointerUp = action == MotionEvent.ACTION_POINTER_UP
        val skipIndex = if (pointerUp) e.actionIndex else -1

        // Determine focal point
        var sumX = 0f
        var sumY = 0f
        val count = e.pointerCount
        for (i in 0..<count) {
            if (skipIndex == i) continue
            sumX += e.getX(i)
            sumY += e.getY(i)
        }
        val div = if (pointerUp) count - 1 else count
        val touchX = sumX / div
        val touchY = sumY / div
        if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_POINTER_DOWN)
            && mIsBeingDragged
        ) {
            mTouchY += touchY - mLastTouchY
        }
        mLastTouchX = touchX
        mLastTouchY = touchY

        if (action == MotionEvent.ACTION_DOWN) {
            /*
             * https://github.com/scwang90/SmartRefreshLayout/issues/1540
             * 辅助下面的问题修复，拦截手势事件的时候，对 MotionEvent.ACTION_DOWN 的触发时间进行备份
             * 计算是否点击需要判断 ACTION_DOWN 和 ACTION_UP 的时间间隔，还有按下坐标
             */
            mLastTouchXOnActionDown = touchX
            mLastTouchYOnActionDown = touchY
            mLastTimeOnActionDown = System.currentTimeMillis()
        }


        //---------------------------------------------------------------------------
        //嵌套滚动模式辅助
        //---------------------------------------------------------------------------
        val thisView: View = this
        if (mNestedInProgress) { //嵌套滚动时，补充竖直方向不滚动，但是水平方向滚动，需要通知 onHorizontalDrag
            val totalUnconsumed = mTotalUnconsumed
            val ret = super.dispatchTouchEvent(e)
            if (action == MotionEvent.ACTION_MOVE) {
                if (totalUnconsumed == mTotalUnconsumed) {
                    val offsetX = mLastTouchX.toInt()
                    val offsetMax = thisView.width
                    val percentX = mLastTouchX / (if (offsetMax == 0) 1 else offsetMax)
                    if (isEnableRefreshOrLoadMore(mEnableRefresh) && mSpinner > 0 && mRefreshHeader?.isSupportHorizontalDrag() == true) {
                        mRefreshHeader?.onHorizontalDrag(percentX, offsetX, offsetMax)
                    } else if (isEnableRefreshOrLoadMore(mEnableLoadMore) && mSpinner < 0 && mRefreshFooter?.isSupportHorizontalDrag() == true) {
                        mRefreshFooter?.onHorizontalDrag(percentX, offsetX, offsetMax)
                    }
                }
            }
            return ret
        } else if (!thisView.isEnabled || (!mEnableRefresh && !mEnableLoadMore && !mEnableOverScrollDrag)
            || (mHeaderNeedTouchEventWhenRefreshing && ((mState.isOpening || mState.isFinishing) && mState.isHeader))
            || (mFooterNeedTouchEventWhenLoading && ((mState.isOpening || mState.isFinishing) && mState.isFooter))
        ) {
            return super.dispatchTouchEvent(e)
        } else if (mState.isFinishing) {
            /*
             * https://github.com/scwang90/SmartRefreshLayout/issues/1540
             * 在刷新动画关闭的时候，点击列表无法跳转，
             * 是因为下面的 if (interceptAnimatorByAction || mState.isFinishing ) return false
             * 屏蔽了所有手势事件，屏蔽的原因是如果关闭动画正在执行的时候，随意滑动列表会导致意外的错乱
             * 目前的修复方法并不是放开屏蔽，而是针对 先 ACTION_DOWN 再 ACTION_UP 的点击事件进行补偿处理
             * 从而实现，完成动画执行时，列表不可手动滑动，但是可以点击列表项
             */
            if (action == MotionEvent.ACTION_UP) {
                //要求时间间隔小于半秒钟
                if (System.currentTimeMillis() - mLastTimeOnActionDown < 500) {
                    val dx = touchX - mLastTouchXOnActionDown
                    val dy = touchY - mLastTouchYOnActionDown
                    //要求坐标偏移小于系统阈值
                    if (abs(dx) < mTouchSlop && abs(dy) < mTouchSlop) {
                        e.setAction(MotionEvent.ACTION_DOWN)
                        super.dispatchTouchEvent(e)
                        e.setAction(MotionEvent.ACTION_UP)
                        return super.dispatchTouchEvent(e)
                    }
                }
            }
            /*
             * 返回 true，但是不调用 super.dispatchTouchEvent(e)，表示已经处理，但实际情况是没处理。
             * 这样在拦截到 down move 事件时，才会继续触发 up 事件
             * 否则 返回false 后面不会再接收到 move 和 up 的事件
             */
            return true
        }

        if (interceptAnimatorByAction(action)
            || (mState == RefreshState.Loading && mDisableContentWhenLoading)
            || (mState == RefreshState.Refreshing && mDisableContentWhenRefresh)
        ) {
            return false
        }

        //-------------------------------------------------------------------------//

        //---------------------------------------------------------------------------
        //传统模式滚动
        //---------------------------------------------------------------------------
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                /*----------------------------------------------------*/
                /*                   速度追踪初始化                    */
                /*----------------------------------------------------*/
                mCurrentVelocity = 0
                mVelocityTracker.addMovement(e)
                mScroller.forceFinished(true)
                /*----------------------------------------------------*/
                /*                   触摸事件初始化                    */
                /*----------------------------------------------------*/
                mTouchX = touchX
                mTouchY = touchY
                mLastSpinner = 0
                mTouchSpinner = mSpinner
                mIsBeingDragged = false
                mEnableDisallowIntercept = false
                /*----------------------------------------------------*/
                mSuperDispatchTouchEvent = super.dispatchTouchEvent(e)
                if (mState == RefreshState.TwoLevel && mTouchY < thisView.measuredHeight * (1 - mTwoLevelBottomPullUpToCloseRate)) {
                    mDragDirection = 'h' //二级刷新标记水平滚动来禁止拖动
                    return mSuperDispatchTouchEvent
                }
                //为 RefreshContent 传递当前触摸事件的坐标，用于智能判断对应坐标位置View的滚动边界和相关信息
                mRefreshContent?.onActionDown(e)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = touchX - mTouchX
                var dy = touchY - mTouchY
                mVelocityTracker.addMovement(e) //速度追踪
                if (!mIsBeingDragged && !mEnableDisallowIntercept && mDragDirection != 'h' && mRefreshContent != null) {
                    //没有拖动之前，检测  canRefresh canLoadMore 来开启拖动
                    if (mDragDirection == 'v' || (abs(dy) >= mTouchSlop && abs(dx) < abs(dy))) { //滑动允许最大角度为45度
                        mDragDirection = 'v'
                        if (dy > 0 && (mSpinner < 0 || ((mEnableOverScrollDrag || mEnableRefresh) && mRefreshContent?.canRefresh() == true))) {
                            mIsBeingDragged = true
                            mTouchY = touchY - mTouchSlop //调整 mTouchSlop 偏差
                        } else if (dy < 0 && (mSpinner > 0 || ((mEnableOverScrollDrag || mEnableLoadMore) && ((mState == RefreshState.Loading && mFooterLocked) || mRefreshContent?.canLoadMore() == true)))) {
                            mIsBeingDragged = true
                            mTouchY = touchY + mTouchSlop //调整 mTouchSlop 偏差
                        }
                        if (mIsBeingDragged) {
                            dy = touchY - mTouchY //调整 mTouchSlop 偏差 重新计算 dy
                            if (mSuperDispatchTouchEvent) { //如果父类拦截了事件，发送一个取消事件通知
                                e.setAction(MotionEvent.ACTION_CANCEL)
                                super.dispatchTouchEvent(e)
                            }
                            mKernel.setState(if (mSpinner > 0 || (mSpinner == 0 && dy > 0)) RefreshState.PullDownToRefresh else RefreshState.PullUpToLoad)
                            val parent = thisView.parent
                            if (parent is ViewGroup) {
                                //修复问题 https://github.com/scwang90/SmartRefreshLayout/issues/580
                                parent.requestDisallowInterceptTouchEvent(true) //通知父控件不要拦截事件
                            }
                        }
                    } else if (abs(dx) >= mTouchSlop && abs(dx) > abs(dy) && mDragDirection != 'v') {
                        mDragDirection = 'h' //标记为水平拖动，将无法再次触发 下拉刷新 上拉加载
                    }
                }
                if (mIsBeingDragged) {
                    var spinner = dy.toInt() + mTouchSpinner
                    if ((mViceState.isHeader && (spinner < 0 || mLastSpinner < 0)) || (mViceState.isFooter && (spinner > 0 || mLastSpinner > 0))) {
                        mLastSpinner = spinner
                        val time = e.eventTime
                        if (mFalsifyEvent == null) {
                            mFalsifyEvent = MotionEvent.obtain(
                                time,
                                time,
                                MotionEvent.ACTION_DOWN,
                                mTouchX + dx,
                                mTouchY,
                                0
                            )
                            super.dispatchTouchEvent(mFalsifyEvent)
                        }
                        val em = MotionEvent.obtain(
                            time,
                            time,
                            MotionEvent.ACTION_MOVE,
                            mTouchX + dx,
                            mTouchY + spinner,
                            0
                        )
                        super.dispatchTouchEvent(em)
                        if (mFooterLocked && dy > mTouchSlop && mSpinner < 0) {
                            mFooterLocked = false //内容向下滚动时 解锁Footer 的锁定
                        }
                        if (spinner > 0 && ((mEnableOverScrollDrag || mEnableRefresh) && mRefreshContent?.canRefresh() == true)) {
                            mLastTouchY = touchY
                            mTouchY = mLastTouchY
                            spinner = 0
                            mTouchSpinner = 0
                            mKernel.setState(RefreshState.PullDownToRefresh)
                        } else if (spinner < 0 && ((mEnableOverScrollDrag || mEnableLoadMore) && mRefreshContent?.canLoadMore() == true)) {
                            mLastTouchY = touchY
                            mTouchY = mLastTouchY
                            spinner = 0
                            mTouchSpinner = 0
                            mKernel.setState(RefreshState.PullUpToLoad)
                        }
                        if ((mViceState.isHeader && spinner < 0) || (mViceState.isFooter && spinner > 0)) {
                            if (mSpinner != 0) {
                                moveSpinnerInfinitely(0f)
                            }
                            return true
                        } else if (mFalsifyEvent != null) {
                            mFalsifyEvent = null
                            em.setAction(MotionEvent.ACTION_CANCEL)
                            super.dispatchTouchEvent(em)
                        }
                        em.recycle()
                    }
                    moveSpinnerInfinitely(spinner.toFloat())
                    return true
                } else if (mFooterLocked && dy > mTouchSlop && mSpinner < 0) {
                    mFooterLocked = false //内容向下滚动时 解锁Footer 的锁定
                }
            }

            MotionEvent.ACTION_UP -> {
                mVelocityTracker.addMovement(e)
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                mCurrentVelocity = mVelocityTracker.yVelocity.toInt()
                startFlingIfNeed(0f)
                mVelocityTracker.clear() //清空速度追踪器
                mDragDirection = 'n' //关闭拖动方向
                if (mFalsifyEvent != null) {
                    mFalsifyEvent?.recycle()
                    mFalsifyEvent = null
                    val time = e.eventTime
                    val ec = MotionEvent.obtain(time, time, action, mTouchX, touchY, 0)
                    super.dispatchTouchEvent(ec)
                    ec.recycle()
                }
                overSpinner()
                if (mIsBeingDragged) {
                    mIsBeingDragged = false //关闭拖动状态
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                mVelocityTracker.clear()
                mDragDirection = 'n'
                if (mFalsifyEvent != null) {
                    mFalsifyEvent?.recycle()
                    mFalsifyEvent = null
                    val time = e.eventTime
                    val ec = MotionEvent.obtain(time, time, action, mTouchX, touchY, 0)
                    super.dispatchTouchEvent(ec)
                    ec.recycle()
                }
                overSpinner()
                if (mIsBeingDragged) {
                    mIsBeingDragged = false
                    return true
                }
            }
        }
        //-------------------------------------------------------------------------//
        return super.dispatchTouchEvent(e)
    }

    /**
     * 这段代码来自谷歌官方的 SwipeRefreshLayout
     * 主要是为了让老版本的 ListView 能平滑的下拉 而选择性的屏蔽 requestDisallowInterceptTouchEvent
     * 应用场景已经在英文注释中解释清楚，大部分第三方下拉刷新库都保留了这段代码，本库也不例外
     */
    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        val target: View = mRefreshContent?.getScrollableView() ?: return
        if (ViewCompat.isNestedScrollingEnabled(target)) {
            mEnableDisallowIntercept = disallowIntercept
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
    }

    /**
     * 在必要的时候 开始 Fling 模式
     * @param flingVelocity 速度
     * @return true 可以拦截 嵌套滚动的 Fling
     */
    protected fun startFlingIfNeed(flingVelocity: Float): Boolean {
        val velocity = if (flingVelocity == 0f) mCurrentVelocity.toFloat() else flingVelocity
        if (abs(velocity) > mMinimumVelocity) {
            if (velocity * mSpinner < 0) {
                /*
                 * 列表准备惯性滑行的时候，如果速度关系
                 * velocity * mSpinner < 0 表示当前速度趋势，需要关闭 mSpinner 才合理
                 * 但是在 mState.isOpening（不含二楼） 状态 和 noMoreData 状态 时 mSpinner 不会自动关闭
                 * 需要使用 FlingRunnable 来关闭 mSpinner ，并在关闭结束后继续 fling 列表
                 */
                if (mState == RefreshState.Refreshing || mState == RefreshState.Loading || (mSpinner < 0 && mFooterNoMoreData)) {
                    animationRunnable = FlingRunnable(velocity).start()
                    return true
                } else if (mState.isReleaseToOpening) {
                    return true //拦截嵌套滚动时，即将刷新或者加载的 Fling
                }
            }
            if ((velocity < 0 && ((mEnableOverScrollBounce && (mEnableLoadMore || mEnableOverScrollDrag)) || (mState == RefreshState.Loading && mSpinner >= 0) || (mEnableAutoLoadMore && isEnableRefreshOrLoadMore(
                    mEnableLoadMore
                ))))
                || (velocity > 0 && ((mEnableOverScrollBounce && mEnableRefresh || mEnableOverScrollDrag) || (mState == RefreshState.Refreshing && mSpinner <= 0)))
            ) {
                /*
                 * 用于监听越界回弹、Refreshing、Loading、noMoreData 时自动拉出
                 * 做法：使用 mScroller.fling 模拟一个惯性滚动，因为 AbsListView 和 ScrollView 等等各种滚动控件内部都是用 mScroller.fling。
                 *      所以 mScroller.fling 的状态和 它们一样，可以试试判断它们的 fling 当前速度 和 是否结束。
                 *      并再 computeScroll 方法中试试判读它们是否滚动到了边界，得到此时的 fling 速度
                 *      如果 当前的速度还能继续 惯性滑行，自动拉出：越界回弹、Refreshing、Loading、noMoreData
                 */
                mVerticalPermit = false //关闭竖直通行证
                mScroller.fling(
                    0,
                    0,
                    0,
                    -velocity.toInt(),
                    0,
                    0,
                    -Int.Companion.MAX_VALUE,
                    Int.Companion.MAX_VALUE
                )
                mScroller.computeScrollOffset()
                val thisView: View = this
                thisView.invalidate()
            }
        }
        return false
    }

    /**
     * 在动画执行时，触摸屏幕，打断动画，转为拖动状态
     * @param action MotionEvent
     * @return 是否成功打断
     */
    protected fun interceptAnimatorByAction(action: Int): Boolean {
        if (action == MotionEvent.ACTION_DOWN) {
            if (reboundAnimator != null) {
                if (mState.isFinishing || mState == RefreshState.TwoLevelReleased || mState == RefreshState.RefreshReleased || mState == RefreshState.LoadReleased) {
                    return true //完成动画和打开动画不能被打断
                }
                if (mState == RefreshState.PullDownCanceled) {
                    mKernel.setState(RefreshState.PullDownToRefresh)
                } else if (mState == RefreshState.PullUpCanceled) {
                    mKernel.setState(RefreshState.PullUpToLoad)
                }
                reboundAnimator?.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
                reboundAnimator?.cancel() //会触发 cancel 和 end 调用
                reboundAnimator = null
            }
            animationRunnable = null
        }
        return reboundAnimator != null
    }

    /**
     * 设置并通知状态改变 （setState）
     * @param state 状态
     */
    protected fun notifyStateChanged(state: RefreshState) {
        val oldState = mState
        if (oldState != state) {
            mState = state
            mViceState = state
            val refreshHeader: OnStateChangedListener? = mRefreshHeader
            val refreshFooter: OnStateChangedListener? = mRefreshFooter
            val refreshListener: OnStateChangedListener? = mOnMultiListener
            refreshHeader?.onStateChanged(this, oldState, state)
            refreshFooter?.onStateChanged(this, oldState, state)
            refreshListener?.onStateChanged(this, oldState, state)
            if (state == RefreshState.LoadFinish) {
                mFooterLocked = false
            }
        } else if (mViceState != mState) {
            /*
             * notifyStateChanged，mViceState 必须和 主状态 一致
             */
            mViceState = mState
        }
    }

    /**
     * 直接将状态设置为 Loading 正在加载
     * @param triggerLoadMoreEvent 是否触发加载回调
     */
    protected fun setStateDirectLoading(triggerLoadMoreEvent: Boolean) {
        if (mState != RefreshState.Loading) {
            mLastOpenTime = System.currentTimeMillis()
            mFooterLocked = true //Footer 正在loading 的时候是否锁住 列表不能向上滚动
            notifyStateChanged(RefreshState.Loading)
            if (mLoadMoreListener != null) {
                if (triggerLoadMoreEvent) {
                    mLoadMoreListener?.onLoadMore(this)
                }
            } else if (mOnMultiListener == null) {
                finishLoadMore(2000) //如果没有任何加载监听器，两秒之后自动关闭
            }
            if (mRefreshFooter != null) {
                val maxDragHeight =
                    if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
                mRefreshFooter?.onStartAnimator(this, mFooterHeight, maxDragHeight.toInt())
            }
            if (mOnMultiListener != null && mRefreshFooter is RefreshFooter) {
                val listener: OnLoadMoreListener? = mOnMultiListener
                if (triggerLoadMoreEvent) {
                    listener?.onLoadMore(this)
                }
                val maxDragHeight =
                    if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate

                val refreshFooter = mRefreshFooter as? RefreshFooter
                refreshFooter?.also {
                    mOnMultiListener?.onFooterStartAnimator(
                        it,
                        mFooterHeight,
                        maxDragHeight.toInt()
                    )
                }
            }
        }
    }

    /**
     * 设置状态为 Loading 正在加载
     * @param notify 是否触发通知事件
     */
    protected fun setStateLoading(notify: Boolean) {
        val listener: AnimatorListenerAdapter = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animation.duration == 0L) {
                    return  //0 表示被取消
                }
                setStateDirectLoading(notify)
            }
        }
        notifyStateChanged(RefreshState.LoadReleased)
        val animator = mKernel.animSpinner(-mFooterHeight)
        animator?.addListener(listener)
        if (mRefreshFooter != null) {
            //onReleased 的执行顺序定在 animSpinner 之后 onAnimationEnd 之前
            // 这样 onReleased 内部 可以做出 对 前面 animSpinner 的覆盖 操作
            val maxDragHeight =
                if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
            mRefreshFooter?.onReleased(this, mFooterHeight, maxDragHeight.toInt())
        }
        if (mOnMultiListener != null && mRefreshFooter is RefreshFooter) {
            //同 mRefreshFooter.onReleased 一致
            val maxDragHeight =
                if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
            mOnMultiListener?.onFooterReleased(
                mRefreshFooter as RefreshFooter,
                mFooterHeight,
                maxDragHeight.toInt()
            )
        }
        //onAnimationEnd 会改变状态为 loading 必须在 onReleased 之后调用
        animator?.also { listener.onAnimationEnd(it) }
    }

    /**
     * 设置状态为 Refreshing 正在刷新
     * @param notify 是否触发通知事件
     */
    protected fun setStateRefreshing(notify: Boolean) {
        val listener: AnimatorListenerAdapter = object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (animation.duration == 0L) {
                    return  //0 表示被取消
                }
                mLastOpenTime = System.currentTimeMillis()
                notifyStateChanged(RefreshState.Refreshing)
                if (mRefreshListener != null) {
                    if (notify) {
                        mRefreshListener?.onRefresh(this@SmartRefreshLayout)
                    }
                } else if (mOnMultiListener == null) {
                    finishRefresh(3000)
                }
                if (mRefreshHeader != null) {
                    val maxDragHeight =
                        (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
                    mRefreshHeader?.onStartAnimator(
                        this@SmartRefreshLayout,
                        mHeaderHeight,
                        maxDragHeight
                    )
                }
                if (mOnMultiListener != null && mRefreshHeader is RefreshHeader) {
                    if (notify) {
                        mOnMultiListener?.onRefresh(this@SmartRefreshLayout)
                    }
                    val maxDragHeight =
                        (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
                    mOnMultiListener?.onHeaderStartAnimator(
                        mRefreshHeader as RefreshHeader,
                        mHeaderHeight,
                        maxDragHeight
                    )
                }
            }
        }
        notifyStateChanged(RefreshState.RefreshReleased)
        val animator = mKernel.animSpinner(mHeaderHeight)
        animator?.addListener(listener)
        if (mRefreshHeader != null) {
            //onReleased 的执行顺序定在 animSpinner 之后 onAnimationEnd 之前
            // 这样 onRefreshReleased内部 可以做出 对 前面 animSpinner 的覆盖 操作
            val maxDragHeight =
                (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
            mRefreshHeader?.onReleased(this, mHeaderHeight, maxDragHeight)
        }
        if (mOnMultiListener != null && mRefreshHeader is RefreshHeader) {
            //同 mRefreshHeader.onReleased 一致
            val maxDragHeight =
                (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
            mOnMultiListener?.onHeaderReleased(
                mRefreshHeader as RefreshHeader,
                mHeaderHeight,
                maxDragHeight
            )
        }

        //onAnimationEnd 会改变状态为 Refreshing 必须在 onReleased 之后调用
        animator?.also { listener.onAnimationEnd(it) }
    }

    /**
     * 设置 副状态
     * @param state 状态
     */
    protected fun setViceState(state: RefreshState) {
        if (mState.isDragging && mState.isHeader != state.isHeader) {
            notifyStateChanged(RefreshState.None)
        }
        if (mViceState != state) {
            mViceState = state
        }
    }

    /**
     * 判断是否 下拉的时候 需要 移动内容
     * @param enable mEnableHeaderTranslationContent or mEnableFooterTranslationContent
     * @param internal mRefreshHeader or mRefreshFooter
     * @return enable
     */
    protected fun isEnableTranslationContent(
        enable: Boolean,
        internal: RefreshComponent?
    ): Boolean {
        /*
         * 2019-12-25 修复 2.0 版本之后无默认 Header Footer 导致的纯滚动模式无效 添加 @Nullable
         */
        return enable || mEnablePureScrollMode || internal == null || internal.getSpinnerStyle() === SpinnerStyle.FixedBehind
    }

    /**
     * 是否真正的 可以刷新或者加载（与 越界拖动 纯滚动模式区分开来）
     * 判断时候可以 刷新 或者 加载（直接影响，Header，Footer 是否显示）
     * @param enable mEnableRefresh or mEnableLoadMore
     * @return enable
     */
    protected fun isEnableRefreshOrLoadMore(enable: Boolean): Boolean {
        return enable && !mEnablePureScrollMode
    }

    protected var animationRunnable: Runnable? = null
    protected var reboundAnimator: ValueAnimator? = null


    init {
        val configuration = ViewConfiguration.get(context)

        mHandler = Handler(Looper.getMainLooper())
        mScroller = Scroller(context)
        mVelocityTracker = VelocityTracker.obtain()
        mScreenHeightPixels = context.resources.displayMetrics.heightPixels
        mReboundInterpolator = SmartUtil(SmartUtil.INTERPOLATOR_VISCOUS_FLUID)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity

        mFooterHeight = dp2px(60f)
        mHeaderHeight = dp2px(100f)

        context.withStyledAttributes(attrs, R.styleable.SmartRefreshLayout) {
            /*
         * SmartRefreshLayout 继承 ViewGroup 后即使不设置 android:clipToPadding，也等效于 android:clipToPadding=true
         * 特意添加 android:clipToPadding 来在 Java 代码中 判断是否设置过，没有设置强制 setClipToPadding(false)
         * android:clipChildren 也是同样
         */
            if (!hasValue(R.styleable.SmartRefreshLayout_android_clipToPadding)) {
                super.setClipToPadding(false)
            }
            if (!hasValue(R.styleable.SmartRefreshLayout_android_clipChildren)) {
                super.setClipChildren(false)
            }

            sRefreshInitializer?.initialize(context, this@SmartRefreshLayout) //调用全局初始化

            mDragRate = getFloat(R.styleable.SmartRefreshLayout_srlDragRate, mDragRate)
            mHeaderMaxDragRate =
                getFloat(R.styleable.SmartRefreshLayout_srlHeaderMaxDragRate, mHeaderMaxDragRate)
            mFooterMaxDragRate =
                getFloat(R.styleable.SmartRefreshLayout_srlFooterMaxDragRate, mFooterMaxDragRate)
            mHeaderTriggerRate =
                getFloat(R.styleable.SmartRefreshLayout_srlHeaderTriggerRate, mHeaderTriggerRate)
            mFooterTriggerRate =
                getFloat(R.styleable.SmartRefreshLayout_srlFooterTriggerRate, mFooterTriggerRate)
            mEnableRefresh =
                getBoolean(R.styleable.SmartRefreshLayout_srlEnableRefresh, mEnableRefresh)
            mReboundDuration =
                getInt(R.styleable.SmartRefreshLayout_srlReboundDuration, mReboundDuration)
            mEnableLoadMore =
                getBoolean(R.styleable.SmartRefreshLayout_srlEnableLoadMore, mEnableLoadMore)
            mHeaderHeight = getDimensionPixelOffset(
                R.styleable.SmartRefreshLayout_srlHeaderHeight,
                mHeaderHeight
            )
            mFooterHeight = getDimensionPixelOffset(
                R.styleable.SmartRefreshLayout_srlFooterHeight,
                mFooterHeight
            )
            mHeaderInsetStart = getDimensionPixelOffset(
                R.styleable.SmartRefreshLayout_srlHeaderInsetStart,
                mHeaderInsetStart
            )
            mFooterInsetStart = getDimensionPixelOffset(
                R.styleable.SmartRefreshLayout_srlFooterInsetStart,
                mFooterInsetStart
            )
            mDisableContentWhenRefresh = getBoolean(
                R.styleable.SmartRefreshLayout_srlDisableContentWhenRefresh,
                mDisableContentWhenRefresh
            )
            mDisableContentWhenLoading = getBoolean(
                R.styleable.SmartRefreshLayout_srlDisableContentWhenLoading,
                mDisableContentWhenLoading
            )
            mEnableHeaderTranslationContent = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableHeaderTranslationContent,
                mEnableHeaderTranslationContent
            )
            mEnableFooterTranslationContent = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableFooterTranslationContent,
                mEnableFooterTranslationContent
            )
            mEnablePreviewInEditMode = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnablePreviewInEditMode,
                mEnablePreviewInEditMode
            )
            mEnableAutoLoadMore =
                getBoolean(
                    R.styleable.SmartRefreshLayout_srlEnableAutoLoadMore,
                    mEnableAutoLoadMore
                )
            mEnableOverScrollBounce = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableOverScrollBounce,
                mEnableOverScrollBounce
            )
            mEnablePureScrollMode = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnablePureScrollMode,
                mEnablePureScrollMode
            )
            mEnableScrollContentWhenLoaded = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableScrollContentWhenLoaded,
                mEnableScrollContentWhenLoaded
            )
            mEnableScrollContentWhenRefreshed = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableScrollContentWhenRefreshed,
                mEnableScrollContentWhenRefreshed
            )
            mEnableLoadMoreWhenContentNotFull = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableLoadMoreWhenContentNotFull,
                mEnableLoadMoreWhenContentNotFull
            )
            mEnableFooterFollowWhenNoMoreData = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableFooterFollowWhenLoadFinished,
                mEnableFooterFollowWhenNoMoreData
            )
            mEnableFooterFollowWhenNoMoreData = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableFooterFollowWhenNoMoreData,
                mEnableFooterFollowWhenNoMoreData
            )
            mEnableClipHeaderWhenFixedBehind = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableClipHeaderWhenFixedBehind,
                mEnableClipHeaderWhenFixedBehind
            )
            mEnableClipFooterWhenFixedBehind = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableClipFooterWhenFixedBehind,
                mEnableClipFooterWhenFixedBehind
            )
            mEnableOverScrollDrag = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableOverScrollDrag,
                mEnableOverScrollDrag
            )
            mFixedHeaderViewId = getResourceId(
                R.styleable.SmartRefreshLayout_srlFixedHeaderViewId,
                mFixedHeaderViewId
            )
            mFixedFooterViewId = getResourceId(
                R.styleable.SmartRefreshLayout_srlFixedFooterViewId,
                mFixedFooterViewId
            )
            mHeaderTranslationViewId = getResourceId(
                R.styleable.SmartRefreshLayout_srlHeaderTranslationViewId,
                mHeaderTranslationViewId
            )
            mFooterTranslationViewId = getResourceId(
                R.styleable.SmartRefreshLayout_srlFooterTranslationViewId,
                mFooterTranslationViewId
            )
            mEnableNestedScrolling = getBoolean(
                R.styleable.SmartRefreshLayout_srlEnableNestedScrolling,
                mEnableNestedScrolling
            )
            mNestedChild.setNestedScrollingEnabled(mEnableNestedScrolling)

            mManualLoadMore =
                mManualLoadMore || hasValue(R.styleable.SmartRefreshLayout_srlEnableLoadMore)
            mManualHeaderTranslationContent =
                mManualHeaderTranslationContent || hasValue(R.styleable.SmartRefreshLayout_srlEnableHeaderTranslationContent)
            mManualFooterTranslationContent =
                mManualFooterTranslationContent || hasValue(R.styleable.SmartRefreshLayout_srlEnableFooterTranslationContent)
            mHeaderHeightStatus =
                if (hasValue(R.styleable.SmartRefreshLayout_srlHeaderHeight)) DimensionStatus.XmlLayoutUnNotify else mHeaderHeightStatus
            mFooterHeightStatus =
                if (hasValue(R.styleable.SmartRefreshLayout_srlFooterHeight)) DimensionStatus.XmlLayoutUnNotify else mFooterHeightStatus

            val accentColor = getColor(R.styleable.SmartRefreshLayout_srlAccentColor, 0)
            val primaryColor = getColor(R.styleable.SmartRefreshLayout_srlPrimaryColor, 0)
            if (primaryColor != 0) {
                mPrimaryColors = if (accentColor != 0) {
                    intArrayOf(primaryColor, accentColor)
                } else {
                    intArrayOf(primaryColor)
                }
            } else if (accentColor != 0) {
                mPrimaryColors = intArrayOf(0, accentColor)
            }

            if (mEnablePureScrollMode && !mManualLoadMore && !mEnableLoadMore) {
                mEnableLoadMore = true
            }

        }
    }

    inner class FlingRunnable internal constructor(var mVelocity: Float) : Runnable {
        var mOffset: Int
        var mFrame: Int = 0
        var mFrameDelay: Int = 10
        var mDamping: Float = 0.98f //每帧速度衰减值
        var mStartTime: Long = 0
        var mLastTime: Long = AnimationUtils.currentAnimationTimeMillis()

        init {
            mOffset = mSpinner
        }

        fun start(): Runnable? {
            if (mState.isFinishing) {
                return null
            }
            if (mSpinner != 0 && (!(mState.isOpening || (mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(
                    mEnableLoadMore
                ))) || ((mState == RefreshState.Loading || (mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(
                    mEnableLoadMore
                ))) && mSpinner < -mFooterHeight)
                        || (mState == RefreshState.Refreshing && mSpinner > mHeaderHeight))
            ) {
                var frame = 0
                var offset = mSpinner
                val spinner = mSpinner
                var velocity = mVelocity
                while (spinner * offset > 0) {
                    velocity *= mDamping.toDouble()
                        .pow(((++frame) * mFrameDelay / 10f).toDouble())
                        .toFloat()
                    val velocityFrame = (velocity * (1f * mFrameDelay / 1000))
                    if (abs(velocityFrame) < 1) {
                        if (!mState.isOpening || (mState == RefreshState.Refreshing && offset > mHeaderHeight)
                            || (mState != RefreshState.Refreshing && offset < -mFooterHeight)
                        ) {
                            return null
                        }
                        break
                    }
                    offset = (offset + velocityFrame).toInt()
                }
            }
            mStartTime = AnimationUtils.currentAnimationTimeMillis()
            mHandler?.postDelayed(this, mFrameDelay.toLong())
            return this
        }

        override fun run() {
            if (animationRunnable === this && !mState.isFinishing) {
                val now = AnimationUtils.currentAnimationTimeMillis()
                val span = now - mLastTime
                mVelocity *= mDamping.toDouble()
                    .pow(((now - mStartTime) / (1000f / mFrameDelay)).toDouble()).toFloat()
                val velocity = (mVelocity * (1f * span / 1000))
                if (abs(velocity) > 1) {
                    mLastTime = now
                    mOffset = (mOffset + velocity).toInt()
                    if (mSpinner * mOffset > 0) {
                        mKernel.moveSpinner(mOffset, true)
                        mHandler?.postDelayed(this, mFrameDelay.toLong())
                    } else {
                        animationRunnable = null
                        mKernel.moveSpinner(0, true)

                        mRefreshContent?.getScrollableView()?.apply {
                            fling(this, -mVelocity.toInt())
                        }
                        if (mFooterLocked && velocity > 0) {
                            mFooterLocked = false
                        }
                    }
                } else {
                    animationRunnable = null
                }
            }
        }
    }

    protected inner class BounceRunnable internal constructor(
        var mVelocity: Float,
        var mSmoothDistance: Int
    ) : Runnable {
        var mFrame: Int = 0
        var mFrameDelay: Int = 10
        var mLastTime: Long
        var mOffset: Float = 0f

        init {
            mLastTime = AnimationUtils.currentAnimationTimeMillis()
            mHandler?.postDelayed(this, mFrameDelay.toLong())
            if (mVelocity > 0) {
                mKernel.setState(RefreshState.PullDownToRefresh)
            } else {
                mKernel.setState(RefreshState.PullUpToLoad)
            }
        }

        override fun run() {
            if (animationRunnable === this && !mState.isFinishing) {
                if (abs(mSpinner) >= abs(mSmoothDistance)) {
                    if (mSmoothDistance != 0) {
                        mVelocity *= 0.45.pow((++mFrame * 2).toDouble())
                            .toFloat() //刷新、加载时回弹滚动数度衰减
                    } else {
                        mVelocity *= 0.85.pow((++mFrame * 2).toDouble()).toFloat() //回弹滚动数度衰减
                    }
                } else {
                    mVelocity *= 0.95.pow((++mFrame * 2).toDouble()).toFloat() //平滑滚动数度衰减
                }
                val now = AnimationUtils.currentAnimationTimeMillis()
                val t = 1f * (now - mLastTime) / 1000
                val velocity = mVelocity * t
                if (abs(velocity) >= 1) {
                    mLastTime = now
                    mOffset += velocity
                    moveSpinnerInfinitely(mOffset)
                    mHandler?.postDelayed(this, mFrameDelay.toLong())
                } else {
                    if (mViceState.isDragging && mViceState.isHeader) {
                        mKernel.setState(RefreshState.PullDownCanceled)
                    } else if (mViceState.isDragging && mViceState.isFooter) {
                        mKernel.setState(RefreshState.PullUpCanceled)
                    }
                    animationRunnable = null
                    if (abs(mSpinner) >= abs(mSmoothDistance)) {
                        val duration =
                            10 * min(
                                max(px2dp(abs(mSpinner - mSmoothDistance)).toInt(), 30),
                                100
                            )
                        animSpinner(mSmoothDistance, 0, mReboundInterpolator, duration)
                    }
                }
            }
        }
    }

    //</editor-fold>
    /**
     * 执行回弹动画
     * @param endSpinner 目标值
     * @param startDelay 延时参数
     * @param interpolator 加速器
     * @param duration 时长
     * @return ValueAnimator or null
     */
    protected fun animSpinner(
        endSpinner: Int,
        startDelay: Int,
        interpolator: Interpolator?,
        duration: Int
    ): ValueAnimator? {
        if (mSpinner != endSpinner) {
            if (reboundAnimator != null) {
                //cancel会触发End调用，可以判断0来确定是否被cancel
                reboundAnimator?.setDuration(0)
                //会触发 cancel 和 end 调用
                reboundAnimator?.cancel()
                reboundAnimator = null
            }
            animationRunnable = null
            reboundAnimator = ValueAnimator.ofInt(mSpinner, endSpinner)
            reboundAnimator?.setDuration(duration.toLong())
            reboundAnimator?.interpolator = interpolator
            reboundAnimator?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animation.duration == 0L) {
                        /*
                         * 2020-3-15 修复
                         * onAnimationEnd 因为 cancel 调用是, 同样触发 onAnimationEnd 导致的各种问题
                         * 在取消之前调用 reboundAnimator.setDuration(0) 来标记动画被取消
                         */
                        return
                    }
                    reboundAnimator = null
                    if (mSpinner == 0 && mState != RefreshState.None && !mState.isOpening && !mState.isDragging) {
                        notifyStateChanged(RefreshState.None)
                    } else if (mState != mViceState) {
                        // 可以帮助在  ViceState 状态模式时，放手执行动画后矫正 mViceState=mState
                        // 用例：
                        // 如 mState=Refreshing 时，用户再向下拖动，setViceState = ReleaseToRefresh
                        // 放手之后，执行动画回弹到 HeaderHeight 处，
                        // 动画结束时 mViceState 会被矫正到 Refreshing，此时与没有向下拖动时一样
                        setViceState(mState)
                    }
                }
            })
            reboundAnimator?.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
                val animValue =
                    animation?.getAnimatedValue() as? Int ?: return@AnimatorUpdateListener
                mKernel.moveSpinner(animValue, false)
            })
            reboundAnimator?.setStartDelay(startDelay.toLong())
            reboundAnimator?.start()
            return reboundAnimator
        }
        return null
    }

    /**
     * 越界回弹动画
     * @param velocity 速度
     */
    protected fun animSpinnerBounce(velocity: Float) {
        if (reboundAnimator == null) {
            if (velocity > 0 && (mState == RefreshState.Refreshing || mState == RefreshState.TwoLevel)) {
                animationRunnable = BounceRunnable(velocity, mHeaderHeight)
            } else if (velocity < 0 && (mState == RefreshState.Loading || (mEnableFooterFollowWhenNoMoreData && mFooterNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(
                    mEnableLoadMore
                ))
                        || (mEnableAutoLoadMore && !mFooterNoMoreData && isEnableRefreshOrLoadMore(
                    mEnableLoadMore
                ) && mState != RefreshState.Refreshing))
            ) {
                animationRunnable = BounceRunnable(velocity, -mFooterHeight)
            } else if (mSpinner == 0 && mEnableOverScrollBounce) {
                animationRunnable = BounceRunnable(velocity, 0)
            }
        }
    }

    /**
     * 手势拖动结束
     * 开始执行回弹动画
     */
    protected fun overSpinner() {
        if (mState == RefreshState.TwoLevel) {
            val thisView: View = this
            if (mCurrentVelocity > -1000 && mSpinner > thisView.getHeight() / 2) {
                val animator = mKernel.animSpinner(thisView.getHeight())
                animator?.setDuration(mFloorDuration.toLong())
            } else if (mIsBeingDragged) {
                mKernel.finishTwoLevel()
            }
        } else if (mState == RefreshState.Loading
            || (mEnableFooterFollowWhenNoMoreData && mFooterNoMoreData && mFooterNoMoreDataEffective && mSpinner < 0 && isEnableRefreshOrLoadMore(
                mEnableLoadMore
            ))
        ) {
            if (mSpinner < -mFooterHeight) {
                mKernel.animSpinner(-mFooterHeight)
            } else if (mSpinner > 0) {
                mKernel.animSpinner(0)
            }
        } else if (mState == RefreshState.Refreshing) {
            if (mSpinner > mHeaderHeight) {
                mKernel.animSpinner(mHeaderHeight)
            } else if (mSpinner < 0) {
                mKernel.animSpinner(0)
            }
        } else if (mState == RefreshState.PullDownToRefresh) {
            mKernel.setState(RefreshState.PullDownCanceled)
        } else if (mState == RefreshState.PullUpToLoad) {
            mKernel.setState(RefreshState.PullUpCanceled)
        } else if (mState == RefreshState.ReleaseToRefresh) {
            mKernel.setState(RefreshState.Refreshing)
        } else if (mState == RefreshState.ReleaseToLoad) {
            mKernel.setState(RefreshState.Loading)
        } else if (mState == RefreshState.ReleaseToTwoLevel) {
            mKernel.setState(RefreshState.TwoLevelReleased)
        } else if (mState == RefreshState.RefreshReleased) {
            if (reboundAnimator == null) {
                mKernel.animSpinner(mHeaderHeight)
            }
        } else if (mState == RefreshState.LoadReleased) {
            if (reboundAnimator == null) {
                mKernel.animSpinner(-mFooterHeight)
            }
        } else if (mState == RefreshState.LoadFinish) {
            /*
             * 2020-5-26 修复 finishLoadMore 中途
             * 拖拽导致 状态重置 最终导致 显示 NoMoreData Footer 菊花却任然在转的情况
             * overSpinner 时 LoadFinish 状态无任何操作即可
             */
            Log.d("SmartRefreshLayout", "overSpinner 时 LoadFinish 状态无任何操作即可")
        } else if (mSpinner != 0) {
            mKernel.animSpinner(0)
        }
    }

    /**
     * 黏性移动 spinner
     * 阻尼计算，根据原始华东距离，计算出阻尼后的距离
     * @param spinner 偏移量
     */
    protected fun moveSpinnerInfinitely(spinner: Float) {
        var spinner = spinner
        val thisView: View = this
        if (mNestedInProgress && !mEnableLoadMoreWhenContentNotFull && spinner < 0) {
            if (mRefreshContent?.canLoadMore()==false) {
                /*
                 * 2019-1-22 修复 嵌套滚动模式下 mEnableLoadMoreWhenContentNotFull=false 无效的bug
                 */
                spinner = 0f
            }
        }
        /*
         * 如果彩蛋影响了您的APP，可以通过以下三种方法关闭
         *
         * 1.全局关闭（推荐）
         *         SmartRefreshLayout.setDefaultRefreshInitializer(new DefaultRefreshInitializer() {
         *             @Override
         *             public void initialize(@NonNull Context context, @NonNull RefreshLayout layout) {
         *                 layout.getLayout().setTag("close egg");
         *             }
         *         });
         *
         * 2.XML关闭
         *          <com.kernelflux.aether.ui.widget.refresh.SmartRefreshLayout
         *              android:layout_width="match_parent"
         *              android:layout_height="match_parent"
         *              android:tag="close egg"/>
         *
         * 3.修改源码
         *          源码引用，然后删掉下面4行的代码
         */
        if (spinner > mScreenHeightPixels * 5 &&
            (thisView.tag == null && thisView.getTag(R.id.srl_tag) == null) &&
            mLastTouchY < mScreenHeightPixels / 6f &&
            mLastTouchX < mScreenHeightPixels / 16f
        ) {
            val egg = "不要再拉了，臣妾做不到啊！"
            Toast.makeText(thisView.context, egg, Toast.LENGTH_SHORT).show()
            thisView.setTag(R.id.srl_tag, egg)
        }
        if (mState == RefreshState.TwoLevel && spinner > 0) {
            mKernel.moveSpinner(min(spinner.toInt(), thisView.measuredHeight), true)
        } else if (mState == RefreshState.Refreshing && spinner >= 0) {
            if (spinner < mHeaderHeight) {
                mKernel.moveSpinner(spinner.toInt(), true)
            } else {
                val maxDragHeight =
                    if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate
                val M = (maxDragHeight - mHeaderHeight).toDouble()
                val H = (max(
                    mScreenHeightPixels * 4 / 3,
                    thisView.getHeight()
                ) - mHeaderHeight).toDouble()
                val x = max(0f, (spinner - mHeaderHeight) * mDragRate).toDouble()
                val y = min(
                    M * (1 - 100.0.pow(-x / (if (H == 0.0) 1.0 else H))),
                    x
                ) // 公式 y = M(1-100^(-x/H))
                mKernel.moveSpinner(y.toInt() + mHeaderHeight, true)
            }
        } else if (spinner < 0 && (mState == RefreshState.Loading || (mEnableFooterFollowWhenNoMoreData && mFooterNoMoreData && mFooterNoMoreDataEffective && isEnableRefreshOrLoadMore(
                mEnableLoadMore
            ))
                    || (mEnableAutoLoadMore && !mFooterNoMoreData && isEnableRefreshOrLoadMore(
                mEnableLoadMore
            )))
        ) {
            if (spinner > -mFooterHeight) {
                mKernel.moveSpinner(spinner.toInt(), true)
            } else {
                val maxDragHeight =
                    if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
                val M = (maxDragHeight - mFooterHeight).toDouble()
                val H = (max(
                    mScreenHeightPixels * 4 / 3,
                    thisView.height
                ) - mFooterHeight).toDouble()
                val x = -min(0f, (spinner + mFooterHeight) * mDragRate).toDouble()
                val y = -min(
                    M * (1 - 100.0.pow(-x / (if (H == 0.0) 1.0 else H))),
                    x
                ) // 公式 y = M(1-100^(-x/H))
                mKernel.moveSpinner(y.toInt() - mFooterHeight, true)
            }
        } else if (spinner >= 0) {
            val M =
                (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toDouble()
            val H = max(mScreenHeightPixels / 2, thisView.height).toDouble()
            val x = max(0f, spinner * mDragRate).toDouble()
            val y = min(
                M * (1 - 100.0.pow(-x / (if (H == 0.0) 1.0 else H))),
                x
            ) // 公式 y = M(1-100^(-x/H))
            mKernel.moveSpinner(y.toInt(), true)
        } else {
            val M =
                (if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate).toDouble()
            val H = max(mScreenHeightPixels / 2, thisView.height).toDouble()
            val x = -min(0f, spinner * mDragRate).toDouble()
            val y = -min(
                M * (1 - 100.0.pow(-x / (if (H == 0.0) 1.0 else H))),
                x
            ) // 公式 y = M(1-100^(-x/H))
            mKernel.moveSpinner(y.toInt(), true)
        }
        if (mEnableAutoLoadMore && !mFooterNoMoreData && isEnableRefreshOrLoadMore(
                mEnableLoadMore
            ) && spinner < 0 && mState != RefreshState.Refreshing && mState != RefreshState.Loading && mState != RefreshState.LoadFinish
        ) {
            if (mDisableContentWhenLoading) {
                animationRunnable = null
                mKernel.animSpinner(-mFooterHeight)
            }
            setStateDirectLoading(false)
            /*
             * 自动加载模式时，延迟触发 onLoadMore ，mReboundDuration 保证动画能顺利执行
             */
            mHandler?.postDelayed({
                if (mLoadMoreListener != null) {
                    mLoadMoreListener?.onLoadMore(this@SmartRefreshLayout)
                } else if (mOnMultiListener == null) {
                    finishLoadMore(2000) //如果没有任何加载监听器，两秒之后自动关闭
                }
                val listener: OnLoadMoreListener? = mOnMultiListener
                listener?.onLoadMore(this@SmartRefreshLayout)
            }, mReboundDuration.toLong())
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        val thisView: View = this
        return LayoutParams(thisView.context, attrs)
    }

    class LayoutParams : MarginLayoutParams {
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            context.withStyledAttributes(attrs, R.styleable.SmartRefreshLayout_Layout) {
                backgroundColor = getColor(
                    R.styleable.SmartRefreshLayout_Layout_layout_srlBackgroundColor,
                    backgroundColor
                )
                if (hasValue(R.styleable.SmartRefreshLayout_Layout_layout_srlSpinnerStyle)) {
                    spinnerStyle = SpinnerStyle.values[getInt(
                        R.styleable.SmartRefreshLayout_Layout_layout_srlSpinnerStyle,
                        SpinnerStyle.Translate.ordinal
                    )]
                }
            }
        }

        constructor(width: Int, height: Int) : super(width, height)

        var backgroundColor: Int = 0
        var spinnerStyle: SpinnerStyle? = null
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedParent.nestedScrollAxes
    }

    override fun onStartNestedScroll(
        child: View,
        target: View,
        nestedScrollAxes: Int
    ): Boolean {
        val thisView: View = this
        var accepted =
            thisView.isEnabled && isNestedScrollingEnabled && (nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
        accepted = accepted && (mEnableOverScrollDrag || mEnableRefresh || mEnableLoadMore)
        return accepted
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedParent.onNestedScrollAccepted(child, target, axes)
        // Dispatch up to the nested parent
        mNestedChild.startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)

        mTotalUnconsumed = mSpinner
        mNestedInProgress = true

        interceptAnimatorByAction(MotionEvent.ACTION_DOWN)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        var consumedY = 0

        // dy * mTotalUnconsumed > 0 表示 mSpinner 已经拉出来，现在正要往回推
        // mTotalUnconsumed 将要减去 dy 的距离 再计算新的 mSpinner
        if (dy * mTotalUnconsumed > 0) {
            if (abs(dy) > abs(mTotalUnconsumed)) {
                consumedY = mTotalUnconsumed
                mTotalUnconsumed = 0
            } else {
                consumedY = dy
                mTotalUnconsumed -= dy
            }
            moveSpinnerInfinitely(mTotalUnconsumed.toFloat())
        } else if (dy > 0 && mFooterLocked) {
            consumedY = dy
            mTotalUnconsumed -= dy
            moveSpinnerInfinitely(mTotalUnconsumed.toFloat())
        }

        // Now let our nested parent consume the leftovers
        mNestedChild.dispatchNestedPreScroll(dx, dy - consumedY, consumed, null)
        consumed[1] += consumedY
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        // Dispatch up to the nested parent first
        val scrolled = mNestedChild.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            mParentOffsetInWindow
        )

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        val dy = dyUnconsumed + mParentOffsetInWindow[1]
        if ((dy < 0 && (mEnableRefresh || mEnableOverScrollDrag) && (mTotalUnconsumed != 0 || mScrollBoundaryDecider == null ||
                    mScrollBoundaryDecider!!.canRefresh(
                mRefreshContent!!.getView()
            )))
            || (dy > 0 && (mEnableLoadMore || mEnableOverScrollDrag) && (mTotalUnconsumed != 0 || mScrollBoundaryDecider == null || mScrollBoundaryDecider!!.canLoadMore(
                mRefreshContent!!.getView()
            )))
        ) {
            if (mViceState == RefreshState.None || mViceState.isOpening) {
                /*
                 * 嵌套下拉或者上拉时，如果状态还是原始，需要更新到对应的状态
                 * mViceState.isOpening 时，主要修改的也是 mViceState 本身，而 mState 一直都是 isOpening
                 */
                mKernel.setState(if (dy > 0) RefreshState.PullUpToLoad else RefreshState.PullDownToRefresh)
                if (!scrolled) {
                    val thisView: View = this
                    val parent = thisView.parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            moveSpinnerInfinitely(dy.let { mTotalUnconsumed -= it; mTotalUnconsumed }.toFloat())
        }

        if (mFooterLocked && dyConsumed < 0) {
            mFooterLocked = false //内容向下滚动时 解锁Footer 的锁定
        }
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return (mFooterLocked && velocityY > 0) || startFlingIfNeed(-velocityY) || mNestedChild.dispatchNestedPreFling(
            velocityX,
            velocityY
        )
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return mNestedChild.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun onStopNestedScroll(target: View) {
        mNestedParent.onStopNestedScroll(target)
        mNestedInProgress = false
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        mTotalUnconsumed = 0
        overSpinner()
        // Dispatch up our nested parent
        mNestedChild.stopNestedScroll()
    }

    //</editor-fold>
    //<editor-fold desc="NestedScrollingChild">
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mEnableNestedScrolling = enabled
        //        mManualNestedScrolling = true;
        mNestedChild.setNestedScrollingEnabled(enabled)
    }

    override fun isNestedScrollingEnabled(): Boolean {
        /*
         * && 后面的判断是为了解决 https://github.com/scwang90/SmartRefreshLayout/issues/961 问题
         */
        return mEnableNestedScrolling && (mEnableOverScrollDrag || mEnableRefresh || mEnableLoadMore)
        //        return mNestedChild.isNestedScrollingEnabled();
    }

    /**
     * Set the Header's height.
     * 设置 Header 高度
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return RefreshLayout
     */
    override fun setHeaderHeight(dp: Float): RefreshLayout {
        return setHeaderHeightPx(dp2px(dp))
    }

    /**
     * 设置 Header 高度
     * @param px 像素
     * @return RefreshLayout
     */
    override fun setHeaderHeightPx(px: Int): RefreshLayout {
        if (px == mHeaderHeight) {
            return this
        }
        if (mHeaderHeightStatus.canReplaceWith(DimensionStatus.CodeExact)) {
            mHeaderHeight = px
            if (mRefreshHeader != null && mAttachedToWindow && mHeaderHeightStatus.notified) {
                val style: SpinnerStyle = mRefreshHeader!!.getSpinnerStyle()
                if (style !== SpinnerStyle.MatchLayout && !style.scale) {
                    /*
                     * 兼容 MotionLayout 2019-6-18
                     * 在 MotionLayout 内部 requestLayout 无效
                     * 该用 直接调用 layout 方式
                     * https://github.com/scwang90/SmartRefreshLayout/issues/944
                     */
                    val headerView: View = mRefreshHeader!!.getView()
                    val lp = headerView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val widthSpec = MeasureSpec.makeMeasureSpec(
                        headerView.measuredWidth,
                        MeasureSpec.EXACTLY
                    )
                    headerView.measure(
                        widthSpec,
                        MeasureSpec.makeMeasureSpec(
                            max(
                                mHeaderHeight - mlp.bottomMargin - mlp.topMargin,
                                0
                            ), MeasureSpec.EXACTLY
                        )
                    )
                    val left = mlp.leftMargin
                    val top =
                        mlp.topMargin + mHeaderInsetStart - (if (style === SpinnerStyle.Translate) mHeaderHeight else 0)
                    headerView.layout(
                        left,
                        top,
                        left + headerView.getMeasuredWidth(),
                        top + headerView.getMeasuredHeight()
                    )
                }
                val maxDragHeight =
                    (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
                mHeaderHeightStatus = DimensionStatus.CodeExact
                mRefreshHeader!!.onInitialized(mKernel, mHeaderHeight, maxDragHeight)
            } else {
                mHeaderHeightStatus = DimensionStatus.CodeExactUnNotify
            }
        }
        return this
    }

    /**
     * Set the Footer's height.
     * 设置 Footer 的高度
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return RefreshLayout
     */
    override fun setFooterHeight(dp: Float): RefreshLayout {
        return setFooterHeightPx(dp2px(dp))
    }

    /**
     * 设置 Footer 高度
     * @param px 像素
     * @return RefreshLayout
     */
    override fun setFooterHeightPx(px: Int): RefreshLayout {
        if (px == mFooterHeight) {
            return this
        }
        if (mFooterHeightStatus.canReplaceWith(DimensionStatus.CodeExact)) {
            mFooterHeight = px
            if (mRefreshFooter != null && mAttachedToWindow && mFooterHeightStatus.notified) {
                val style: SpinnerStyle = mRefreshFooter!!.getSpinnerStyle()
                if (style !== SpinnerStyle.MatchLayout && !style.scale) {
                    /*
                     * 兼容 MotionLayout 2019-6-18
                     * 在 MotionLayout 内部 requestLayout 无效
                     * 该用 直接调用 layout 方式
                     * https://github.com/scwang90/SmartRefreshLayout/issues/944
                     */
//                  mRefreshFooter.getView().requestLayout();
                    val thisView: View = this
                    val footerView: View = mRefreshFooter!!.getView()
                    val lp = footerView.layoutParams
                    val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                    val widthSpec = MeasureSpec.makeMeasureSpec(
                        footerView.measuredWidth,
                        MeasureSpec.EXACTLY
                    )
                    footerView.measure(
                        widthSpec,
                        MeasureSpec.makeMeasureSpec(
                            max(
                                mFooterHeight - mlp.bottomMargin - mlp.topMargin,
                                0
                            ), MeasureSpec.EXACTLY
                        )
                    )
                    val left = mlp.leftMargin
                    val top =
                        mlp.topMargin + thisView.measuredHeight - mFooterInsetStart - (if (style !== SpinnerStyle.Translate) mFooterHeight else 0)
                    footerView.layout(
                        left,
                        top,
                        left + footerView.measuredWidth,
                        top + footerView.getMeasuredHeight()
                    )
                }
                val maxDragHeight =
                    if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
                mFooterHeightStatus = DimensionStatus.CodeExact
                mRefreshFooter!!.onInitialized(mKernel, mFooterHeight, maxDragHeight.toInt())
            } else {
                mFooterHeightStatus = DimensionStatus.CodeExactUnNotify
            }
        }
        return this
    }

    /**
     * Set the Header's start offset（see srlHeaderInsetStart in the RepastPracticeActivity XML in demo-app for the practical application）.
     * 设置 Header 的起始偏移量（使用方法参考 demo-app 中的 RepastPracticeActivity xml 中的 srlHeaderInsetStart）
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return RefreshLayout
     */
    override fun setHeaderInsetStart(dp: Float): RefreshLayout {
        mHeaderInsetStart = dp2px(dp)
        return this
    }

    /**
     * Set the Header's start offset（see srlHeaderInsetStart in the RepastPracticeActivity XML in demo-app for the practical application）.
     * 设置 Header 起始偏移量（使用方法参考 demo-app 中的 RepastPracticeActivity xml 中的 srlHeaderInsetStart）
     * @param px 像素
     * @return RefreshLayout
     */
    override fun setHeaderInsetStartPx(px: Int): RefreshLayout {
        mHeaderInsetStart = px
        return this
    }

    /**
     * Set the Footer's start offset.
     * 设置 Footer 起始偏移量（用户和 setHeaderInsetStart 一样）
     * @see RefreshLayout.setHeaderInsetStart
     * @param dp Density-independent Pixels 虚拟像素（px需要调用px2dp转换）
     * @return RefreshLayout
     */
    override fun setFooterInsetStart(dp: Float): RefreshLayout {
        mFooterInsetStart = dp2px(dp)
        return this
    }

    /**
     * Set the Footer's start offset.
     * 设置 Footer 起始偏移量（用处和 setHeaderInsetStartPx 一样）
     * @param px 像素
     * @return RefreshLayout
     */
    override fun setFooterInsetStartPx(px: Int): RefreshLayout {
        mFooterInsetStart = px
        return this
    }

    /**
     * Set the damping effect.
     * 显示拖动高度/真实拖动高度 比率（默认0.5，阻尼效果）
     * @param rate ratio = (The drag height of the view)/(The actual drag height of the finger)
     * 比率 = 视图拖动高度 / 手指拖动高度
     * @return RefreshLayout
     */
    override fun setDragRate(rate: Float): RefreshLayout {
        this.mDragRate = rate
        return this
    }

    /**
     * Set the ratio of the maximum height to drag header.
     * 设置下拉最大高度和Header高度的比率（将会影响可以下拉的最大高度）
     * @param rate ratio = (the maximum height to drag header)/(the height of header)
     * 比率 = 下拉最大高度 / Header的高度
     * @return RefreshLayout
     */
    override fun setHeaderMaxDragRate(rate: Float): RefreshLayout {
        this.mHeaderMaxDragRate = rate
        if (mRefreshHeader != null && mAttachedToWindow) {
            val maxDragHeight =
                (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
            mRefreshHeader?.onInitialized(mKernel, mHeaderHeight, maxDragHeight)
        } else {
            mHeaderHeightStatus = mHeaderHeightStatus.unNotify()
        }
        return this
    }

    /**
     * Set the ratio of the maximum height to drag footer.
     * 设置上拉最大高度和Footer高度的比率（将会影响可以上拉的最大高度）
     * @param rate ratio = (the maximum height to drag footer)/(the height of footer)
     * 比率 = 下拉最大高度 / Footer的高度
     * @return RefreshLayout
     */
    override fun setFooterMaxDragRate(rate: Float): RefreshLayout {
        this.mFooterMaxDragRate = rate
        if (mRefreshFooter != null && mAttachedToWindow) {
            val maxDragHeight =
                if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate
            mRefreshFooter?.onInitialized(mKernel, mFooterHeight, maxDragHeight.toInt())
        } else {
            mFooterHeightStatus = mFooterHeightStatus.unNotify()
        }
        return this
    }

    /**
     * Set the ratio at which the refresh is triggered.
     * 设置 触发刷新距离 与 HeaderHeight 的比率
     * @param rate 触发刷新距离 与 HeaderHeight 的比率
     * @return RefreshLayout
     */
    override fun setHeaderTriggerRate(rate: Float): RefreshLayout {
        this.mHeaderTriggerRate = rate
        return this
    }

    /**
     * Set the ratio at which the load more is triggered.
     * 设置 触发加载距离 与 FooterHeight 的比率
     * @param rate 触发加载距离 与 FooterHeight 的比率
     * @return RefreshLayout
     */
    override fun setFooterTriggerRate(rate: Float): RefreshLayout {
        this.mFooterTriggerRate = rate
        return this
    }

    /**
     * Set the rebound interpolator.
     * 设置回弹显示插值器 [放手时回弹动画,结束时收缩动画]
     * @param interpolator 动画插值器
     * @return RefreshLayout
     */
    override fun setReboundInterpolator(interpolator: Interpolator): RefreshLayout {
        this.mReboundInterpolator = interpolator
        return this
    }

    /**
     * Set the duration of the rebound animation.
     * 设置回弹动画时长 [放手时回弹动画,结束时收缩动画]
     * @param duration 时长
     * @return RefreshLayout
     */
    override fun setReboundDuration(duration: Int): RefreshLayout {
        this.mReboundDuration = duration
        return this
    }

    /**
     * Set whether to enable pull-up loading more (enabled by default).
     * 设置是否启用上拉加载更多（默认启用）
     * 注意：本方法仅仅支持初始化的时候调用，如过没有数据的时候，需要关闭请使用: finishLoadMoreWithNoMoreData 或者 finishRefreshWithNoMoreData
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableLoadMore(enabled: Boolean): RefreshLayout {
        this.mManualLoadMore = true
        this.mEnableLoadMore = enabled
        return this
    }

    /**
     * 是否启用下拉刷新（默认启用）
     * @param enabled 是否启用
     * @return SmartRefreshLayout
     */
    override fun setEnableRefresh(enabled: Boolean): RefreshLayout {
        this.mEnableRefresh = enabled
        return this
    }

    /**
     * Whether to enable pull-down refresh (enabled by default).
     * 是否启用下拉刷新（默认启用）
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableHeaderTranslationContent(enabled: Boolean): RefreshLayout {
        this.mEnableHeaderTranslationContent = enabled
        this.mManualHeaderTranslationContent = true
        return this
    }

    /**
     * Set whether to pull up the content while pulling up the header.
     * 设置是否启在上拉 Footer 的同时上拉内容
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableFooterTranslationContent(enabled: Boolean): RefreshLayout {
        this.mEnableFooterTranslationContent = enabled
        this.mManualFooterTranslationContent = true
        return this
    }

    /**
     * Sets whether to listen for the list to trigger a load event when scrolling to the bottom (default true).
     * 设置是否监听列表在滚动到底部时触发加载事件（默认true）
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableAutoLoadMore(enabled: Boolean): RefreshLayout {
        this.mEnableAutoLoadMore = enabled
        return this
    }

    /**
     * Set whether to enable cross-border rebound function.
     * 设置是否启用越界回弹
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableOverScrollBounce(enabled: Boolean): RefreshLayout {
        this.mEnableOverScrollBounce = enabled
        return this
    }

    /**
     * Set whether to enable the pure scroll mode.
     * 设置是否开启纯滚动模式
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnablePureScrollMode(enabled: Boolean): RefreshLayout {
        this.mEnablePureScrollMode = enabled
        return this
    }

    /**
     * Set whether to scroll the content to display new data after loading more complete.
     * 设置是否在加载更多完成之后滚动内容显示新数据
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableScrollContentWhenLoaded(enabled: Boolean): RefreshLayout {
        this.mEnableScrollContentWhenLoaded = enabled
        return this
    }

    /**
     * Set whether to scroll the content to display new data after the refresh is complete.
     * 是否在刷新完成之后滚动内容显示新数据
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableScrollContentWhenRefreshed(enabled: Boolean): RefreshLayout {
        this.mEnableScrollContentWhenRefreshed = enabled
        return this
    }

    /**
     * Set whether to pull up and load more when the content is not full of one page.
     * 设置在内容不满一页的时候，是否可以上拉加载更多
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableLoadMoreWhenContentNotFull(enabled: Boolean): RefreshLayout {
        this.mEnableLoadMoreWhenContentNotFull = enabled
        if (mRefreshContent != null) {
            mRefreshContent!!.setEnableLoadMoreWhenContentNotFull(enabled)
        }
        return this
    }

    /**
     * Set whether to enable cross-border drag (imitation iphone effect).
     * 设置是否启用越界拖动（仿苹果效果）
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableOverScrollDrag(enabled: Boolean): RefreshLayout {
        this.mEnableOverScrollDrag = enabled
        return this
    }

    /**
     * Set whether or not Footer follows the content after there is no more data.
     * 设置是否在没有更多数据之后 Footer 跟随内容
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableFooterFollowWhenNoMoreData(enabled: Boolean): RefreshLayout {
        this.mEnableFooterFollowWhenNoMoreData = enabled
        return this
    }

    /**
     * Set whether to clip header when the Header is in the FixedBehind state.
     * 设置是否在当 Header 处于 FixedBehind 状态的时候剪裁遮挡 Header
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableClipHeaderWhenFixedBehind(enabled: Boolean): RefreshLayout {
        this.mEnableClipHeaderWhenFixedBehind = enabled
        return this
    }

    /**
     * Set whether to clip footer when the Footer is in the FixedBehind state.
     * 设置是否在当 Footer 处于 FixedBehind 状态的时候剪裁遮挡 Footer
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableClipFooterWhenFixedBehind(enabled: Boolean): RefreshLayout {
        this.mEnableClipFooterWhenFixedBehind = enabled
        return this
    }

    /**
     * Setting whether nesting scrolling is enabled (default off + smart on).
     * 设置是会否启用嵌套滚动功能（默认关闭+智能开启）
     * @param enabled 是否启用
     * @return RefreshLayout
     */
    override fun setEnableNestedScroll(enabled: Boolean): RefreshLayout {
        setNestedScrollingEnabled(enabled)
        return this
    }

    /**
     * 设置固定在 Header 下方的视图Id，可以在 Footer 上下滚动的时候保持不跟谁滚动
     * @param id 固定在头部的视图Id
     * @return RefreshLayout
     */
    override fun setFixedHeaderViewId(id: Int): RefreshLayout {
        this.mFixedHeaderViewId = id
        return this
    }

    /**
     * 设置固定在 Footer 上方的视图Id，可以在 Header 上下滚动的时候保持不跟谁滚动
     * @param id 固定在底部的视图Id
     * @return RefreshLayout
     */
    override fun setFixedFooterViewId(id: Int): RefreshLayout {
        this.mFixedFooterViewId = id
        return this
    }

    /**
     * 设置在 Header 上下滚动时，需要跟随滚动的视图Id，默认整个内容视图
     * @param id 固定在头部的视图Id
     * @return RefreshLayout
     */
    override fun setHeaderTranslationViewId(id: Int): RefreshLayout {
        this.mHeaderTranslationViewId = id
        return this
    }

    /**
     * 设置在 Footer 上下滚动时，需要跟随滚动的视图Id，默认整个内容视图
     * @param id 固定在头部的视图Id
     * @return RefreshLayout
     */
    override fun setFooterTranslationViewId(id: Int): RefreshLayout {
        this.mFooterTranslationViewId = id
        return this
    }

    //    /**
    //     * Sets whether to enable pure nested scrolling mode
    //     * Smart scrolling supports both [nested scrolling] and [traditional scrolling] modes
    //     * With nested scrolling enabled, traditional mode also works when necessary
    //     * However, sometimes interference and conflict can occur. If you find this conflict, you can try to turn on [pure nested scrolling] mode and [traditional mode] off
    //     * 设置是否开启【纯嵌套滚动】模式
    //     * Smart 的滚动支持 【嵌套滚动】 + 【传统滚动】 两种模式
    //     * 在开启 【嵌套滚动】 的情况下，【传统模式】也会在必要的时候发挥作用
    //     * 但是有时候也会发生干扰和冲突，如果您发现了这个冲突，可以尝试开启 【纯嵌套滚动】模式，【传统模式】关闭
    //     * @param enabled 是否启用
    //     * @return RefreshLayout
    //     */
    //    @Override
    //    public RefreshLayout setEnableNestedScrollOnly(boolean enabled) {
    //        if (enabled && !mNestedChild.isNestedScrollingEnabled()) {
    //            mNestedChild.setNestedScrollingEnabled(true);
    //        }
    //        mEnableNestedScrollingOnly = enabled;
    //        return this;
    //    }
    /**
     * Set whether to enable the action content view when refreshing.
     * 设置是否开启在刷新时候禁止操作内容视图
     * @param disable 是否禁止
     * @return RefreshLayout
     */
    override fun setDisableContentWhenRefresh(disable: Boolean): RefreshLayout {
        this.mDisableContentWhenRefresh = disable
        return this
    }

    /**
     * Set whether to enable the action content view when loading.
     * 设置是否开启在加载时候禁止操作内容视图
     * @param disable 是否禁止
     * @return RefreshLayout
     */
    override fun setDisableContentWhenLoading(disable: Boolean): RefreshLayout {
        this.mDisableContentWhenLoading = disable
        return this
    }

    /**
     * Set the header of RefreshLayout.
     * 设置指定的 Header
     * @param header RefreshHeader 刷新头
     * @return RefreshLayout
     */
    override fun setRefreshHeader(header: RefreshHeader): RefreshLayout {
        return setRefreshHeader(header, 0, 0)
    }

    /**
     * Set the header of RefreshLayout.
     * 设置指定的 Header
     * @param header RefreshHeader 刷新头
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     * 宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     * 高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    override fun setRefreshHeader(
        header: RefreshHeader,
        width: Int,
        height: Int
    ): RefreshLayout {
        var width = width
        var height = height
        if (mRefreshHeader != null) {
            super.removeView(mRefreshHeader!!.getView())
        }
        this.mRefreshHeader = header
        this.mHeaderBackgroundColor = 0
        this.mHeaderNeedTouchEventWhenRefreshing = false
        this.mHeaderHeightStatus =
            DimensionStatus.DefaultUnNotify //2020-5-23 修复动态切换时，不能及时测量新的高度
        /*
         * 2020-3-16 修复 header 中自带 LayoutParams 丢失问题
         */
        width = if (width == 0) ViewGroup.LayoutParams.MATCH_PARENT else width
        height = if (height == 0) ViewGroup.LayoutParams.WRAP_CONTENT else height
        var lp = LayoutParams(width, height)
        val olp: Any? = header.getView().layoutParams
        if (olp is LayoutParams) {
            lp = olp
        }
        if (mRefreshHeader!!.getSpinnerStyle().front) {
            val thisGroup: ViewGroup = this
            super.addView(mRefreshHeader!!.getView(), thisGroup.size, lp)
        } else {
            super.addView(mRefreshHeader!!.getView(), 0, lp)
        }
        if (mPrimaryColors != null && mRefreshHeader != null) {
            mRefreshHeader!!.setPrimaryColors(*mPrimaryColors!!)
        }
        return this
    }

    /**
     * Set the footer of RefreshLayout.
     * 设置指定的 Footer
     * @param footer RefreshFooter 刷新尾巴
     * @return RefreshLayout
     */
    override fun setRefreshFooter(footer: RefreshFooter): RefreshLayout {
        return setRefreshFooter(footer, 0, 0)
    }

    /**
     * Set the footer of RefreshLayout.
     * 设置指定的 Footer
     * @param footer RefreshFooter 刷新尾巴
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     * 宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     * 高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    override fun setRefreshFooter(
        footer: RefreshFooter,
        width: Int,
        height: Int
    ): RefreshLayout {
        var width = width
        var height = height
        if (mRefreshFooter != null) {
            super.removeView(mRefreshFooter!!.getView())
        }
        this.mRefreshFooter = footer
        this.mFooterLocked = false
        this.mFooterBackgroundColor = 0
        this.mFooterNoMoreDataEffective = false
        this.mFooterNeedTouchEventWhenLoading = false
        this.mFooterHeightStatus =
            DimensionStatus.DefaultUnNotify //2020-5-23 修复动态切换时，不能及时测量新的高度
        this.mEnableLoadMore = !mManualLoadMore || mEnableLoadMore
        /*
         * 2020-3-16 修复 header 中自带 LayoutParams 丢失问题
         */
        width = if (width == 0) ViewGroup.LayoutParams.MATCH_PARENT else width
        height = if (height == 0) ViewGroup.LayoutParams.WRAP_CONTENT else height
        var lp = LayoutParams(width, height)
        val olp: Any? = footer.getView().getLayoutParams()
        if (olp is LayoutParams) {
            lp = olp
        }
        if (mRefreshFooter!!.getSpinnerStyle().front) {
            val thisGroup: ViewGroup = this
            super.addView(mRefreshFooter!!.getView(), thisGroup.size, lp)
        } else {
            super.addView(mRefreshFooter!!.getView(), 0, lp)
        }
        if (mPrimaryColors != null && mRefreshFooter != null) {
            mRefreshFooter!!.setPrimaryColors(*mPrimaryColors!!)
        }
        return this
    }

    /**
     * Set the content of RefreshLayout（Suitable for non-XML pages, not suitable for replacing empty layouts）。
     * 设置指定的 Content（适用于非XML页面，不适合用替换空布局）
     * @param content View 内容视图
     * @return RefreshLayout
     */
    override fun setRefreshContent(content: View): RefreshLayout {
        return setRefreshContent(content, 0, 0)
    }

    /**
     * Set the content of RefreshLayout（Suitable for non-XML pages, not suitable for replacing empty layouts）.
     * 设置指定的 Content（适用于非XML页面，不适合用替换空布局）
     * @param content View 内容视图
     * @param width the width in px, can use MATCH_PARENT and WRAP_CONTENT.
     * 宽度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @param height the height in px, can use MATCH_PARENT and WRAP_CONTENT.
     * 高度 可以使用 MATCH_PARENT, WRAP_CONTENT
     * @return RefreshLayout
     */
    override fun setRefreshContent(content: View, width: Int, height: Int): RefreshLayout {
        var width = width
        var height = height
        val thisView: View = this
        if (mRefreshContent != null) {
            super.removeView(mRefreshContent!!.getView())
        }
        val thisGroup: ViewGroup = this

        /*
         * 2020-3-16 修复 content 中自带 LayoutParams 丢失问题
         */
        width = if (width == 0) ViewGroup.LayoutParams.MATCH_PARENT else width
        height = if (height == 0) ViewGroup.LayoutParams.MATCH_PARENT else height
        var lp = LayoutParams(width, height)
        val olp: Any = content.getLayoutParams()
        if (olp is LayoutParams) {
            lp = olp
        }

        super.addView(content, thisGroup.size, lp)

        mRefreshContent = RefreshContentWrapper(content)
        if (mAttachedToWindow) {
            val fixedHeaderView = thisView.findViewById<View?>(mFixedHeaderViewId)
            val fixedFooterView = thisView.findViewById<View?>(mFixedFooterViewId)

            mScrollBoundaryDecider?.apply {
                mRefreshContent?.setScrollBoundaryDecider(this)
            }
            mRefreshContent?.setEnableLoadMoreWhenContentNotFull(mEnableLoadMoreWhenContentNotFull)
            mRefreshContent?.setUpComponent(mKernel, fixedHeaderView, fixedFooterView)
        }

        if (mRefreshHeader != null && mRefreshHeader!!.getSpinnerStyle().front) {
            super.bringChildToFront(mRefreshHeader!!.getView())
        }
        if (mRefreshFooter != null && mRefreshFooter!!.getSpinnerStyle().front) {
            super.bringChildToFront(mRefreshFooter!!.getView())
        }
        return this
    }

    /**
     * Get footer of RefreshLayout
     * 获取当前 Footer
     * @return RefreshLayout
     */
    override fun getRefreshFooter(): RefreshFooter? {
        return if (mRefreshFooter is RefreshFooter) mRefreshFooter as RefreshFooter else null
    }

    /**
     * Get header of RefreshLayout
     * 获取当前 Header
     * @return RefreshLayout
     */
    override fun getRefreshHeader(): RefreshHeader? {
        return if (mRefreshHeader is RefreshHeader) mRefreshHeader as RefreshHeader else null
    }

    /**
     * Get the current state of RefreshLayout
     * 获取当前状态
     * @return RefreshLayout
     */
    override fun getState(): RefreshState {
        return mState
    }

    /**
     * Get the ViewGroup of RefreshLayout
     * 获取实体布局视图
     * @return ViewGroup
     */
    override fun getLayout(): ViewGroup {
        return this
    }

    /**
     * Set refresh listener separately.
     * 单独设置刷新监听器
     * @param listener OnRefreshListener 刷新监听器
     * @return RefreshLayout
     */
    override fun setOnRefreshListener(listener: OnRefreshListener?): RefreshLayout {
        this.mRefreshListener = listener
        return this
    }

    /**
     * Set load more listener separately.
     * 单独设置加载监听器
     * @param listener OnLoadMoreListener 加载监听器
     * @return RefreshLayout
     */
    override fun setOnLoadMoreListener(listener: OnLoadMoreListener?): RefreshLayout {
        this.mLoadMoreListener = listener
        this.mEnableLoadMore = mEnableLoadMore || (!mManualLoadMore && listener != null)
        return this
    }

    /**
     * Set refresh and load listeners at the same time.
     * 同时设置刷新和加载监听器
     * @param listener OnRefreshLoadMoreListener 刷新加载监听器
     * @return RefreshLayout
     */
    override fun setOnRefreshLoadMoreListener(listener: OnRefreshLoadMoreListener?): RefreshLayout {
        this.mRefreshListener = listener
        this.mLoadMoreListener = listener
        this.mEnableLoadMore = mEnableLoadMore || (!mManualLoadMore && listener != null)
        return this
    }

    /**
     * Set up a multi-function listener.
     * Recommended [com.kernelflux.aether.ui.widget.refresh.simple.SimpleBoundaryDecider]
     * 设置滚动边界判断器
     * 建议使用 [com.kernelflux.aether.ui.widget.refresh.simple.SimpleBoundaryDecider]
     * @param listener OnMultiListener 多功能监听器
     * @return RefreshLayout
     */
    override fun setOnMultiListener(listener: OnMultiListener?): RefreshLayout {
        this.mOnMultiListener = listener
        return this
    }

    /**
     * Set theme color int (primaryColor and accentColor).
     * 设置主题颜色
     * @param primaryColors ColorInt 主题颜色
     * @return RefreshLayout
     */
    override fun setPrimaryColors(@ColorInt vararg primaryColors: Int): RefreshLayout {
        if (mRefreshHeader != null) {
            mRefreshHeader!!.setPrimaryColors(*primaryColors)
        }
        if (mRefreshFooter != null) {
            mRefreshFooter!!.setPrimaryColors(*primaryColors)
        }
        mPrimaryColors = primaryColors
        return this
    }

    /**
     * Set theme color id (primaryColor and accentColor).
     * 设置主题颜色
     * @param primaryColorId ColorRes 主题颜色ID
     * @return RefreshLayout
     */
    override fun setPrimaryColorsId(@ColorRes vararg primaryColorId: Int): RefreshLayout {
        val thisView: View = this
        val colors = IntArray(primaryColorId.size)
        for (i in primaryColorId.indices) {
            colors[i] = ContextCompat.getColor(thisView.getContext(), primaryColorId[i])
        }
        setPrimaryColors(*colors)
        return this
    }

    /**
     * Set the scroll boundary Decider, Can customize when you can refresh.
     * Recommended [com.kernelflux.aether.ui.widget.refresh.simple.SimpleBoundaryDecider]
     * 设置滚动边界判断器
     * 建议使用 [com.kernelflux.aether.ui.widget.refresh.simple.SimpleBoundaryDecider]
     * @param boundary ScrollBoundaryDecider 判断器
     * @return RefreshLayout
     */
    override fun setScrollBoundaryDecider(boundary: ScrollBoundaryDecider?): RefreshLayout {
        mScrollBoundaryDecider = boundary
        boundary?.apply {
            mRefreshContent?.setScrollBoundaryDecider(this)
        }
        return this
    }

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     * 恢复没有更多数据的原始状态
     * @param noMoreData 是否有更多数据
     * @return RefreshLayout
     */
    override fun setNoMoreData(noMoreData: Boolean): RefreshLayout {
        if (mState == RefreshState.Refreshing && noMoreData) {
            finishRefreshWithNoMoreData()
        } else if (mState == RefreshState.Loading && noMoreData) {
            finishLoadMoreWithNoMoreData()
        } else if (mFooterNoMoreData != noMoreData) {
            mFooterNoMoreData = noMoreData
            if (mRefreshFooter is RefreshFooter) {
                if ((mRefreshFooter as RefreshFooter).setNoMoreData(noMoreData)) {
                    mFooterNoMoreDataEffective = true
                    if (mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mSpinner > 0 && mRefreshFooter!!.getSpinnerStyle() === SpinnerStyle.Translate && isEnableRefreshOrLoadMore(
                            mEnableLoadMore
                        )
                        && isEnableTranslationContent(mEnableRefresh, mRefreshHeader)
                    ) {
                        mRefreshFooter!!.getView().setTranslationY(mSpinner.toFloat())
                    }
                } else {
                    mFooterNoMoreDataEffective = false
                    val msg =
                        "Footer:$mRefreshFooter NoMoreData is not supported.(不支持NoMoreData，请使用[ClassicsFooter]或者[自定义Footer并实现setNoMoreData方法且返回true])"
                    val e: Throwable = RuntimeException(msg)
                    e.printStackTrace()
                }
            }
        }
        return this
    }

    /**
     * Restore the original state after finishLoadMoreWithNoMoreData.
     * 恢复没有更多数据的原始状态
     * @return RefreshLayout
     */
    override fun resetNoMoreData(): RefreshLayout {
        return setNoMoreData(false)
    }

    /**
     * finish refresh.
     * 完成刷新
     * @return RefreshLayout
     */
    override fun finishRefresh(): RefreshLayout {
        return finishRefresh(true)
    }

    /**
     * finish load more.
     * 完成加载
     * @return RefreshLayout
     */
    override fun finishLoadMore(): RefreshLayout {
        return finishLoadMore(true)
    }

    /**
     * finish refresh.
     * 完成刷新
     * @param delayed 开始延时
     * @return RefreshLayout
     */
    override fun finishRefresh(delayed: Int): RefreshLayout {
        return finishRefresh(delayed, true, false)
    }

    /**
     * finish refresh.
     * 完成加载
     * @param success 数据是否成功刷新 （会影响到上次更新时间的改变）
     * @return RefreshLayout
     */
    override fun finishRefresh(success: kotlin.Boolean): RefreshLayout {
        if (success) {
            val passTime = System.currentTimeMillis() - mLastOpenTime
            val delayed = (min(max(0, 300 - passTime.toInt()), 300) shl 16) //保证加载动画有300毫秒的时间
            return finishRefresh(delayed, true, false)
        } else {
            return finishRefresh(0, false, null)
        }
    }

    /**
     * finish refresh.
     * 完成刷新
     *
     * @param delayed 开始延时
     * @param success 数据是否成功刷新 （会影响到上次更新时间的改变）
     * @return RefreshLayout
     */
    override fun finishRefresh(
        delayed: Int,
        success: kotlin.Boolean,
        noMoreData: kotlin.Boolean?
    ): RefreshLayout {
        val more = delayed shr 16 //动画剩余延时
        val delay = delayed shl 16 shr 16 //用户指定延时
        val runnable: Runnable = object : Runnable {
            var count: Int = 0
            override fun run() {
                if (count == 0) {
                    if (mState == RefreshState.None && mViceState == RefreshState.Refreshing) {
                        //autoRefresh 即将执行，但未开始
                        mViceState = RefreshState.None
                    } else if (reboundAnimator != null && mState.isHeader && (mState.isDragging || mState == RefreshState.RefreshReleased)) {
                        //autoRefresh 正在执行，但未结束
                        //mViceState = RefreshState.None;
                        /*
                         * 2020-3-15 BUG修复
                         * https://github.com/scwang90/SmartRefreshLayout/issues/1019
                         * 修复 autoRefresh 因为 cancel 触发 end 回调 导致 偶尔不能关闭问题
                         */
                        reboundAnimator!!.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
                        reboundAnimator!!.cancel() //会触发 cancel 和 end 调用
                        reboundAnimator = null
                        /*
                         * 2020-1-4 BUG修复
                         * https://github.com/scwang90/SmartRefreshLayout/issues/1104
                         * 如果当前状态为 PullDownToRefresh 并且 mSpinner != 0
                         * mKernel.setState(RefreshState.None); 内部会调用 animSpinner(0); 动画关闭
                         * 但是 PullDownToRefresh 具有 isDragging 特性，animSpinner(0); 不会重置 None 状态
                         * 将会导致 PullDownToRefresh 保持，点击列表之后 overSpinner(); 出发刷新
                         */
                        if (mKernel.animSpinner(0) == null) {
                            notifyStateChanged(RefreshState.None)
                        } else {
                            notifyStateChanged(RefreshState.PullDownCanceled)
                        }
                        //                      mKernel.setState(RefreshState.None);
                    } else if (mState == RefreshState.Refreshing) {
                        count++
                        mHandler!!.postDelayed(this, more.toLong())
                        //提前设置 状态为 RefreshFinish 防止 postDelayed 导致 finishRefresh 过后，外部判断 state 还是 Refreshing
                        notifyStateChanged(RefreshState.RefreshFinish)
                        if (noMoreData == false) {
                            setNoMoreData(false) //真正有刷新状态的时候才可以重置 noMoreData
                        }
                    }
                    if (noMoreData == true) {
                        setNoMoreData(true)
                    }
                } else {
                    // 本else 分支的触发是的代码是上面的：count++;mHandler.postDelayed(this, more);
                    var startDelay = 0
                    if (mRefreshHeader != null) {
                        startDelay = mRefreshHeader!!.onFinish(this@SmartRefreshLayout, success)
                        if (mOnMultiListener != null && mRefreshHeader is RefreshHeader) {
                            mOnMultiListener!!.onHeaderFinish(
                                mRefreshHeader as RefreshHeader,
                                success
                            )
                        }
                    }
                    //startDelay < Integer.MAX_VALUE 表示 延时 startDelay 毫秒之后，回弹关闭刷新
                    if (startDelay < Int.Companion.MAX_VALUE) {
                        //如果正在拖动的话，偏移初始点击事件 【两种情况都是结束刷新时，手指还按住屏幕不放手哦】
                        if (mIsBeingDragged || mNestedInProgress) {
                            val time = System.currentTimeMillis()
                            if (mIsBeingDragged) {
                                mTouchY = mLastTouchY
                                mTouchSpinner = 0
                                mIsBeingDragged = false
                                super@SmartRefreshLayout.dispatchTouchEvent(
                                    MotionEvent.obtain(
                                        time,
                                        time,
                                        MotionEvent.ACTION_DOWN,
                                        mLastTouchX,
                                        mLastTouchY + mSpinner - mTouchSlop * 2,
                                        0
                                    )
                                )
                                super@SmartRefreshLayout.dispatchTouchEvent(
                                    MotionEvent.obtain(
                                        time,
                                        time,
                                        MotionEvent.ACTION_MOVE,
                                        mLastTouchX,
                                        mLastTouchY + mSpinner,
                                        0
                                    )
                                )
                            }
                            if (mNestedInProgress) {
                                mTotalUnconsumed = 0
                                super@SmartRefreshLayout.dispatchTouchEvent(
                                    MotionEvent.obtain(
                                        time,
                                        time,
                                        MotionEvent.ACTION_UP,
                                        mLastTouchX,
                                        mLastTouchY,
                                        0
                                    )
                                )
                                mNestedInProgress = false
                                mTouchSpinner = 0
                            }
                        }
                        if (mSpinner > 0) {
                            var updateListener: AnimatorUpdateListener? = null
                            val valueAnimator =
                                animSpinner(
                                    0,
                                    startDelay,
                                    mReboundInterpolator,
                                    mReboundDuration
                                )
                            if (mEnableScrollContentWhenRefreshed) {
                                updateListener =
                                    mRefreshContent!!.scrollContentWhenFinished(mSpinner)
                            }
                            if (valueAnimator != null && updateListener != null) {
                                valueAnimator.addUpdateListener(updateListener)
                            }
                        } else if (mSpinner < 0) {
                            animSpinner(0, startDelay, mReboundInterpolator, mReboundDuration)
                        } else {
                            mKernel.moveSpinner(0, false)
                            //                            resetStatus();
                            mKernel.setState(RefreshState.None)
                        }
                    }
                }
            }
        }
        if (delay > 0) {
            mHandler!!.postDelayed(runnable, delay.toLong())
        } else {
            runnable.run()
        }
        return this
    }

    /**
     * finish load more with no more data.
     * 完成刷新并标记没有更多数据
     * @return RefreshLayout
     */
    override fun finishRefreshWithNoMoreData(): RefreshLayout {
        val passTime = System.currentTimeMillis() - mLastOpenTime
        return finishRefresh((min(max(0, 300 - passTime.toInt()), 300) shl 16), true, true)
    }

    /**
     * finish load more.
     * 完成加载
     * @param delayed 开始延时
     * @return RefreshLayout
     */
    override fun finishLoadMore(delayed: Int): RefreshLayout {
        return finishLoadMore(delayed, true, false)
    }

    /**
     * finish load more.
     * 完成加载
     * @param success 数据是否成功
     * @return RefreshLayout
     */
    override fun finishLoadMore(success: kotlin.Boolean): RefreshLayout {
        val passTime = System.currentTimeMillis() - mLastOpenTime
        return finishLoadMore(
            if (success) (min(max(0, 300 - passTime.toInt()), 300) shl 16) else 0,
            success,
            false
        )
    }

    /**
     * finish load more.
     * 完成加载
     * @param delayed 开始延时
     * @param success 数据是否成功
     * @param noMoreData 是否有更多数据
     * @return RefreshLayout
     */
    override fun finishLoadMore(
        delayed: Int,
        success: kotlin.Boolean,
        noMoreData: kotlin.Boolean
    ): RefreshLayout {
        val more = delayed shr 16 //动画剩余延时
        val delay = delayed shl 16 shr 16 //用户指定延时
        val runnable: Runnable = object : Runnable {
            var count: Int = 0
            override fun run() {
                if (count == 0) {
                    if (mState == RefreshState.None && mViceState == RefreshState.Loading) {
                        //autoLoadMore 即将执行，但未开始
                        mViceState = RefreshState.None
                    } else if (reboundAnimator != null && (mState.isDragging || mState == RefreshState.LoadReleased) && mState.isFooter) {
                        //autoLoadMore 正在执行，但未结束
                        /*
                         * 2020-3-15 BUG修复
                         * https://github.com/scwang90/SmartRefreshLayout/issues/1019
                         * 修复 autoRefresh 因为 cancel 触发 end 回调 导致 偶尔不能关闭问题
                         */
                        reboundAnimator!!.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
                        reboundAnimator!!.cancel() //会触发 cancel 和 end 调用
                        reboundAnimator = null
                        /*
                         * 2020-1-4 BUG修复
                         * https://github.com/scwang90/SmartRefreshLayout/issues/1104
                         * 如果当前状态为 PullDownToRefresh 并且 mSpinner != 0
                         * mKernel.setState(RefreshState.None); 内部会调用 animSpinner(0); 动画关闭
                         * 但是 PullDownToRefresh 具有 isDragging 特性，animSpinner(0); 不会重置 None 状态
                         * 将会导致 PullDownToRefresh 保持，点击列表之后 overSpinner(); 出发刷新
                         */
                        if (mKernel.animSpinner(0) == null) {
                            notifyStateChanged(RefreshState.None)
                        } else {
                            notifyStateChanged(RefreshState.PullUpCanceled)
                        }
                        //mKernel.setState(RefreshState.None);
                    } else if (mState == RefreshState.Loading && mRefreshFooter != null && mRefreshContent != null) {
                        count++
                        mHandler!!.postDelayed(this, more.toLong())
                        //提前设置 状态为 LoadFinish 防止 postDelayed 导致 finishLoadMore 过后，外部判断 state 还是 Loading
                        notifyStateChanged(RefreshState.LoadFinish)
                        return
                    }
                    if (noMoreData) {
                        setNoMoreData(true)
                    }
                } else {
                    val startDelay = mRefreshFooter!!.onFinish(this@SmartRefreshLayout, success)
                    if (mOnMultiListener != null && mRefreshFooter is RefreshFooter) {
                        mOnMultiListener!!.onFooterFinish(
                            mRefreshFooter as RefreshFooter,
                            success
                        )
                    }
                    if (startDelay < Int.Companion.MAX_VALUE) {
                        //计算布局将要移动的偏移量
                        val needHoldFooter =
                            noMoreData && mEnableFooterFollowWhenNoMoreData && mSpinner < 0 && mRefreshContent!!.canLoadMore()
                        val offset =
                            mSpinner - (if (needHoldFooter) max(
                                mSpinner,
                                -mFooterHeight
                            ) else 0)
                        //如果正在拖动的话，偏移初始点击事件
                        if (mIsBeingDragged || mNestedInProgress) {
                            val time = System.currentTimeMillis()
                            if (mIsBeingDragged) {
                                mTouchY = mLastTouchY
                                mTouchSpinner = mSpinner - offset
                                mIsBeingDragged = false
                                val offsetY = if (mEnableFooterTranslationContent) offset else 0
                                super@SmartRefreshLayout.dispatchTouchEvent(
                                    MotionEvent.obtain(
                                        time,
                                        time,
                                        MotionEvent.ACTION_DOWN,
                                        mLastTouchX,
                                        mLastTouchY + offsetY + mTouchSlop * 2,
                                        0
                                    )
                                )
                                super@SmartRefreshLayout.dispatchTouchEvent(
                                    MotionEvent.obtain(
                                        time,
                                        time,
                                        MotionEvent.ACTION_MOVE,
                                        mLastTouchX,
                                        mLastTouchY + offsetY,
                                        0
                                    )
                                )
                            }
                            if (mNestedInProgress) {
                                mTotalUnconsumed = 0
                                super@SmartRefreshLayout.dispatchTouchEvent(
                                    MotionEvent.obtain(
                                        time,
                                        time,
                                        MotionEvent.ACTION_UP,
                                        mLastTouchX,
                                        mLastTouchY,
                                        0
                                    )
                                )
                                mNestedInProgress = false
                                mTouchSpinner = 0
                            }
                        }
                        //准备：偏移并结束状态
                        mHandler?.postDelayed({
                            var updateListener: AnimatorUpdateListener? = null
                            if (mEnableScrollContentWhenLoaded && offset < 0) {
                                updateListener =
                                    mRefreshContent!!.scrollContentWhenFinished(mSpinner)
                                updateListener?.onAnimationUpdate(
                                    ValueAnimator.ofInt(
                                        0,
                                        0
                                    )
                                )
                            }
                            var animator: ValueAnimator? = null //动议动画和动画结束回调
                            val listenerAdapter: AnimatorListenerAdapter =
                                object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        if (animation.getDuration() == 0L) {
                                            return  //0 表示被取消
                                        }
                                        mFooterLocked = false
                                        if (noMoreData) {
                                            setNoMoreData(true)
                                        }
                                        if (mState == RefreshState.LoadFinish) {
                                            notifyStateChanged(RefreshState.None)
                                        }
                                    }
                                }
                            if (mSpinner > 0) { //大于0表示下拉, 这是 Header 可见, Footer 不可见
                                animator = mKernel.animSpinner(0) //关闭 Header 回到原始状态
                            } else if (updateListener != null || mSpinner == 0) { //如果 Header 和 Footer 都不可见 或者内容需要滚动显示新内容
                                if (reboundAnimator != null) {
                                    reboundAnimator!!.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
                                    reboundAnimator!!.cancel() //会触发 cancel 和 end 调用
                                    reboundAnimator = null //取消之前的任何动画
                                }
                                //直接关闭 Header 或者 Header 到原始状态
                                mKernel.moveSpinner(0, false)
                                mKernel.setState(RefreshState.None)
                            } else { //准备按正常逻辑关闭Footer
                                if (noMoreData && mEnableFooterFollowWhenNoMoreData) { //如果需要显示没有更多数据
                                    if (mSpinner >= -mFooterHeight) { //如果 Footer 的位置再可见范围内
                                        notifyStateChanged(RefreshState.None) //直接通知重置状态,不关闭 Footer
                                    } else { //如果 Footer 的位置超出 Footer 显示高度 (这个情况的概率应该很低, 手指故意拖拽 Footer 向上超出原位置时会触发)
                                        animator =
                                            mKernel.animSpinner(-mFooterHeight) //通过动画让 Footer 回到全显示状态位置
                                    }
                                } else {
                                    animator = mKernel.animSpinner(0) //动画正常关闭 Footer
                                }
                            }
                            if (animator != null) {
                                animator.addListener(listenerAdapter) //如果通过动画关闭,绑定动画结束回调
                            } else {
                                //TODO  listenerAdapter.onAnimationEnd(null) //如果没有动画,立即执行结束回调(必须逻辑)
                            }
                        }, (if (mSpinner < 0) startDelay else 0).toLong())
                    }
                }
            }
        }
        if (delay > 0) {
            mHandler?.postDelayed(runnable, delay.toLong())
        } else {
            runnable.run()
        }
        return this
    }

    /**
     * finish load more with no more data.
     * 完成加载并标记没有更多数据
     * @return RefreshLayout
     */
    override fun finishLoadMoreWithNoMoreData(): RefreshLayout {
        val passTime = System.currentTimeMillis() - mLastOpenTime
        return finishLoadMore((min(max(0, 300 - passTime.toInt()), 300) shl 16), true, true)
    }

    /**
     * Close the Header or Footer, can't replace finishRefresh and finishLoadMore.
     * 关闭 Header 或者 Footer
     * 注意：
     * 1.closeHeaderOrFooter 任何时候任何状态都能关闭  header 和 footer
     * 2.finishRefresh 和 finishLoadMore 只能在 刷新 或者 加载 的时候关闭
     * @return RefreshLayout
     */
    override fun closeHeaderOrFooter(): RefreshLayout {
        if (mState == RefreshState.None && (mViceState == RefreshState.Refreshing || mViceState == RefreshState.Loading)) {
            //autoRefresh autoLoadMore 即将执行，但未开始
            mViceState = RefreshState.None
        }
        if (mState == RefreshState.Refreshing) {
            finishRefresh()
        } else if (mState == RefreshState.Loading) {
            finishLoadMore()
        } else {
            /*
             * 2020-3-15 closeHeaderOrFooter 的关闭逻辑，
             * 帮助 FalsifyHeader 取消刷新
             * 邦族 FalsifyFooter 取消加载
             */
            if (mKernel.animSpinner(0) == null) {
                notifyStateChanged(RefreshState.None)
            } else {
                if (mState.isHeader) {
                    notifyStateChanged(RefreshState.PullDownCanceled)
                } else {
                    notifyStateChanged(RefreshState.PullUpCanceled)
                }
            }
        }
        return this
    }

    /**
     * Display refresh animation and trigger refresh event.
     * 显示刷新动画并且触发刷新事件
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoRefresh(): kotlin.Boolean {
        return autoRefresh(
            if (mAttachedToWindow) 0 else 400,
            mReboundDuration,
            (mHeaderMaxDragRate + mHeaderTriggerRate) / 2,
            false
        )
    }

    /**
     * Display refresh animation and trigger refresh event, Delayed start.
     * 显示刷新动画并且触发刷新事件，延时启动
     * @param delayed 开始延时
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoRefresh(delayed: Int): kotlin.Boolean {
        return autoRefresh(
            delayed,
            mReboundDuration,
            (mHeaderMaxDragRate + mHeaderTriggerRate) / 2,
            false
        )
    }


    /**
     * Display refresh animation without triggering events.
     * 显示刷新动画，不触发事件
     * 注意：本方法只会开启动画，不会触发刷新事件。所以需要自己在外部适当的时机调用 finishRefresh 来关闭刷新动画
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoRefreshAnimationOnly(): kotlin.Boolean {
        return autoRefresh(
            if (mAttachedToWindow) 0 else 400,
            mReboundDuration,
            (mHeaderMaxDragRate + mHeaderTriggerRate) / 2,
            true
        )
    }

    /**
     * Display refresh animation, Multifunction.
     * 显示刷新动画并且触发刷新事件
     * @param delayed 开始延时
     * @param duration 拖拽动画持续时间
     * @param dragRate 拉拽的高度比率
     * @param animationOnly animation only 只有动画
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoRefresh(
        delayed: Int,
        duration: Int,
        dragRate: Float,
        animationOnly: kotlin.Boolean
    ): kotlin.Boolean {
        if (mState == RefreshState.None && isEnableRefreshOrLoadMore(mEnableRefresh)) {
            val runnable = Runnable {
                if (mViceState != RefreshState.Refreshing) return@Runnable
                if (reboundAnimator != null) {
                    reboundAnimator?.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
                    reboundAnimator?.cancel() //会触发 cancel 和 end 调用
                    reboundAnimator = null
                }

                val thisView: View = this@SmartRefreshLayout
                mLastTouchX = thisView.measuredWidth / 2f
                mKernel.setState(RefreshState.PullDownToRefresh)

                if (mRefreshHeader?.autoOpen(duration, dragRate, animationOnly) == true) {
                    /*
                     * 2022-11-03 添加Header可以自己实现 autoOpen ，返回true表示支持，返回False表示不支持，使用老版本的 autoOpen
                     */
                    return@Runnable
                }

                val height =
                    if (mHeaderHeight == 0) mHeaderTriggerRate else mHeaderHeight.toFloat()
                val dragHeight = if (dragRate < 10) height * dragRate else dragRate
                reboundAnimator = ValueAnimator.ofInt(mSpinner, (dragHeight).toInt())
                reboundAnimator?.setDuration(duration.toLong())
                reboundAnimator?.interpolator = SmartUtil(SmartUtil.INTERPOLATOR_VISCOUS_FLUID)
                reboundAnimator?.addUpdateListener { animation: ValueAnimator? ->
                    if (reboundAnimator != null) {
                        mKernel.moveSpinner(animation?.getAnimatedValue() as? Int ?: 0, true)
                    }
                }
                reboundAnimator?.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        mKernel.onAutoRefreshAnimationEnd(animation, animationOnly)
                    }
                })
                reboundAnimator?.start()
            }
            setViceState(RefreshState.Refreshing)
            if (delayed > 0) {
                mHandler?.postDelayed(runnable, delayed.toLong())
            } else {
                runnable.run()
            }
            return true
        } else {
            return false
        }
    }

    /**
     * Display load more animation and trigger load more event.
     * 显示加载动画并且触发刷新事件
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoLoadMore(): kotlin.Boolean {
        return autoLoadMore(
            0,
            mReboundDuration,
            (mFooterMaxDragRate + mFooterTriggerRate) / 2,
            false
        )
    }

    /**
     * Display load more animation and trigger load more event, Delayed start.
     * 显示加载动画并且触发刷新事件, 延时启动
     * @param delayed 开始延时
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoLoadMore(delayed: Int): kotlin.Boolean {
        return autoLoadMore(
            delayed,
            mReboundDuration,
            (mFooterMaxDragRate + mFooterTriggerRate) / 2,
            false
        )
    }

    /**
     * Display load more animation without triggering events.
     * 显示加载动画，不触发事件
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoLoadMoreAnimationOnly(): kotlin.Boolean {
        return autoLoadMore(
            0,
            mReboundDuration,
            (mFooterMaxDragRate + mFooterTriggerRate) / 2,
            true
        )
    }

    /**
     * Display load more animation and trigger load more event, Delayed start.
     * 显示加载动画, 多功能选项
     * @param delayed 开始延时
     * @param duration 拖拽动画持续时间
     * @param dragRate 拉拽的高度比率
     * @return true or false, Status non-compliance will fail.
     * 是否成功（状态不符合会失败）
     */
    override fun autoLoadMore(
        delayed: Int,
        duration: Int,
        dragRate: Float,
        animationOnly: kotlin.Boolean
    ): kotlin.Boolean {
        if (mState == RefreshState.None && (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mFooterNoMoreData)) {
            val runnable = Runnable {
                if (mViceState != RefreshState.Loading) return@Runnable
                if (reboundAnimator != null) {
                    reboundAnimator?.setDuration(0) //cancel会触发End调用，可以判断0来确定是否被cancel
                    reboundAnimator?.cancel() //会触发 cancel 和 end 调用
                    reboundAnimator = null
                }

                val thisView: View = this@SmartRefreshLayout
                mLastTouchX = thisView.measuredWidth / 2f
                mKernel.setState(RefreshState.PullUpToLoad)

                if (mRefreshFooter?.autoOpen(duration, dragRate, animationOnly) == true) {
                    /*
                     * 2022-11-03 添加Header可以自己实现 autoOpen ，返回true表示支持，返回False表示不支持，使用老版本的 autoOpen
                     */
                    return@Runnable
                }

                val height =
                    if (mFooterHeight == 0) mFooterTriggerRate else mFooterHeight.toFloat()
                val dragHeight = if (dragRate < 10) dragRate * height else dragRate
                reboundAnimator = ValueAnimator.ofInt(mSpinner, -(dragHeight).toInt())
                reboundAnimator?.setDuration(duration.toLong())
                reboundAnimator?.interpolator = SmartUtil(SmartUtil.INTERPOLATOR_VISCOUS_FLUID)
                reboundAnimator?.addUpdateListener { animation: ValueAnimator? ->
                    if (reboundAnimator != null && mRefreshFooter != null) {
                        mKernel.moveSpinner(animation?.getAnimatedValue() as? Int ?: 0, true)
                    }
                }
                reboundAnimator?.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        mKernel.onAutoLoadMoreAnimationEnd(animation, animationOnly)
                    }
                })
                reboundAnimator?.start()
            }
            setViceState(RefreshState.Loading)
            if (delayed > 0) {
                mHandler?.postDelayed(runnable, delayed.toLong())
            } else {
                runnable.run()
            }
            return true
        } else {
            return false
        }
    }

    //<editor-fold desc="丢弃的API">
    /**
     * 是否正在刷新
     * @return 是否正在刷新
     */
    override fun isRefreshing(): kotlin.Boolean {
        return mState == RefreshState.Refreshing
    }

    /**
     * 是否正在加载
     * @return 是否正在加载
     */
    override fun isLoading(): kotlin.Boolean {
        return mState == RefreshState.Loading
    }

    //</editor-fold>
    //</editor-fold>
    //<editor-fold desc="核心接口 RefreshKernel">
    /**
     * 刷新布局核心功能接口
     * 为功能复杂的 Header 或者 Footer 开放的接口
     */
    inner class RefreshKernelImpl : RefreshKernel {
        override fun getRefreshLayout(): RefreshLayout {
            return this@SmartRefreshLayout
        }

        override fun getRefreshContent(): RefreshContent {
            return mRefreshContent!!
        }

        override fun setState(state: RefreshState): RefreshKernel {
            when (state) {
                RefreshState.None -> if (mState != RefreshState.None && mSpinner == 0) {
                    notifyStateChanged(RefreshState.None)
                } else if (mSpinner != 0) {
                    animSpinner(0)
                }

                RefreshState.PullDownToRefresh -> if (!mState.isOpening && isEnableRefreshOrLoadMore(
                        mEnableRefresh
                    )
                ) {
                    notifyStateChanged(RefreshState.PullDownToRefresh)
                } else {
                    setViceState(RefreshState.PullDownToRefresh)
                }

                RefreshState.PullUpToLoad -> if (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mState.isOpening && !mState.isFinishing && !(mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective)) {
                    notifyStateChanged(RefreshState.PullUpToLoad)
                } else {
                    setViceState(RefreshState.PullUpToLoad)
                }

                RefreshState.PullDownCanceled -> if (!mState.isOpening && isEnableRefreshOrLoadMore(
                        mEnableRefresh
                    )
                ) {
                    notifyStateChanged(RefreshState.PullDownCanceled)
                    setState(RefreshState.None)
                } else {
                    setViceState(RefreshState.PullDownCanceled)
                }

                RefreshState.PullUpCanceled -> if (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mState.isOpening && !(mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective)) {
                    notifyStateChanged(RefreshState.PullUpCanceled)
                    setState(RefreshState.None)
                } else {
                    setViceState(RefreshState.PullUpCanceled)
                }

                RefreshState.ReleaseToRefresh -> if (!mState.isOpening && isEnableRefreshOrLoadMore(
                        mEnableRefresh
                    )
                ) {
                    notifyStateChanged(RefreshState.ReleaseToRefresh)
                } else {
                    setViceState(RefreshState.ReleaseToRefresh)
                }

                RefreshState.ReleaseToLoad -> if (isEnableRefreshOrLoadMore(mEnableLoadMore) && !mState.isOpening && !mState.isFinishing && !(mFooterNoMoreData && mEnableFooterFollowWhenNoMoreData && mFooterNoMoreDataEffective)) {
                    notifyStateChanged(RefreshState.ReleaseToLoad)
                } else {
                    setViceState(RefreshState.ReleaseToLoad)
                }

                RefreshState.ReleaseToTwoLevel -> {
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.ReleaseToTwoLevel)
                    } else {
                        setViceState(RefreshState.ReleaseToTwoLevel)
                    }
                }

                RefreshState.RefreshReleased -> {
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableRefresh)) {
                        notifyStateChanged(RefreshState.RefreshReleased)
                    } else {
                        setViceState(RefreshState.RefreshReleased)
                    }
                }

                RefreshState.LoadReleased -> {
                    if (!mState.isOpening && isEnableRefreshOrLoadMore(mEnableLoadMore)) {
                        notifyStateChanged(RefreshState.LoadReleased)
                    } else {
                        setViceState(RefreshState.LoadReleased)
                    }
                }

                RefreshState.Refreshing -> setStateRefreshing(true)
                RefreshState.Loading -> setStateLoading(true)
                else -> notifyStateChanged(state)
            }
            return this
        }

        override fun startTwoLevel(open: kotlin.Boolean): RefreshKernel {
            if (open) {
                val listener: AnimatorListenerAdapter = object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (animation.duration == 0L) {
                            return  //0 表示被取消
                        }
                        mKernel.setState(RefreshState.TwoLevel)
                    }
                }
                val thisView: View = this@SmartRefreshLayout
                val animator = animSpinner(thisView.measuredHeight)
                if (animator != null && animator === reboundAnimator) {
                    animator.setDuration(mFloorDuration.toLong())
                    animator.addListener(listener)
                } else {
                    animator?.apply {
                        listener.onAnimationEnd(this)
                    }
                }
            } else {
                if (animSpinner(0) == null) {
                    notifyStateChanged(RefreshState.None)
                }
            }
            return this
        }

        override fun finishTwoLevel(): RefreshKernel {
            if (mState == RefreshState.TwoLevel) {
                mKernel.setState(RefreshState.TwoLevelFinish)
                if (mSpinner == 0) {
                    moveSpinner(0, false)
                    notifyStateChanged(RefreshState.None)
                } else {
                    animSpinner(0)?.setDuration(mFloorDuration.toLong())
                }
            }
            return this
        }

        /**
         * 移动滚动 Scroll
         * moveSpinner 的取名来自 谷歌官方的 { android.support.v4.widget.SwipeRefreshLayout#moveSpinner(float)}
         * moveSpinner The name comes from { android.support.v4.widget.SwipeRefreshLayout#moveSpinner(float)}
         * @param spinner 新的 spinner
         * @param isDragging 是否是拖动产生的滚动
         * 只有，finishRefresh，finishLoadMore，overSpinner 的回弹动画才会是 false
         * dispatchTouchEvent , nestScroll 等都为 true
         * autoRefresh，autoLoadMore，需要模拟拖动，也为 true
         */
        override fun moveSpinner(spinner: Int, isDragging: kotlin.Boolean): RefreshKernel {
            if (mSpinner == spinner && (mRefreshHeader == null || mRefreshHeader?.isSupportHorizontalDrag() == false)
                && (mRefreshFooter == null || mRefreshFooter?.isSupportHorizontalDrag() == false)
            ) {
                return this
            }
            val thisView: View = this@SmartRefreshLayout
            val oldSpinner = mSpinner
            mSpinner = spinner
            // 附加 mViceState.isDragging 的判断，是因为 isDragging 有时候时动画模拟的，如 autoRefresh 动画
            //
            if (isDragging && (mViceState.isDragging || mViceState.isOpening)) {
                if (mSpinner > (if (mHeaderTriggerRate < 10) mHeaderHeight * mHeaderTriggerRate else mHeaderTriggerRate)) {
                    if (mState != RefreshState.ReleaseToTwoLevel) {
                        mKernel.setState(RefreshState.ReleaseToRefresh)
                    }
                } else if (-mSpinner > (if (mFooterTriggerRate < 10) mFooterHeight * mFooterTriggerRate else mFooterTriggerRate) && !mFooterNoMoreData) {
                    mKernel.setState(RefreshState.ReleaseToLoad)
                } else if (mSpinner < 0 && !mFooterNoMoreData) {
                    mKernel.setState(RefreshState.PullUpToLoad)
                } else if (mSpinner > 0) {
                    mKernel.setState(RefreshState.PullDownToRefresh)
                }
            }
            if (mRefreshContent != null) {
                var tSpinner = 0
                var changed = false
                /*
                 * 2019-12-25 修复 2.0 版本之后无默认 Header Footer 导致的纯滚动模式无效
                 */
                if (spinner >= 0) {
                    if (isEnableTranslationContent(
                            mEnableHeaderTranslationContent,
                            mRefreshHeader
                        )
                    ) {
                        changed = true
                        tSpinner = spinner
                    } else if (oldSpinner < 0) {
                        changed = true
                    }
                }
                /*
                 * 2019-12-25 修复 2.0 版本之后无默认 Header Footer 导致的纯滚动模式无效
                 */
                if (spinner <= 0) {
                    if (isEnableTranslationContent(
                            mEnableFooterTranslationContent,
                            mRefreshFooter
                        )
                    ) {
                        changed = true
                        tSpinner = spinner
                    } else if (oldSpinner > 0) {
                        changed = true
                    }
                }
                if (changed) {
                    mRefreshContent?.moveSpinner(
                        tSpinner,
                        mHeaderTranslationViewId,
                        mFooterTranslationViewId
                    )
                    if (mFooterNoMoreData &&
                        mFooterNoMoreDataEffective &&
                        mEnableFooterFollowWhenNoMoreData &&
                        mRefreshFooter is RefreshFooter &&
                        mRefreshFooter?.getSpinnerStyle() === SpinnerStyle.Translate &&
                        isEnableRefreshOrLoadMore(mEnableLoadMore)
                    ) {
                        mRefreshFooter?.getView()?.translationY =
                            max(0.toFloat(), tSpinner.toFloat())
                    }
                    var header =
                        mEnableClipHeaderWhenFixedBehind && mRefreshHeader != null && mRefreshHeader?.getSpinnerStyle() === SpinnerStyle.FixedBehind
                    header = header || mHeaderBackgroundColor != 0
                    var footer =
                        mEnableClipFooterWhenFixedBehind && mRefreshFooter != null && mRefreshFooter?.getSpinnerStyle() === SpinnerStyle.FixedBehind
                    footer = footer || mFooterBackgroundColor != 0
                    if ((header && (tSpinner >= 0 || oldSpinner > 0)) || (footer && (tSpinner <= 0 || oldSpinner < 0))) {
                        thisView.invalidate()
                    }
                }
            }
            if ((spinner >= 0 || oldSpinner > 0) && mRefreshHeader != null) {
                val offset = max(spinner, 0)
                val headerHeight = mHeaderHeight
                val maxDragHeight =
                    (if (mHeaderMaxDragRate < 10) mHeaderHeight * mHeaderMaxDragRate else mHeaderMaxDragRate).toInt()
                val percent =
                    1f * offset / (if (mHeaderTriggerRate < 10) mHeaderTriggerRate * mHeaderHeight else mHeaderTriggerRate)
                //因为用户有可能 finish 之后，直接 enable=false 关闭，所以还要加上 state 的状态判断
                if (isEnableRefreshOrLoadMore(mEnableRefresh) || (mState == RefreshState.RefreshFinish && !isDragging)) {
                    if (oldSpinner != mSpinner) {
                        if (mRefreshHeader?.getSpinnerStyle() === SpinnerStyle.Translate) {
                            mRefreshHeader?.getView()?.translationY = mSpinner.toFloat()
                            if (mHeaderBackgroundColor != 0 && mPaint != null && !isEnableTranslationContent(
                                    mEnableHeaderTranslationContent,
                                    mRefreshHeader
                                )
                            ) {
                                thisView.invalidate()
                            }
                        } else if (mRefreshHeader?.getSpinnerStyle()?.scale == true) {
                            /*
                             * 兼容 MotionLayout 2019-6-18
                             * 在 MotionLayout 内部 requestLayout 无效
                             * 该用 直接调用 layout 方式
                             * https://github.com/scwang90/SmartRefreshLayout/issues/944
                             */
                            val headerView: View? = mRefreshHeader?.getView()
                            val lp = headerView?.layoutParams
                            val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                            headerView?.also {
                                val widthSpec = MeasureSpec.makeMeasureSpec(
                                    it.measuredWidth,
                                    MeasureSpec.EXACTLY
                                )
                                it.measure(
                                    widthSpec,
                                    MeasureSpec.makeMeasureSpec(
                                        max(
                                            mSpinner - mlp.bottomMargin - mlp.topMargin,
                                            0
                                        ), MeasureSpec.EXACTLY
                                    )
                                )
                                val left = mlp.leftMargin
                                val top = mlp.topMargin + mHeaderInsetStart
                                it.layout(
                                    left,
                                    top,
                                    left + headerView.measuredWidth,
                                    top + headerView.measuredHeight
                                )
                            }
                        }
                        mRefreshHeader?.onMoving(
                            isDragging,
                            percent,
                            offset,
                            headerHeight,
                            maxDragHeight
                        )
                    }
                    if (isDragging && mRefreshHeader?.isSupportHorizontalDrag() == true) {
                        val offsetX = mLastTouchX.toInt()
                        val offsetMax = thisView.width
                        val percentX = mLastTouchX / (if (offsetMax == 0) 1 else offsetMax)
                        mRefreshHeader?.onHorizontalDrag(percentX, offsetX, offsetMax)
                    }
                }

                if (oldSpinner != mSpinner && mOnMultiListener != null && mRefreshHeader is RefreshHeader) {
                    mOnMultiListener?.onHeaderMoving(
                        mRefreshHeader as RefreshHeader,
                        isDragging,
                        percent,
                        offset,
                        headerHeight,
                        maxDragHeight
                    )
                }
            }
            if ((spinner <= 0 || oldSpinner < 0) && mRefreshFooter != null) {
                val offset = -min(spinner, 0)
                val footerHeight = mFooterHeight
                val maxDragHeight =
                    (if (mFooterMaxDragRate < 10) mFooterHeight * mFooterMaxDragRate else mFooterMaxDragRate).toInt()
                val percent =
                    offset * 1f / (if (mFooterTriggerRate < 10) mFooterTriggerRate * mFooterHeight else mFooterTriggerRate)

                if (isEnableRefreshOrLoadMore(mEnableLoadMore) || (mState == RefreshState.LoadFinish && !isDragging)) {
                    if (oldSpinner != mSpinner) {
                        if (mRefreshFooter?.getSpinnerStyle() === SpinnerStyle.Translate) {
                            mRefreshFooter?.getView()?.translationY = mSpinner.toFloat()
                            if (mFooterBackgroundColor != 0 && mPaint != null && !isEnableTranslationContent(
                                    mEnableFooterTranslationContent,
                                    mRefreshFooter
                                )
                            ) {
                                thisView.invalidate()
                            }
                        } else if (mRefreshFooter?.getSpinnerStyle()?.scale == true) {
                            /*
                             * 兼容 MotionLayout 2019-6-18
                             * 在 MotionLayout 内部 requestLayout 无效
                             * 该用 直接调用 layout 方式
                             * https://github.com/scwang90/SmartRefreshLayout/issues/944
                             */
                            val footerView: View? = mRefreshFooter?.getView()
                            val lp = footerView?.layoutParams
                            val mlp = lp as? MarginLayoutParams ?: sDefaultMarginLP
                            footerView?.also {
                                val widthSpec = MeasureSpec.makeMeasureSpec(
                                    it.measuredWidth,
                                    MeasureSpec.EXACTLY
                                )
                                it.measure(
                                    widthSpec,
                                    MeasureSpec.makeMeasureSpec(
                                        max(
                                            -mSpinner - mlp.bottomMargin - mlp.topMargin,
                                            0
                                        ), MeasureSpec.EXACTLY
                                    )
                                )
                                val left = mlp.leftMargin
                                val bottom =
                                    mlp.topMargin + thisView.measuredHeight - mFooterInsetStart
                                it.layout(
                                    left,
                                    bottom - footerView.measuredHeight,
                                    left + footerView.measuredWidth,
                                    bottom
                                )
                            }
                        }
                        mRefreshFooter?.onMoving(
                            isDragging,
                            percent,
                            offset,
                            footerHeight,
                            maxDragHeight
                        )
                    }
                    if (isDragging && mRefreshFooter?.isSupportHorizontalDrag() == true) {
                        val offsetX = mLastTouchX.toInt()
                        val offsetMax = thisView.width
                        val percentX = mLastTouchX / (if (offsetMax == 0) 1 else offsetMax)
                        mRefreshFooter?.onHorizontalDrag(percentX, offsetX, offsetMax)
                    }
                }

                if (oldSpinner != mSpinner && mOnMultiListener != null && mRefreshFooter is RefreshFooter) {
                    mOnMultiListener?.onFooterMoving(
                        mRefreshFooter as RefreshFooter,
                        isDragging,
                        percent,
                        offset,
                        footerHeight,
                        maxDragHeight
                    )
                }
            }
            return this
        }

        override fun animSpinner(endSpinner: Int): ValueAnimator? {
            return this@SmartRefreshLayout.animSpinner(
                endSpinner,
                0,
                mReboundInterpolator,
                mReboundDuration
            )
        }

        override fun requestDrawBackgroundFor(
            internal: RefreshComponent,
            backgroundColor: Int
        ): RefreshKernel {
            if (mPaint == null && backgroundColor != 0) {
                mPaint = Paint()
            }
            if (internal == mRefreshHeader) {
                mHeaderBackgroundColor = backgroundColor
            } else if (internal == mRefreshFooter) {
                mFooterBackgroundColor = backgroundColor
            }
            return this
        }

        override fun requestNeedTouchEventFor(
            internal: RefreshComponent,
            request: kotlin.Boolean
        ): RefreshKernel {
            if (internal == mRefreshHeader) {
                mHeaderNeedTouchEventWhenRefreshing = request
            } else if (internal == mRefreshFooter) {
                mFooterNeedTouchEventWhenLoading = request
            }
            return this
        }

        override fun requestDefaultTranslationContentFor(
            internal: RefreshComponent,
            translation: kotlin.Boolean
        ): RefreshKernel {
            if (internal == mRefreshHeader) {
                if (!mManualHeaderTranslationContent) {
                    mManualHeaderTranslationContent = true
                    mEnableHeaderTranslationContent = translation
                }
            } else if (internal == mRefreshFooter) {
                if (!mManualFooterTranslationContent) {
                    mManualFooterTranslationContent = true
                    mEnableFooterTranslationContent = translation
                }
            }
            return this
        }

        override fun requestRemeasureHeightFor(internal: RefreshComponent): RefreshKernel {
            if (internal == mRefreshHeader) {
                if (mHeaderHeightStatus.notified) {
                    mHeaderHeightStatus = mHeaderHeightStatus.unNotify()
                }
            } else if (internal == mRefreshFooter) {
                if (mFooterHeightStatus.notified) {
                    mFooterHeightStatus = mFooterHeightStatus.unNotify()
                }
            }
            return this
        }

        override fun requestFloorDuration(duration: Int): RefreshKernel {
            mFloorDuration = duration
            return this
        }

        override fun requestFloorBottomPullUpToCloseRate(rate: Float): RefreshKernel {
            mTwoLevelBottomPullUpToCloseRate = rate
            return this
        }

        override fun onAutoRefreshAnimationEnd(
            animation: Animator?,
            animationOnly: kotlin.Boolean
        ): RefreshKernel {
            if (animation != null && animation.duration == 0L) {
                return this //0 表示被取消
            }
            reboundAnimator = null
            if (mState != RefreshState.ReleaseToRefresh) {
                this.setState(RefreshState.ReleaseToRefresh)
            }
            this@SmartRefreshLayout.setStateRefreshing(!animationOnly)
            return this
        }

        override fun onAutoLoadMoreAnimationEnd(
            animation: Animator?,
            animationOnly: kotlin.Boolean
        ): RefreshKernel {
            if (animation != null && animation.duration == 0L) {
                return this //0 表示被取消
            }
            reboundAnimator = null
            if (mRefreshFooter != null) {
                if (mState != RefreshState.ReleaseToLoad) {
                    this.setState(RefreshState.ReleaseToLoad)
                }
                this@SmartRefreshLayout.setStateLoading(!animationOnly)
            } else {
                /*
                 * 2019-12-24 修复 mRefreshFooter=null 时状态错乱问题
                 */
                this.setState(RefreshState.None)
            }
            return this
        }
    }

    companion object {
        //全局默认 Footer 构造器
        protected var sFooterCreator: DefaultRefreshFooterCreator? = null

        //全局默认 Header 构造器
        protected var sHeaderCreator: DefaultRefreshHeaderCreator? = null

        //全局默认 控件 初始化器
        protected var sRefreshInitializer: DefaultRefreshInitializer? = null

        //默认全局 布局 Margin
        protected var sDefaultMarginLP: MarginLayoutParams = MarginLayoutParams(-1, -1)

        /**
         * 设置默认 Header 构建器
         * @param creator Header构建器
         */
        fun setDefaultRefreshHeaderCreator(creator: DefaultRefreshHeaderCreator) {
            sHeaderCreator = creator
        }

        /**
         * 设置默认 Footer 构建器
         * @param creator Footer构建器
         */
        fun setDefaultRefreshFooterCreator(creator: DefaultRefreshFooterCreator) {
            sFooterCreator = creator
        }

        /**
         * 设置默认 Refresh 初始化器
         * @param initializer 全局初始化器
         */
        fun setDefaultRefreshInitializer(initializer: DefaultRefreshInitializer) {
            sRefreshInitializer = initializer
        }
    }
}
