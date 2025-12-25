package com.kernelflux.aether.ui.widget.tab

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.kernelflux.aether.common.ColorHelper
import com.kernelflux.aether.common.HandlerUtils
import com.kernelflux.aether.common.WindowInsetsProvider
import com.kernelflux.aether.common.dp
import com.kernelflux.aether.common.getPixelSizeInt
import com.kernelflux.aether.ui.R
import java.lang.ref.WeakReference
import java.util.regex.Pattern


class HomeTabBottomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private var mHomeTabClickReference: WeakReference<HomeTabClickListener>? = null
    private var mSelectListenerReferenceIHome: WeakReference<IHomeTabSelectListener>? = null
    private var mEngTextSize = 0.0f
    private var mTextSize = 0f
    private var mTextColorNormal = 0
    private var mTextColorSelected = 0
    private var mDrawablePadding = 0
    private lateinit var mPaint: Paint
    private val mTabHolderList = ArrayList<TabHolder>()
    private var mCurrentIndex = 0
    private var mPressedIndex = -1
    private var mAnimatorFactor = 1.0f
    private var tabBackgroundColor: Int = 0
    private var mCallSelectChangedAfterDraw = false
    private var mIsVipConfigured: Boolean = false
    private var tabVipBackgroundColor = -1
    private var tabVipLastBackgroundColor = -1
    private var targetWidthScale = 70

    private val mAnimatorUpdateListener: ValueAnimator.AnimatorUpdateListener =
        ValueAnimator.AnimatorUpdateListener { animation ->
            this@HomeTabBottomView.mAnimatorFactor = animation.animatedValue as Float
            this@HomeTabBottomView.clearPressState()
            this@HomeTabBottomView.postInvalidate()
        }

    private val mGestureDetector: GestureDetector = GestureDetector(
        getContext(),
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(motionEvent: MotionEvent): Boolean {
                val touchTabIndex = this@HomeTabBottomView.touchIndex(motionEvent.x)
                if (this@HomeTabBottomView.isTouchIndexValid(touchTabIndex)) {
                    val homeTabData = this@HomeTabBottomView.mTabHolderList[touchTabIndex]
                    if (this@HomeTabBottomView.mCurrentIndex != touchTabIndex) {
                        this@HomeTabBottomView.mPressedIndex = touchTabIndex
                        this@HomeTabBottomView.clearPressState()
                        homeTabData.pressState = true
                        this@HomeTabBottomView.postInvalidate()
                    }
                }
                return super.onDown(motionEvent)
            }

            override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
                val touchTabIndex = this@HomeTabBottomView.touchIndex(motionEvent.x)
                val homeTabClickListener: HomeTabClickListener? =
                    this@HomeTabBottomView.getTabClickListener()
                if (this@HomeTabBottomView.isTouchIndexValid(touchTabIndex) && homeTabClickListener != null) {
                    homeTabClickListener.onDoubleClick(touchTabIndex)
                }
                return super.onDoubleTap(motionEvent)
            }
        }
    )

    init {
        attrs?.also {
            val obtainAttributes =
                resources.obtainAttributes(it, R.styleable.HomeTabBottomViewStyle)
            this.mTextSize = obtainAttributes.getDimension(
                R.styleable.HomeTabBottomViewStyle_textSize,
                0f
            )
            this.mTextColorNormal = obtainAttributes.getColor(
                R.styleable.HomeTabBottomViewStyle_textColorNormal,
                0
            )
            this.mTextColorSelected = obtainAttributes.getColor(
                R.styleable.HomeTabBottomViewStyle_textColorSelected,
                0
            )
            this.mDrawablePadding = obtainAttributes.getDimension(
                R.styleable.HomeTabBottomViewStyle_imageTextPadding,
                0f
            ).toInt()
            obtainAttributes.recycle()
        }
        if (this.mTextSize <= 0f) {
            this.mTextSize = 12f
        }
        this.mEngTextSize = 11f.dp.toFloat()
        if (this.mDrawablePadding == 0) {
            this.mDrawablePadding = 4f.dp
        }
        initPaint()
        onTouchListener()
    }

    private fun initPaint() {
        this.mPaint = Paint()
        this.mPaint.apply {
            isAntiAlias = true
            textSize = this@HomeTabBottomView.mTextSize
        }
    }

    private fun onTouchListener() {
        setOnTouchListener(object : OnTouchListener {

            var downIndex = -1

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downIndex = this@HomeTabBottomView.touchIndex(event.x)
                        if (event.y < EXTEND_HEIGHT) {
                            return false
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        val touchTabIndex = this@HomeTabBottomView.touchIndex(event.x)
                        if (this@HomeTabBottomView.isTouchIndexValid(touchTabIndex)) {
                            if (this@HomeTabBottomView.mPressedIndex == touchTabIndex) {
                                this@HomeTabBottomView.onSingleTab(touchTabIndex)
                            }
                            if (downIndex == this@HomeTabBottomView.mCurrentIndex) {
                                this@HomeTabBottomView.onTabSelectCallback()
                            }
                        }
                        downIndex = -1
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val touchTabIndex = this@HomeTabBottomView.touchIndex(event.x)
                        if (this@HomeTabBottomView.isTouchIndexValid(touchTabIndex) &&
                            ((touchTabIndex != this@HomeTabBottomView.mPressedIndex || event.y < EXTEND_HEIGHT) && this@HomeTabBottomView.mPressedIndex != -1)
                        ) {
                            this@HomeTabBottomView.mTabHolderList[this@HomeTabBottomView.mPressedIndex].pressState =
                                false
                            this@HomeTabBottomView.mPressedIndex = -1
                            this@HomeTabBottomView.postInvalidate()
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (this@HomeTabBottomView.mPressedIndex != -1 && this@HomeTabBottomView.isTouchIndexValid(
                                this@HomeTabBottomView.mPressedIndex
                            )
                        ) {
                            this@HomeTabBottomView.mTabHolderList[this@HomeTabBottomView.mPressedIndex].pressState =
                                false
                            this@HomeTabBottomView.mPressedIndex = -1
                            this@HomeTabBottomView.postInvalidate()
                        }
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        val touchTabIndex = this@HomeTabBottomView.touchIndex(event.x)
                        if (this@HomeTabBottomView.isTouchIndexValid(touchTabIndex)) {
                            val touchHomeTabData =
                                this@HomeTabBottomView.mTabHolderList[touchTabIndex]
                            if (touchHomeTabData.pressState) {
                                touchHomeTabData.pressState = false
                                this@HomeTabBottomView.postInvalidate()
                            }
                        }
                    }
                }
                this@HomeTabBottomView.mGestureDetector.onTouchEvent(event)
                return true
            }
        })
    }

    private fun touchIndex(touchX: Float): Int {
        return if (touchX > 0) {
            (touchX.toInt() - 1) / (measuredWidth / this.mTabHolderList.size)
        } else {
            0
        }
    }

    fun getSize(): Int {
        return this.mTabHolderList.size
    }

    fun getTabName(index: Int): String {
        val tabName: String? = getTabHolder(index)?.tabName
        return tabName ?: ""
    }

    fun setTabData(arrayList: ArrayList<HomeTabDataWrapper>?) {
        if (!arrayList.isNullOrEmpty()) {
            for (homeTabDataWrapper in arrayList) {
                val tabHolder = TabHolder()

                val tabIcon = homeTabDataWrapper.getHomeTabIcon()
                tabHolder.mDrawableSelected = tabIcon.selectedDrawable
                tabHolder.mDrawableNormal = tabIcon.normalDrawable
                tabHolder.iconWidth = tabIcon.width
                tabHolder.iconHeight = tabIcon.height

                val tabData = homeTabDataWrapper.getHomeTabData()

                tabHolder.tabName = tabData.tabName
                tabHolder.isEngText = isEngText(tabData.tabName)
                tabHolder.textSelectColor = ColorHelper.parseColor(
                    tabData.selectColor,
                    mTextColorSelected
                )
                tabHolder.textUnSelectColor = ColorHelper.parseColor(
                    tabData.unSelectColor,
                    mTextColorNormal
                )
                tabHolder.hasLottie = !TextUtils.isEmpty(tabData.lottieUrl)
                tabHolder.pageType = tabData.pageType

                this.mTabHolderList.add(tabHolder)
            }
        }
        this.targetWidthScale = this.mTabHolderList.size * 70 / 5
        postInvalidate()
    }

    private fun isEngText(str: String): Boolean {
        return try {
            val compile: Pattern? = Pattern.compile("[0-9a-zA-Z]*")
            compile?.matcher(str)?.matches() ?: false
        } catch (unused: java.lang.Exception) {
            false
        }
    }

    fun setTabVipBackgroundColor(colorStr: String, refresh: Boolean) {
        if (refresh) {
            this.tabVipBackgroundColor = parseColor(colorStr)
            this.tabVipLastBackgroundColor = this.tabVipBackgroundColor
            invalidate()
            return
        }
        this.tabVipLastBackgroundColor = parseColor(colorStr)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            EXTEND_HEIGHT + CONTENT_HEIGHT
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (this.mTabHolderList.isNotEmpty()) {
            drawBackgroundColor(canvas)
            drawVipBackgroundColor(canvas)
            drawBackground(canvas)
            drawTabItems(canvas)

            if (this.mCallSelectChangedAfterDraw) {
                this.mCallSelectChangedAfterDraw = false
                post {
                    this@HomeTabBottomView.onTabSelectCallback()
                }
            }
        }
    }

    private fun drawVipBackgroundColor(canvas: Canvas?) {
        if (this.tabVipBackgroundColor != -1) {
            this.mPaint.color = this.tabVipBackgroundColor
            canvas?.drawRect(
                0.0f,
                EXTEND_HEIGHT.toFloat(),
                measuredWidth.toFloat(),
                measuredHeight.toFloat(),
                this.mPaint
            )
        }
    }

    fun setVipConfig(config: Boolean) {
        if (this.mIsVipConfigured != config) {
            this.mIsVipConfigured = config
            postInvalidate()
        }
    }


    fun recoverBottomTabVipColor() {
        this.tabVipBackgroundColor = this.tabVipLastBackgroundColor
        invalidate()
    }

    fun removeBottomTabVipColor() {
        if (this.tabVipBackgroundColor != -1) {
            this.tabVipLastBackgroundColor = this.tabVipBackgroundColor
        }
        this.tabVipBackgroundColor = -1
        invalidate()
    }

    private fun drawBackgroundColor(canvas: Canvas?) {
        if (!this.mCallSelectChangedAfterDraw || this.tabBackgroundColor == -1) {
            this.tabBackgroundColor = ContextCompat.getColor(context, android.R.color.white)
        }
        this.mPaint.color = this.tabBackgroundColor
        canvas?.drawRect(
            0.0f,
            EXTEND_HEIGHT.toFloat(),
            measuredWidth.toFloat(),
            measuredHeight.toFloat(),
            this.mPaint
        )
    }

    private fun drawBackground(canvas: Canvas) {
        try {
            this.mPaint.color = ContextCompat.getColor(
                context,
                R.color.component_tab_bottom_view_bg_color
            )
            if (this.tabBackgroundColor == -1 && this.tabVipBackgroundColor == -1) {
                canvas.drawLine(
                    0.0f,
                    EXTEND_HEIGHT.toFloat(),
                    measuredWidth.toFloat(),
                    EXTEND_HEIGHT.toFloat(),
                    this.mPaint
                )
            }
            getBackgroundDrawable()?.apply {
                setBounds(0, EXTEND_HEIGHT, measuredWidth, measuredHeight)
                draw(canvas)
            }

        } catch (e: Exception) {
            //
        }
    }

    private fun drawTabItems(canvas: Canvas) {
        val sMeasureWidth = measuredWidth / this.mTabHolderList.size
        for (index in 0 until this.mTabHolderList.size) {
            if (!this.mTabHolderList[index].hasExtendData) {
                drawTab(sMeasureWidth, index, canvas)
            }
        }
    }

    private fun drawTab(tabWidth: Int, tabIndex: Int, canvas: Canvas) {
        if (getPageType(tabIndex) != 2 || !this.mIsVipConfigured) {
            val left = tabWidth * tabIndex
            val top = EXTEND_HEIGHT + 1
            if (drawImageText(tabIndex, tabWidth, left, top, canvas) != null) {
                drawAnimationView(tabWidth, tabIndex, canvas, left)
                drawRedDotView(tabIndex, tabWidth, left, top, canvas)
            }
        }
    }


    private fun drawRedDotView(
        tabIndex: Int,
        tabWidth: Int,
        left: Int,
        top: Int,
        canvas: Canvas?,
    ) {
        val tabHolder = getTabHolder(tabIndex)
        if (tabHolder != null) {
            val msgNum = tabHolder.msgNum
            if (msgNum > 0) {
                val formatMsgNum = formatMsgNum(msgNum)
                var measureText: Float = this.mPaint.measureText(formatMsgNum) + 2 * BUBBLE_PADDING
                if (measureText < BUBBLE_HEIGHT) {
                    measureText = BUBBLE_HEIGHT.toFloat()
                }
                val redDotNumberRectF = getRedDotNumberRectF(
                    tabWidth,
                    left,
                    top,
                    measureText
                )
                this.mPaint.color = ContextCompat.getColor(context, R.color.component_red_dot_color)
                canvas?.drawRoundRect(
                    redDotNumberRectF,
                    BUBBLE_HEIGHT / 2f,
                    BUBBLE_HEIGHT / 2f,
                    this.mPaint
                )
                val typeface = this.mPaint.typeface
                this.mPaint.color = ContextCompat.getColor(context, android.R.color.white)
                this.mPaint.textSize = RED_DOT_TEXT_SIZE.toFloat()
                val fontMetricsInt = this.mPaint.fontMetricsInt
                canvas?.drawText(
                    formatMsgNum,
                    redDotNumberRectF.left + measureText / 2.0f - (this.mPaint.measureText(
                        formatMsgNum
                    ) / 2.0f),
                    redDotNumberRectF.top + BUBBLE_HEIGHT / 2.0f - (fontMetricsInt.bottom + fontMetricsInt.top) / 2.0f,
                    this.mPaint
                )
                this.mPaint.typeface = typeface
            } else if (msgNum == 0) {
                val msgCircleLeft = left + tabWidth / 2.0f + RED_DOT_LEFT_DISTANCE
                val msgDotPointX = msgCircleLeft + RED_DOT_RADIUS
                val msgDotPointY = (top + RED_DOT_TOP_DISTANCE + RED_DOT_RADIUS).toFloat()
                this.mPaint.color = ContextCompat.getColor(
                    context,
                    R.color.component_red_dot_color
                )
                canvas?.drawCircle(
                    msgDotPointX,
                    msgDotPointY,
                    RED_DOT_RADIUS.toFloat(),
                    this.mPaint
                )
            }
        }
    }

    private fun getRedDotNumberRectF(i2: Int, i3: Int, i4: Int, f2: Float): RectF {
        val rectF = RectF()
        rectF.left = i3.toFloat() + i2.toFloat() * 1.0f / 2.0f + BUBBLE_LEFT_DISTANCE.toFloat()
        rectF.right = rectF.left + f2
        rectF.top = (i4 + BUBBLE_TOP_DISTANCE).toFloat()
        rectF.bottom = rectF.top + BUBBLE_HEIGHT.toFloat()
        return rectF
    }

    private fun drawAnimationView(tabWidth: Int, tabIndex: Int, canvas: Canvas, left: Int): Int {
        val tabHolder = getTabHolder(tabIndex)
        val animDrawable = tabHolder?.getAnimationDrawable()
        if (animDrawable == null || animDrawable.intrinsicWidth <= 0 || animDrawable.intrinsicHeight <= 0) {
            return 0
        }
        try {
            if (tabIndex == this.mCurrentIndex && !tabHolder.pressState) {
                val sMeasureHeight = measuredHeight
                var intrinsicWidth = this.targetWidthScale * tabWidth / 100
                var intrinsicHeight =
                    animDrawable.intrinsicHeight * intrinsicWidth / animDrawable.intrinsicWidth
                if (intrinsicHeight > sMeasureHeight) {
                    intrinsicWidth =
                        animDrawable.intrinsicWidth * sMeasureHeight / animDrawable.intrinsicHeight
                    intrinsicHeight = sMeasureHeight
                }
                val animDrawableWidth = intrinsicWidth * this.mAnimatorFactor.toInt()
                val animDrawableLeft = (tabWidth - animDrawableWidth) / 2 + left
                val animDrawableTop =
                    sMeasureHeight - intrinsicHeight * this.mAnimatorFactor.toInt()
                val animDrawableRight = animDrawableLeft + animDrawableWidth

                animDrawable.setBounds(
                    animDrawableLeft,
                    animDrawableTop,
                    animDrawableRight,
                    sMeasureHeight
                )
                animDrawable.draw(canvas)
                return animDrawableRight
            }
        } catch (e: Exception) {
            //
        }
        return 0
    }

    private fun drawImageText(
        tabIndex: Int,
        tabWidth: Int,
        left: Int,
        top: Int,
        canvas: Canvas?,
    ): Rect? {
        val tabHolder: TabHolder = getTabHolder(tabIndex) ?: return null
        val isSelected = tabIndex == this.mCurrentIndex || tabHolder.pressState
        val drawable: Drawable =
            (if (isSelected) tabHolder.getSelectedDrawable() else tabHolder.getNormalDrawable())
                ?: return null
        val drawableWidth = tabHolder.iconWidth
        var drawableHeight = tabHolder.iconHeight
        if (drawableHeight == 0) {
            drawableHeight = drawable.intrinsicHeight * drawableWidth / drawable.intrinsicWidth
        }
        val drawableLeft = (tabWidth - drawableWidth) / 2 + left
        val drawableTop =
            (((measuredHeight - EXTEND_HEIGHT) - (this.mTextSize + this.mDrawablePadding + drawableHeight)) / 2 + top).toInt()
        val drawableRight = drawableLeft + drawableWidth
        val drawableBottom = drawableTop + drawableHeight

        tabHolder.iconLeft = drawableLeft
        tabHolder.iconTop = drawableTop
        tabHolder.iconRight = drawableRight
        tabHolder.iconBottom = drawableBottom

        onTabRectChanged(tabHolder, tabIndex)

        if (tabIndex == this.mCurrentIndex && tabHolder.getAnimationDrawable() == null && tabHolder.hasLottie) {
            drawText(
                tabIndex,
                tabWidth,
                left,
                canvas,
                tabHolder,
                drawableBottom
            )
        } else if (tabIndex != this.mCurrentIndex || tabHolder.getAnimationDrawable() == null || this.mAnimatorFactor < 1.0f) {
            drawImage(
                canvas,
                drawable,
                drawableLeft,
                drawableTop,
                drawableRight,
                drawableBottom
            )
            drawText(
                tabIndex,
                tabWidth,
                left,
                canvas,
                tabHolder,
                drawableBottom
            )
        }
        return Rect(drawableLeft, drawableTop, drawableRight, drawableBottom)
    }

    private fun getIconColor(): Int {
        return 0
    }

    private fun getCurrentPageType(): Int {
        return getPageType(this.mCurrentIndex)
    }

    fun setCurrentPageType(pageType: Int) {
        var index = 0
        while (true) {
            val tempIndex = index
            if (tempIndex >= this.mTabHolderList.size) {
                return
            }
            if (this.mTabHolderList[tempIndex].pageType == pageType) {
                setCurrentIndex(tempIndex)
                return
            }
            index = tempIndex + 1
        }
    }

    fun getIndex(pageType: Int): Int {
        for (i3 in mTabHolderList.indices) {
            if (mTabHolderList[i3].pageType == pageType) {
                return i3
            }
        }
        return -1
    }

    fun setCurrentIndex(index: Int) {
        if (isTouchIndexValid(index) && this.mCurrentIndex != index) {
            this.mCurrentIndex = index
            if (canDoAnimation(this.mCurrentIndex)) {
                startTabSelectAnim()
            } else {
                postInvalidate()
            }
        }
        this.mCallSelectChangedAfterDraw = true
    }

    fun getCurrentIndex(): Int {
        return this.mCurrentIndex
    }

    private fun drawImage(
        canvas: Canvas?,
        drawable: Drawable,
        drawableLeft: Int,
        drawableTop: Int,
        drawableRight: Int,
        drawableBottom: Int,
    ) {
        canvas?.also {
            try {
                val iconColor = getIconColor()
                if (iconColor != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        drawable.colorFilter = BlendModeColorFilter(iconColor, BlendMode.SRC_ATOP)
                    } else {
                        drawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
                    }
                } else {
                    drawable.clearColorFilter()
                }
                drawable.setBounds(drawableLeft, drawableTop, drawableRight, drawableBottom)
                drawable.draw(it)
            } catch (e: Exception) {
                //
            }
        }
    }

    private fun drawText(
        tabIndex: Int,
        tabWidth: Int,
        left: Int,
        canvas: Canvas?,
        tabHolder: TabHolder,
        drawableBottom: Int,
    ) {
        if (tabHolder.isEngText) {
            this.mPaint.textSize = this.mEngTextSize
        } else {
            this.mPaint.textSize = this.mTextSize
        }
        this.mPaint.color = getTabTextColor(tabIndex, tabHolder)
        val measureText: Float = this.mPaint.measureText(tabHolder.tabName)
        val fontMetricsInt = this.mPaint.fontMetricsInt
        canvas?.drawText(
            tabHolder.tabName,
            ((tabWidth - measureText).toInt() / 2 + left).toFloat(),
            ((this.mDrawablePadding + drawableBottom) * 2 + this.mTextSize - fontMetricsInt.bottom - fontMetricsInt.top) / 2,
            this.mPaint
        )
    }

    private fun getTabTextColor(
        tabIndex: Int,
        tabHolder: TabHolder,
    ): Int {
        return if (tabIndex == this.mCurrentIndex || tabHolder.pressState) {
            tabHolder.textSelectColor
        } else {
            tabHolder.textUnSelectColor
        }
    }

    private fun onTabRectChanged(
        tabHolder: TabHolder,
        selectIndex: Int,
    ) {
        HandlerUtils.post {
            val tabSelectSelectListener = this@HomeTabBottomView.getTabSelectListener()
            tabSelectSelectListener?.onTabSelected(
                selectIndex,
                tabHolder.iconLeft,
                tabHolder.iconTop,
                tabHolder.iconRight,
                tabHolder.iconBottom
            )
        }
    }

    fun getPageType(index: Int): Int {
        val homeTabData = getTabHolder(index)
        return homeTabData?.pageType ?: -1
    }

    fun getTabHolder(index: Int): TabHolder? {
        if (index < 0 || index >= this.mTabHolderList.size) {
            return null
        }
        return this.mTabHolderList[index]
    }

    private fun getBackgroundDrawable(): Drawable? {
        return null
    }


    fun setTabBackgroundColor(colorStr: String?) {
        this.tabBackgroundColor = parseColor(colorStr)
        this.mCallSelectChangedAfterDraw = true
        postInvalidate()
    }

    override fun postInvalidate() {
        Log.d(TAG, "postInvalidate is Called ...")
        super.postInvalidate()
    }

    fun resetTabBackgroundColor() {
        this.tabBackgroundColor = -1
        this.mCallSelectChangedAfterDraw = false
        postInvalidate()
    }

    private fun parseColor(colorStr: String?): Int {
        if (TextUtils.isEmpty(colorStr)) {
            return -1
        }
        return try {
            Color.parseColor(colorStr)
        } catch (exp: Exception) {
            -1
        }
    }

    private fun onSingleTab(index: Int) {
        onTouchUp(index)
        val tabClickListener = getTabClickListener()
        tabClickListener?.onSingleClick(this.mCurrentIndex)
    }

    private fun onTabSelectCallback() {
        val tabHolder = this.mTabHolderList[this.mCurrentIndex]
        val tabSelectListener = getTabSelectListener()
        tabSelectListener?.onTabSelected(
            this.mCurrentIndex,
            tabHolder.iconLeft,
            tabHolder.iconTop,
            tabHolder.iconRight,
            tabHolder.iconBottom
        )
    }


    fun onTabTipsUpdate(pageType: Int, msgNum: Int) {
        updateTipsInfo(pageType, msgNum)
        postInvalidate()
    }

    private fun updateTipsInfo(pageType: Int, msgNum: Int) {
        if (getIndex(pageType) != -1) {
            this.mTabHolderList[getIndex(pageType)].msgNum = msgNum
        }
    }

    private fun onTouchUp(index: Int) {
        clearPressState()
        var tabIndexChanged = false
        if (index != this.mCurrentIndex) {
            this.mCurrentIndex = index
            if (canDoAnimation(this.mCurrentIndex)) {
                startTabSelectAnim()
            } else {
                tabIndexChanged = true
            }
        }
        if (tabIndexChanged) {
            postInvalidate()
        }
        this@HomeTabBottomView.mPressedIndex = -1
    }


    @SuppressLint("Recycle")
    private fun startTabSelectAnim() {
        val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator?.apply {
            duration = 200
            addUpdateListener(this@HomeTabBottomView.mAnimatorUpdateListener)
            start()
        }
    }


    private fun canDoAnimation(index: Int): Boolean {
        val drawable = this.mTabHolderList[index].mDrawableStarTheme
        return drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0
    }

    private fun clearPressState() {
        val iterator = this.mTabHolderList.iterator()
        while (iterator.hasNext()) {
            iterator.next().pressState = false
        }
    }


    private fun isTouchIndexValid(index: Int): Boolean {
        return index >= 0 && index < this.mTabHolderList.size
    }

    fun setHomeTabClickListener(listenerHome: HomeTabClickListener?) {
        listenerHome?.also {
            this.mHomeTabClickReference = WeakReference(it)
        }
    }

    fun setHomeTabSelectListener(listenerIHome: IHomeTabSelectListener?) {
        listenerIHome?.also {
            this.mSelectListenerReferenceIHome = WeakReference(it)
        }
    }

    fun getTabClickListener(): HomeTabClickListener? {
        return this.mHomeTabClickReference?.get()
    }

    fun getTabSelectListener(): IHomeTabSelectListener? {
        return this.mSelectListenerReferenceIHome?.get()
    }

    fun getContentHeight(): Int {
        return CONTENT_HEIGHT
    }


    fun formatMsgNum(i2: Int): String {
        return if (i2 > 0) {
            if (i2 <= 99) i2.toString() else "99+"
        } else ""
    }


    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (measuredWidth != WindowInsetsProvider.from(context).getRealSize().x) {
            requestLayout()
        }
    }


    interface HomeTabClickListener {
        fun onSingleClick(index: Int)
        fun onDoubleClick(index: Int)
    }

    interface IHomeTabSelectListener {
        fun onTabSelected(
            selectedIndex: Int,
            drawableLeft: Int,
            drawableTop: Int,
            drawableRight: Int,
            drawableBottom: Int,
        )

        fun onTabSizeChanged(
            selectedIndex: Int,
            drawableLeft: Int,
            drawableTop: Int,
            drawableRight: Int,
            drawableBottom: Int,
        )
    }

    class TabHolder {
        var tabName: String = ""
        var mDrawableSelected: Drawable? = null
        var mDrawableNormal: Drawable? = null
        var mDrawableStarTheme: Drawable? = null
        var iconWidth: Int = 0
        var iconHeight: Int = 0
        var pressState: Boolean = false
        var hasLottie: Boolean = false
        var iconLeft: Int = 0
        var iconTop: Int = 0
        var iconRight: Int = 0
        var iconBottom: Int = 0
        var pageType: Int = 0
        var msgNum: Int = -1
        var isEngText: Boolean = false
        var textSelectColor: Int = 0
        var textUnSelectColor: Int = 0
        var hasExtendData: Boolean = false


        fun getAnimationDrawable(): Drawable? {
            return this.mDrawableStarTheme
        }

        fun getNormalDrawable(): Drawable? {
            return this.mDrawableNormal
        }

        fun getSelectedDrawable(): Drawable? {
            return this.mDrawableSelected
        }
    }

    class VipConfig {
        var backgroundColor = 0
        var backgroundDrawable: Drawable? = null
        var isConfigValid = false
        var tabIconColor = 0
        var tabTextColor = 0

        init {
            this.isConfigValid = false
        }
    }

    companion object {
        val BUBBLE_HEIGHT = getPixelSizeInt(R.dimen.base_dp_14)
        val BUBBLE_LEFT_DISTANCE = getPixelSizeInt(R.dimen.base_dp_8)
        val BUBBLE_PADDING = getPixelSizeInt(R.dimen.base_dp_3)
        val BUBBLE_TOP_DISTANCE = getPixelSizeInt(R.dimen.base_dp_6)
        val CONTENT_HEIGHT = getPixelSizeInt(R.dimen.base_dp_50)
        val EXTEND_HEIGHT = getPixelSizeInt(R.dimen.base_dp_25)
        val RED_DOT_LEFT_DISTANCE = getPixelSizeInt(R.dimen.base_dp_14)
        val RED_DOT_RADIUS = getPixelSizeInt(R.dimen.base_dp_3)
        val RED_DOT_TEXT_SIZE = getPixelSizeInt(R.dimen.base_dp_9)
        val RED_DOT_TOP_DISTANCE = getPixelSizeInt(R.dimen.base_dp_6)
        private val TAG = HomeTabBottomView::class.java.simpleName

    }


}