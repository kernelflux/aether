package com.kernelflux.aether.ui.widget.refresh.header

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import com.kernelflux.aether.ui.widget.refresh.api.RefreshHeader
import com.kernelflux.aether.ui.widget.refresh.api.RefreshLayout
import com.kernelflux.aether.ui.widget.refresh.classics.ArrowDrawable
import com.kernelflux.aether.ui.widget.refresh.classics.ClassicsAbstract
import com.kernelflux.aether.ui.widget.refresh.constant.RefreshState
import com.kernelflux.aether.ui.widget.refresh.constant.SpinnerStyle
import com.kernelflux.aether.ui.widget.refresh.drawable.ProgressDrawable
import com.kernelflux.aether.ui.widget.refresh.util.SmartUtil
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.core.content.withStyledAttributes
import androidx.core.content.edit
import com.kernelflux.aether.ui.R

/**
 * 经典下拉头部
 * Created by scwang on 2017/5/28.
 */
@Suppress("unused")
open class ClassicsHeader @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ClassicsAbstract<ClassicsHeader>(context, attrs, 0), RefreshHeader {
    //    public static String REFRESH_HEADER_UPDATE = "'Last update' M-d HH:mm";
    protected var KEY_LAST_UPDATE_TIME: String = "LAST_UPDATE_TIME"

    protected var mLastTime: Date? = null
    protected var mLastUpdateText: TextView
    protected var mShared: SharedPreferences?
    protected lateinit var mLastUpdateFormat: DateFormat
    protected var mEnableLastTime: Boolean = true

    protected var mTextPulling: String? = null //"下拉可以刷新";
    protected var mTextRefreshing: String? = null //"正在刷新...";
    protected var mTextLoading: String? = null //"正在加载...";
    protected var mTextRelease: String? = null //"释放立即刷新";
    protected var mTextFinish: String? = null //"刷新完成";
    protected var mTextFailed: String? = null //"刷新失败";
    protected var mTextUpdate: String? = null //"上次更新 M-d HH:mm";
    protected var mTextSecondary: String? = null //"释放进入二楼";


    init {
        inflate(context, R.layout.srl_classics_header, this)
        mArrowView = findViewById<ImageView>(R.id.srl_classics_arrow)
        mLastUpdateText = findViewById<TextView>(R.id.srl_classics_update)
        mProgressView = findViewById<ImageView>(R.id.srl_classics_progress)

        mTitleText = findViewById<TextView>(R.id.srl_classics_title)

        context.withStyledAttributes(attrs, R.styleable.ClassicsHeader) {

            val lpArrow = mArrowView?.layoutParams as? LayoutParams
            val lpProgress = mProgressView?.layoutParams as? LayoutParams
            val lpUpdateText = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            lpUpdateText.topMargin = getDimensionPixelSize(
                R.styleable.ClassicsHeader_srlTextTimeMarginTop,
                SmartUtil.dp2px(0f)
            )
            val marginRight=getDimensionPixelSize(
                R.styleable.ClassicsHeader_srlDrawableMarginRight,
                SmartUtil.dp2px(20f)
            )

            lpArrow?.let {
                it.rightMargin = marginRight
                it.width = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableArrowSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableArrowSize,
                    it.height
                )

                it.width = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableSize,
                    it.height
                )
            }

            lpProgress?.let {
                it.rightMargin = marginRight
                it.width = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableProgressSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableProgressSize,
                    it.height
                )

                it.width = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableSize,
                    it.width
                )
                it.height = getLayoutDimension(
                    R.styleable.ClassicsHeader_srlDrawableSize,
                    it.height
                )

            }

            mFinishDuration = getInt(R.styleable.ClassicsHeader_srlFinishDuration, mFinishDuration)
            mEnableLastTime =
                getBoolean(R.styleable.ClassicsHeader_srlEnableLastTime, mEnableLastTime)

            mSpinnerStyle = mSpinnerStyle?.let {
                SpinnerStyle.values[getInt(
                    R.styleable.ClassicsFooter_srlClassicsSpinnerStyle,
                    it.ordinal
                )]
            }

            if (hasValue(R.styleable.ClassicsHeader_srlDrawableArrow)) {
                mArrowView?.setImageDrawable(getDrawable(R.styleable.ClassicsHeader_srlDrawableArrow))
            } else if (mArrowView?.getDrawable() == null) {
                mArrowDrawable = ArrowDrawable()
                mArrowDrawable?.setColor(-0x99999a)
                mArrowView?.setImageDrawable(mArrowDrawable)
            }

            if (hasValue(R.styleable.ClassicsHeader_srlDrawableProgress)) {
                mProgressView?.setImageDrawable(getDrawable(R.styleable.ClassicsHeader_srlDrawableProgress))
            } else if (mProgressView?.getDrawable() == null) {
                mProgressDrawable = ProgressDrawable()
                mProgressDrawable?.setColor(-0x99999a)
                mProgressView?.setImageDrawable(mProgressDrawable)
            }

            if (hasValue(R.styleable.ClassicsHeader_srlTextSizeTitle)) {
                mTitleText?.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    getDimensionPixelSize(
                        R.styleable.ClassicsHeader_srlTextSizeTitle,
                        SmartUtil.dp2px(16f)
                    ).toFloat()
                )
            }

            if (hasValue(R.styleable.ClassicsHeader_srlTextSizeTime)) {
                mLastUpdateText.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    getDimensionPixelSize(
                        R.styleable.ClassicsHeader_srlTextSizeTime,
                        SmartUtil.dp2px(12f)
                    ).toFloat()
                )
            }

            if (hasValue(R.styleable.ClassicsHeader_srlPrimaryColor)) {
                super.setPrimaryColor(getColor(R.styleable.ClassicsHeader_srlPrimaryColor, 0))
            }
            if (hasValue(R.styleable.ClassicsHeader_srlAccentColor)) {
                setAccentColor(getColor(R.styleable.ClassicsHeader_srlAccentColor, 0))
            }

            mTextPulling = if (hasValue(R.styleable.ClassicsHeader_srlTextPulling)) {
                getString(R.styleable.ClassicsHeader_srlTextPulling)
            } else if (REFRESH_HEADER_PULLING != null) {
                REFRESH_HEADER_PULLING
            } else {
                context.getString(R.string.srl_header_pulling)
            }

            mTextLoading = if (hasValue(R.styleable.ClassicsHeader_srlTextLoading)) {
                getString(R.styleable.ClassicsHeader_srlTextLoading)
            } else if (REFRESH_HEADER_LOADING != null) {
                REFRESH_HEADER_LOADING
            } else {
                context.getString(R.string.srl_header_loading)
            }

            mTextRelease = if (hasValue(R.styleable.ClassicsHeader_srlTextRelease)) {
                getString(R.styleable.ClassicsHeader_srlTextRelease)
            } else if (REFRESH_HEADER_RELEASE != null) {
                REFRESH_HEADER_RELEASE
            } else {
                context.getString(R.string.srl_header_release)
            }

            mTextFinish = if (hasValue(R.styleable.ClassicsHeader_srlTextFinish)) {
                getString(R.styleable.ClassicsHeader_srlTextFinish)
            } else if (REFRESH_HEADER_FINISH != null) {
                REFRESH_HEADER_FINISH
            } else {
                context.getString(R.string.srl_header_finish)
            }

            mTextFailed = if (hasValue(R.styleable.ClassicsHeader_srlTextFailed)) {
                getString(R.styleable.ClassicsHeader_srlTextFailed)
            } else if (REFRESH_HEADER_FAILED != null) {
                REFRESH_HEADER_FAILED
            } else {
                context.getString(R.string.srl_header_failed)
            }

            mTextSecondary = if (hasValue(R.styleable.ClassicsHeader_srlTextSecondary)) {
                getString(R.styleable.ClassicsHeader_srlTextSecondary)
            } else if (REFRESH_HEADER_SECONDARY != null) {
                REFRESH_HEADER_SECONDARY
            } else {
                context.getString(R.string.srl_header_secondary)
            }

            mTextRefreshing = if (hasValue(R.styleable.ClassicsHeader_srlTextRefreshing)) {
                getString(R.styleable.ClassicsHeader_srlTextRefreshing)
            } else if (REFRESH_HEADER_REFRESHING != null) {
                REFRESH_HEADER_REFRESHING
            } else {
                context.getString(R.string.srl_header_refreshing)
            }

            mTextUpdate = if (hasValue(R.styleable.ClassicsHeader_srlTextUpdate)) {
                getString(R.styleable.ClassicsHeader_srlTextUpdate)
            } else if (REFRESH_HEADER_UPDATE != null) {
                REFRESH_HEADER_UPDATE
            } else {
                context.getString(R.string.srl_header_update)
            }
            mLastUpdateFormat = SimpleDateFormat(mTextUpdate, Locale.getDefault())

        }

        mProgressView?.animate()?.setInterpolator(null)
        mLastUpdateText.visibility = if (mEnableLastTime) VISIBLE else GONE
        mTitleText?.text = if (isInEditMode) mTextRefreshing else mTextPulling

        if (isInEditMode) {
            mArrowView?.setVisibility(GONE)
        } else {
            mProgressView?.setVisibility(GONE)
        }

        try { //try 不能删除-否则会出现兼容性问题
            if (context is FragmentActivity) {
                val manager = context.supportFragmentManager
                @SuppressLint("RestrictedApi") val fragments = manager.fragments
                if (fragments.isNotEmpty()) {
                    setLastUpdateTime(Date())
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        KEY_LAST_UPDATE_TIME += context.javaClass.getName()
        mShared = context.getSharedPreferences("ClassicsHeader", Context.MODE_PRIVATE)
        setLastUpdateTime(Date(mShared!!.getLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())))
    }

    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        if (success) {
            mTitleText?.text = mTextFinish
            if (mLastTime != null) {
                setLastUpdateTime(Date())
            }
        } else {
            mTitleText?.text = mTextFailed
        }
        return super.onFinish(refreshLayout, success) //延迟500毫秒之后再弹回
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
        val arrowView: View? = mArrowView
        val updateView: View = mLastUpdateText
        when (newState) {
            RefreshState.None -> {
                updateView.visibility = if (mEnableLastTime) VISIBLE else GONE
                mTitleText?.text = mTextPulling
                arrowView?.visibility = VISIBLE
                arrowView?.animate()?.rotation(0f)
            }

            RefreshState.PullDownToRefresh -> {
                mTitleText?.text = mTextPulling
                arrowView?.visibility = VISIBLE
                arrowView?.animate()?.rotation(0f)
            }

            RefreshState.Refreshing, RefreshState.RefreshReleased -> {
                mTitleText?.text = mTextRefreshing
                arrowView?.visibility = GONE
            }

            RefreshState.ReleaseToRefresh -> {
                mTitleText?.text = mTextRelease
                arrowView?.animate()?.rotation(180f)
            }

            RefreshState.ReleaseToTwoLevel -> {
                mTitleText?.text = mTextSecondary
                arrowView?.animate()?.rotation(0f)
            }

            RefreshState.Loading -> {
                arrowView?.visibility = GONE
                updateView.visibility = if (mEnableLastTime) INVISIBLE else GONE
                mTitleText?.text = mTextLoading
            }
            else->{

            }
        }
    }

    fun setLastUpdateTime(time: Date): ClassicsHeader {
        val thisView: View = this
        mLastTime = time
        /*
         * 时区修改后需要及时更新下拉刷新的时间否则出现下拉刷新的时间与系统时间不一致问题
         * 需要及时设置：mLastUpdateFormat.setCalendar
         * 感谢：github 用户 findviewbyid 贡献的代码
         * 贡献日期：2023-01-12
         */
        mLastUpdateFormat.setCalendar(
            Calendar.getInstance(
                TimeZone.getDefault(),
                Locale.getDefault()
            )
        )
        mLastUpdateText.text = mLastUpdateFormat.format(time)
        if (mShared != null && !thisView.isInEditMode) {
            mShared?.edit { putLong(KEY_LAST_UPDATE_TIME, time.time) }
        }
        return this
    }

    fun setTimeFormat(format: DateFormat): ClassicsHeader {
        mLastUpdateFormat = format
        mLastTime?.also {
            mLastUpdateText.text = mLastUpdateFormat.format(it)
        }
        return this
    }

    fun setLastUpdateText(text: CharSequence?): ClassicsHeader {
        mLastTime = null
        mLastUpdateText.text = text
        return this
    }

    public override fun setAccentColor(@ColorInt accentColor: Int): ClassicsHeader? {
        mLastUpdateText.setTextColor(accentColor and 0x00ffffff or -0x34000000)
        return super.setAccentColor(accentColor)
    }

    fun setEnableLastTime(enable: Boolean): ClassicsHeader {
        val updateView: View = mLastUpdateText
        mEnableLastTime = enable
        updateView.visibility = if (enable) VISIBLE else GONE
        mRefreshKernel?.requestRemeasureHeightFor(this)
        return this
    }

    fun setTextSizeTime(size: Float): ClassicsHeader {
        mLastUpdateText.textSize = size
        mRefreshKernel?.requestRemeasureHeightFor(this)
        return this
    }

    fun setTextSizeTime(unit: Int, size: Float): ClassicsHeader {
        mLastUpdateText.setTextSize(unit, size)
        mRefreshKernel?.requestRemeasureHeightFor(this)
        return this
    }

    fun setTextTimeMarginTop(dp: Float): ClassicsHeader {
        val updateView: View = mLastUpdateText
        val lp = updateView.layoutParams as MarginLayoutParams
        lp.topMargin = SmartUtil.dp2px(dp)
        updateView.setLayoutParams(lp)
        return this
    }

    fun setTextTimeMarginTopPx(px: Int): ClassicsHeader {
        val lp = mLastUpdateText.layoutParams as MarginLayoutParams
        lp.topMargin = px
        mLastUpdateText.setLayoutParams(lp)
        return this
    }

    companion object {
        val ID_TEXT_UPDATE: Int = R.id.srl_classics_update

        var REFRESH_HEADER_PULLING: String? = null //"下拉可以刷新";
        var REFRESH_HEADER_REFRESHING: String? = null //"正在刷新...";
        var REFRESH_HEADER_LOADING: String? = null //"正在加载...";
        var REFRESH_HEADER_RELEASE: String? = null //"释放立即刷新";
        var REFRESH_HEADER_FINISH: String? = null //"刷新完成";
        var REFRESH_HEADER_FAILED: String? = null //"刷新失败";
        var REFRESH_HEADER_UPDATE: String? = null //"上次更新 M-d HH:mm";
        var REFRESH_HEADER_SECONDARY: String? = null //"释放进入二楼";
    }
}
