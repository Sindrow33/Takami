package com.mangareader.reader.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.compose.ui.graphics.toArgb
import com.mangareader.reader.engine.gesture.PeekGestureController
import com.mangareader.reader.engine.gesture.PeekPhase
import com.mangareader.reader.engine.gesture.TapAction
import com.mangareader.reader.engine.gesture.TapZoneScheme
import com.mangareader.reader.engine.gesture.TapZones
import com.mangareader.reader.engine.layout.FeedItem
import com.mangareader.reader.engine.layout.PagedNavigator
import com.mangareader.reader.engine.layout.SpreadPairing
import dev.takami.app.ui.theme.Aurora
import kotlin.math.abs

/**
 * Постраничное чтение: одна страница на экран, листание смахиванием и
 * тапом по боковым зонам.
 *
 * Существует отдельно от [WebtoonFeedView] потому, что это другой
 * способ показа, а не настройка того же. Лента непрерывна и меряется в
 * пикселях прокрутки; здесь единица — страница, между страницами есть
 * граница, а горизонтальный жест означает перелист, а не панораму.
 * Попытка совместить оба режима в одной вьюхе даёт ветвление в каждом
 * методе касания, и именно там ошибки направления не видны.
 *
 * Режимы RTL и LTR отличаются ровно одним: знаком, с которым жест и
 * боковые зоны переводятся в шаг по ленте. Порядок самой ленты не
 * переворачивается никогда — иначе направление инвертировалось бы
 * дважды и вернулось к исходному.
 */
class PagedReaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface PageBitmapProvider {
        fun bitmapFor(item: FeedItem.Page): Bitmap?
    }

    var items: List<FeedItem> = emptyList()
        set(value) {
            field = value
            if (currentIndex !in value.indices) {
                currentIndex = PagedNavigator.stops(value).firstOrNull() ?: 0
            }
            invalidate()
        }

    var bitmapProvider: PageBitmapProvider? = null
    var onViewportSettled: ((centerGlobalIndex: Int) -> Unit)? = null
    var onTap: (() -> Unit)? = null
    var onChapterCrossed: (() -> Unit)? = null
    var tapZoneScheme: TapZoneScheme = TapZoneScheme.L_SHAPE

    /** Направление чтения: зеркалит и жест, и боковые зоны. */
    var isRtl: Boolean = false

    /**
     * Разворот на две страницы. Включённая настройка не означает, что
     * разворот виден: в портрете он не применяется — две страницы рядом
     * дали бы полосу вдвое уже экрана.
     */
    var doubleSpreadEnabled: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var currentIndex: Int = 0
        private set

    private val scroller = OverScroller(context)
    private val peek = PeekGestureController()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val backgroundPaint = Paint().apply { color = Aurora.Surface.toArgb() }
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    private var velocityTracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var dragX = 0f
    private var isDragging = false
    private var holdRunnable: Runnable? = null

    /**
     * Смещение анимации перелистывания в пикселях. Ноль — страница на
     * месте; положительное значение сдвигает её вправо.
     */
    private var offsetX = 0f

    /**
     * Действует ли разворот прямо сейчас. Пересчитывается на каждом
     * кадре: поворот экрана меняет ответ, и отдельного уведомления об
     * этом вьюхе не приходит.
     */
    private fun spreadActive(): Boolean =
        SpreadPairing.applies(doubleSpreadEnabled, width, height)

    private fun spreads(): List<SpreadPairing.Spread> = SpreadPairing.pair(
        items.map { item ->
            val page = item as? FeedItem.Page
            val bitmap = page?.let { bitmapProvider?.bitmapFor(it) }
            SpreadPairing.Slot(
                isPage = page != null,
                // Ширина берётся из битмапа, а не из PageRef: источник
                // размеры отдаёт не всегда, а разворот художника надо
                // распознать до того, как он окажется ужат вдвое.
                isWide = bitmap != null && bitmap.width > bitmap.height,
                chapterId = page?.chapterId,
            )
        },
        rtl = isRtl,
    )

    fun showIndex(globalIndex: Int) {
        if (globalIndex !in items.indices) return
        scroller.forceFinished(true)
        currentIndex = globalIndex
        offsetX = 0f
        invalidate()
        onViewportSettled?.invoke(currentIndex)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        if (items.isEmpty() || width == 0 || height == 0) return

        if (spreadActive()) {
            drawSpreadAt(canvas, currentIndex, offsetX)
        } else {
            drawPageAt(canvas, currentIndex, offsetX)
        }

        /*
         * Соседняя страница подрисовывается только во время жеста, и
         * именно та, что въезжает. Рисовать обе всегда — значит
         * декодировать вдвое больше битмапов на каждый кадр покоя.
         */
        if (offsetX != 0f) {
            val incomingForward = offsetX < 0 != isRtl
            val neighbour = PagedNavigator.step(items, currentIndex, forward = incomingForward)
            if (neighbour != null) {
                val shift = if (offsetX < 0) width.toFloat() else -width.toFloat()
                if (spreadActive()) {
                    drawSpreadAt(canvas, neighbour, offsetX + shift)
                } else {
                    drawPageAt(canvas, neighbour, offsetX + shift)
                }
            }
        }
    }

    /**
     * Рисует экран целиком в режиме разворота: одну или две страницы.
     *
     * Половина экрана на страницу, а не «сколько влезет»: иначе соседние
     * листы разной ширины стоят с разной высотой, и линия кадра между
     * ними ломается.
     */
    private fun drawSpreadAt(canvas: Canvas, index: Int, dx: Float) {
        val all = spreads()
        val position = SpreadPairing.spreadOf(all, index)
        val spread = position?.let { all[it] }
        val second = spread?.second
        if (spread == null || second == null) {
            drawPageAt(canvas, spread?.first ?: index, dx)
            return
        }
        val half = width / 2f
        // В RTL первая по чтению половина — правая.
        val firstOnLeft = !isRtl
        drawPageAt(canvas, spread.first, dx + if (firstOnLeft) 0f else half, halfWidth = true)
        drawPageAt(canvas, second, dx + if (firstOnLeft) half else 0f, halfWidth = true)
    }

    private fun drawPageAt(canvas: Canvas, index: Int, dx: Float, halfWidth: Boolean = false) {
        val item = items.getOrNull(index) ?: return
        if (item is FeedItem.EndCap) return // содержимое торца рисует Compose поверх
        val page = item as? FeedItem.Page ?: return
        val bitmap = bitmapProvider?.bitmapFor(page) ?: return

        /*
         * Вписываем целиком, сохраняя пропорции: страница манги должна
         * быть видна вся, а обрезка по ширине скрыла бы низ кадра.
         * Масштаб не больше 1 — растянутый скан выглядит хуже полей.
         */
        val slotWidth = if (halfWidth) width / 2f else width.toFloat()
        val scale = minOf(
            slotWidth / bitmap.width,
            height.toFloat() / bitmap.height,
        )
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val left = dx + (slotWidth - drawW) / 2f
        val top = (height - drawH) / 2f
        canvas.drawBitmap(
            bitmap,
            Rect(0, 0, bitmap.width, bitmap.height),
            RectF(left, top, left + drawW, top + drawH),
            bitmapPaint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tracker = velocityTracker ?: VelocityTracker.obtain().also { velocityTracker = it }
        tracker.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                downX = event.x
                downY = event.y
                dragX = 0f
                isDragging = false
                peek.onPointerDown(event.x, event.y, event.eventTime)
                scheduleHoldTimer()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!isDragging && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    isDragging = true
                    cancelHoldTimer()
                }
                if (isDragging) {
                    dragX = dx
                    offsetX = dx
                    invalidate()
                }
                if (peek.onPointerMove(event.x, event.y) == PeekPhase.CANCELLED) cancelHoldTimer()
            }
            MotionEvent.ACTION_UP -> {
                cancelHoldTimer()
                val phase = peek.onPointerUp()
                if (isDragging) {
                    tracker.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    finishDrag(tracker.xVelocity)
                } else if (phase != PeekPhase.RELEASED) {
                    performClick()
                    handleTap(event.x, event.y)
                }
                releaseTracker()
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelHoldTimer()
                peek.onPointerCancel()
                settleBack()
                releaseTracker()
            }
        }
        return true
    }

    private fun finishDrag(velocityX: Float) {
        val fraction = if (width > 0) dragX / width else 0f
        if (!PagedNavigator.shouldFlip(fraction, velocityX, width)) {
            settleBack()
            return
        }
        // Палец потянул влево — при LTR это движение к следующей
        // странице, при RTL к предыдущей. Один знак на оба режима, и
        // он же используется для тап-зон ниже.
        val forward = (dragX < 0) != isRtl
        flip(forward)
    }

    private fun handleTap(x: Float, y: Float) {
        if (width <= 0 || height <= 0) return
        val action = TapZones.resolve(
            scheme = tapZoneScheme,
            normalizedX = x / width,
            normalizedY = y / height,
            isRtl = isRtl,
        )
        when (action) {
            TapAction.MENU -> onTap?.invoke()
            TapAction.NEXT -> flip(forward = true)
            TapAction.PREVIOUS -> flip(forward = false)
            TapAction.NONE -> Unit
        }
    }

    private fun flip(forward: Boolean) {
        val target = nextStop(forward)
        if (target == null) {
            settleBack()
            return
        }
        if (PagedNavigator.crossesChapter(items, currentIndex, target)) {
            // Граница главы не рисуется отдельным экраном — только
            // короткий отклик и тихая смена заголовка.
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onChapterCrossed?.invoke()
        }
        /*
         * Новая страница въезжает с той стороны, куда уходит старая, —
         * с противоположной. Она сразу становится текущей и ставится
         * за краем экрана, а анимация доводит смещение до нуля: так
         * кадр рисуется одной и той же веткой и во время жеста, и
         * после него.
         */
        val outgoingDirection = if (forward != isRtl) -1 else 1
        currentIndex = target
        offsetX = (-outgoingDirection * width).toFloat()
        scroller.startScroll(offsetX.toInt(), 0, -offsetX.toInt(), 0, FLIP_DURATION_MS)
        postInvalidateOnAnimation()
        onViewportSettled?.invoke(currentIndex)
    }

    /**
     * Следующая остановка листания.
     *
     * В режиме разворота шаг — экран, а не страница: иначе перелист с
     * пары показывал бы ту же пару, сдвинутую на страницу, и половина
     * листов появлялась бы дважды.
     */
    private fun nextStop(forward: Boolean): Int? {
        if (!spreadActive()) return PagedNavigator.step(items, currentIndex, forward)
        val all = spreads()
        val position = SpreadPairing.spreadOf(all, currentIndex)
            ?: return PagedNavigator.step(items, currentIndex, forward)
        val next = all.getOrNull(position + if (forward) 1 else -1) ?: return null
        return next.first
    }

    private fun settleBack() {
        if (offsetX == 0f) return
        scroller.startScroll(offsetX.toInt(), 0, -offsetX.toInt(), 0, SETTLE_DURATION_MS)
        postInvalidateOnAnimation()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            offsetX = scroller.currX.toFloat()
            postInvalidateOnAnimation()
        } else if (offsetX != 0f && scroller.isFinished) {
            offsetX = 0f
            invalidate()
        }
    }

    private fun scheduleHoldTimer() {
        val runnable = Runnable {
            if (peek.onHoldTimerFired() == PeekPhase.ACTIVATED) {
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
        holdRunnable = runnable
        postDelayed(runnable, PEEK_HOLD_MS)
    }

    private fun cancelHoldTimer() {
        holdRunnable?.let { removeCallbacks(it) }
        holdRunnable = null
    }

    private fun releaseTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
        isDragging = false
        dragX = 0f
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private companion object {
        const val FLIP_DURATION_MS = 220
        const val SETTLE_DURATION_MS = 160
        const val PEEK_HOLD_MS = 250L
    }
}
