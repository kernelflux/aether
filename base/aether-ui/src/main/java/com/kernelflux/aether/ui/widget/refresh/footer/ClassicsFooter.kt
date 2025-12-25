package com.kernelflux.aether.ui.widget.refresh.footer

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import com.kernelflux.aether.ui.widget.refresh.api.RefreshFooter
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.classics.ArrowDrawable
import com.kernelflux.aether.ui.widget.refresh.classics.ClassicsAbstract
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState
import com.kernelflux.aether.ui.widget.refresh.constant.SpinnerStyle
import com.kernelflux.aether.ui.widget.refresh.drawable.ProgressDrawable
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil
import androidx.core.content.withStyledAttributes
import com.kernelflux.aether.ui.R

/**
 * 经典上拉底部
 * Created by scwang on 2017/5/28.
 */
@Suppress("unused")
open class ClassicsFooter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ClassicsAbstract<ClassicsFooter>(context, attrs, 0), RefreshFooter {
    protected var mTextPulling: String? = null //"上拉加载更多";
    protected var mTextRelease: String? = null //"释放立即加载";
    protected var mTextLoading: String? = null //"正在加载...";
    protected var mTextRefreshing: String? = null //"正在刷新...";
    protected var mTextFinish: String? = null //"加载完成";
    protected var mTextFailed: String? = null //"加载失败";
    protected var mTextNothing: String? = null //"没有更多数据了";

    protected var mNoMoreData: Boolean = false

    init {
        inflate(context, R.layout.srl_classics_footer, this)
        mArrowView = findViewById<ImageView>(R.id.srl_classics_arrow)
        mProgressView = findViewById<ImageView>(R.id.srl_classics_progress)
        mTitleText = findViewById<TextView>(R.id.srl_classics_title)

        context.withStyledAttributes(attrs, R.styleable.ClassicsFooter) {

            val lpArrow = mArrowView?.layoutParams as? LayoutParams
            val lpProgress = mProgressView?.layoutParams as? LayoutParams
            val rightMargin = getDimensionPixelSize(
                R.styleable.ClassicsFooter_srlDrawableMarginRight,
                SmartUtil.dp2px(20f)
            )
            lpProgress?.rightMargin = rightMargin
            lpArrow?.rightMargin = rightMargin

            lpArrow?.also {
                it.width = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableArrowSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableArrowSize,
                    it.height
                )
            }


            lpProgress?.also {
                it.width = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableProgressSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableProgressSize,
                    it.height
                )
            }


            lpArrow?.also {
                it.width = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableSize,
                    it.height
                )
            }


            lpProgress?.also {
                it.width = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsFooter_srlDrawableSize,
                    it.height
                )
            }

            mFinishDuration = getInt(R.styleable.ClassicsFooter_srlFinishDuration, mFinishDuration)


            mSpinnerStyle = mSpinnerStyle?.let {
                SpinnerStyle.values[getInt(
                    R.styleable.ClassicsFooter_srlClassicsSpinnerStyle,
                    it.ordinal
                )]
            }

            if (hasValue(R.styleable.ClassicsFooter_srlDrawableArrow)) {
                mArrowView?.setImageDrawable(getDrawable(R.styleable.ClassicsFooter_srlDrawableArrow))
            } else if (mArrowView?.getDrawable() == null) {
                mArrowDrawable = ArrowDrawable()
                mArrowDrawable?.setColor(-0x99999a)
                mArrowView?.setImageDrawable(mArrowDrawable)
            }

            if (hasValue(R.styleable.ClassicsFooter_srlDrawableProgress)) {
                mProgressView?.setImageDrawable(getDrawable(R.styleable.ClassicsFooter_srlDrawableProgress))
            } else if (mProgressView?.getDrawable() == null) {
                mProgressDrawable = ProgressDrawable()
                mProgressDrawable?.setColor(-0x99999a)
                mProgressView?.setImageDrawable(mProgressDrawable)
            }

            if (hasValue(R.styleable.ClassicsFooter_srlTextSizeTitle)) {
                mTitleText?.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    getDimensionPixelSize(
                        R.styleable.ClassicsFooter_srlTextSizeTitle,
                        SmartUtil.dp2px(16f)
                    ).toFloat()
                )
            }

            if (hasValue(R.styleable.ClassicsFooter_srlPrimaryColor)) {
                super.setPrimaryColor(getColor(R.styleable.ClassicsFooter_srlPrimaryColor, 0))
            }
            if (hasValue(R.styleable.ClassicsFooter_srlAccentColor)) {
                super.setAccentColor(getColor(R.styleable.ClassicsFooter_srlAccentColor, 0))
            }

            mTextPulling = if (hasValue(R.styleable.ClassicsFooter_srlTextPulling)) {
                getString(R.styleable.ClassicsFooter_srlTextPulling)
            } else if (REFRESH_FOOTER_PULLING != null) {
                REFRESH_FOOTER_PULLING
            } else {
                context.getString(R.string.srl_footer_pulling)
            }
            mTextRelease = if (hasValue(R.styleable.ClassicsFooter_srlTextRelease)) {
                getString(R.styleable.ClassicsFooter_srlTextRelease)
            } else if (REFRESH_FOOTER_RELEASE != null) {
                REFRESH_FOOTER_RELEASE
            } else {
                context.getString(R.string.srl_footer_release)
            }
            mTextLoading = if (hasValue(R.styleable.ClassicsFooter_srlTextLoading)) {
                getString(R.styleable.ClassicsFooter_srlTextLoading)
            } else if (REFRESH_FOOTER_LOADING != null) {
                REFRESH_FOOTER_LOADING
            } else {
                context.getString(R.string.srl_footer_loading)
            }
            mTextRefreshing = if (hasValue(R.styleable.ClassicsFooter_srlTextRefreshing)) {
                getString(R.styleable.ClassicsFooter_srlTextRefreshing)
            } else if (REFRESH_FOOTER_REFRESHING != null) {
                REFRESH_FOOTER_REFRESHING
            } else {
                context.getString(R.string.srl_footer_refreshing)
            }
            mTextFinish = if (hasValue(R.styleable.ClassicsFooter_srlTextFinish)) {
                getString(R.styleable.ClassicsFooter_srlTextFinish)
            } else if (REFRESH_FOOTER_FINISH != null) {
                REFRESH_FOOTER_FINISH
            } else {
                context.getString(R.string.srl_footer_finish)
            }
            mTextFailed = if (hasValue(R.styleable.ClassicsFooter_srlTextFailed)) {
                getString(R.styleable.ClassicsFooter_srlTextFailed)
            } else if (REFRESH_FOOTER_FAILED != null) {
                REFRESH_FOOTER_FAILED
            } else {
                context.getString(R.string.srl_footer_failed)
            }

            mTextNothing = if (hasValue(R.styleable.ClassicsFooter_srlTextNothing)) {
                getString(R.styleable.ClassicsFooter_srlTextNothing)
            } else if (REFRESH_FOOTER_NOTHING != null) {
                REFRESH_FOOTER_NOTHING
            } else {
                context.getString(R.string.srl_footer_nothing)
            }

        }

        mProgressView?.animate()?.setInterpolator(null)
        mTitleText?.text = if (isInEditMode) mTextLoading else mTextPulling
        if (isInEditMode) {
            mArrowView?.setVisibility(GONE)
        } else {
            mProgressView?.setVisibility(GONE)
        }
    }


    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        /*
         * 2020-5-15 修复BUG
         * https://github.com/scwang90/SmartRefreshLayout/issues/1003
         * 修复 没有更多数据之后 loading 还在显示问题
         */
        super.onFinish(refreshLayout, success)
        if (!mNoMoreData) {
            mTitleText?.text = if (success) mTextFinish else mTextFailed
            return mFinishDuration
        }
        return 0
    }

    /**
     * @param colors 对应Xml中配置的 srlPrimaryColor srlAccentColor
     */
    @Deprecated(
        """只由框架调用
      使用者使用 {@link RefreshLayout#setPrimaryColorsId(int...)}"""
    )
    override fun setPrimaryColors(@ColorInt vararg colors: Int) {
        if (mSpinnerStyle === SpinnerStyle.FixedBehind) {
            super.setPrimaryColors(*colors)
        }
    }

    /**
     * 设置数据全部加载完成，将不能再次触发加载功能
     */
    override fun setNoMoreData(noMoreData: Boolean): Boolean {
        if (mNoMoreData != noMoreData) {
            mNoMoreData = noMoreData
            val arrowView: View? = mArrowView
            if (noMoreData) {
                mTitleText?.text = mTextNothing
                arrowView?.visibility = GONE
            } else {
                mTitleText?.text = mTextPulling
                arrowView?.visibility = VISIBLE
            }
        }
        return true
    }


    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
        val arrowView: View? = mArrowView
        if (!mNoMoreData) {
            when (newState) {
                RefreshState.None -> {
                    arrowView?.visibility = VISIBLE
                    mTitleText?.text = mTextPulling
                    arrowView?.animate()?.rotation(180f)
                }

                RefreshState.PullUpToLoad -> {
                    mTitleText?.text = mTextPulling
                    arrowView?.animate()?.rotation(180f)
                }

                RefreshState.Loading, RefreshState.LoadReleased -> {
                    arrowView?.visibility = GONE
                    mTitleText?.text = mTextLoading
                }

                RefreshState.ReleaseToLoad -> {
                    mTitleText?.text = mTextRelease
                    arrowView?.animate()?.rotation(0f)
                }

                RefreshState.Refreshing -> {
                    mTitleText?.text = mTextRefreshing
                    arrowView?.visibility = GONE
                }

                else -> {

                }
            }
        }
    }


    companion object {
        var REFRESH_FOOTER_PULLING: String? = null //"上拉加载更多";
        var REFRESH_FOOTER_RELEASE: String? = null //"释放立即加载";
        var REFRESH_FOOTER_LOADING: String? = null //"正在加载...";
        var REFRESH_FOOTER_REFRESHING: String? = null //"正在刷新...";
        var REFRESH_FOOTER_FINISH: String? = null //"加载完成";
        var REFRESH_FOOTER_FAILED: String? = null //"加载失败";
        var REFRESH_FOOTER_NOTHING: String? = null //"没有更多数据了";
    }


}
