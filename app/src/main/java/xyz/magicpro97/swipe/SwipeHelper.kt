package xyz.magicpro97.swipe

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.SparseArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.util.contains
import androidx.core.util.size
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList


abstract class SwipeHelper(context: Context, private val recyclerView: RecyclerView) :
    ItemTouchHelper.SimpleCallback(0, LEFT) {
    var buttons = ArrayList<UnderlayButton>()
    var swipedPos = -1
    private val buttonsBuffer = SparseArray<ArrayList<UnderlayButton>>()
    private var swipeThreshold = 0.5f

    private var recoverQueue = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean =
            if (contains(element)) false else super.add(element)
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            for (button in buttons) {
                if (button.onClick(e!!.x, e.y)) break
            }
            return true
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    private val onTouchListener = View.OnTouchListener { view, e ->
        if (swipedPos < 0) false
        else {
            val point = Point(e.rawX.toInt(), e.rawY.toInt())
            val swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos)
            val swipedItem = swipedViewHolder?.itemView
            val rect = Rect()
            swipedItem?.getGlobalVisibleRect(rect)
            if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_MOVE) {
                if (rect.top < point.y && rect.bottom > point.y)
                    gestureDetector.onTouchEvent(e)
                else {
                    recoverQueue.add(swipedPos)
                    swipedPos = -1
                    recoverSwipedItem()
                }
            }
            true
        }
    }

    init {
        recyclerView.setOnTouchListener(onTouchListener)
        attachSwipe()
    }

    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val pos = recoverQueue.poll()!!
            if (pos > -1) {
                recyclerView.adapter!!.notifyItemChanged(pos)
            }
        }
    }

    companion object {
        const val TAG = "SwipeHelper"
        const val BUTTON_WIDTH = 300f
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.adapterPosition

        if (swipedPos != pos)
            recoverQueue.add(swipedPos)

        swipedPos = pos

        if (buttonsBuffer.contains(swipedPos))
            buttons = buttonsBuffer.get(swipedPos)
        else
            buttons.clear()

        buttonsBuffer.clear()
        swipeThreshold = 0.5f * buttons.size * BUTTON_WIDTH
        recoverSwipedItem()
    }

    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float = swipeThreshold
    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = 0.1f * defaultValue
    override fun getSwipeVelocityThreshold(defaultValue: Float): Float = 5.0f * defaultValue

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.adapterPosition
        val translationX: Float
        val itemView = viewHolder.itemView
        var lockDx = dX

        if (pos < 0) {
            swipedPos = pos
            return
        }

        if (actionState === ACTION_STATE_SWIPE) {
            if (dX < 0) {
                var buffer = ArrayList<UnderlayButton>()

                if (!buttonsBuffer.contains(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer)
                    buttonsBuffer.put(pos, buffer)
                } else {
                    buffer = buttonsBuffer.get(pos)
                }

                translationX = dX * buffer.size * BUTTON_WIDTH / itemView.width
                drawButtons(c, itemView, buffer, pos, translationX)
                if (-dX >= buttonsBuffer.size * BUTTON_WIDTH) lockDx = -(buffer.size * BUTTON_WIDTH)
//                Log.e(TAG, "dX: $dX | tranX: $translationX")
//                Log.e(TAG, "H: ${c.height} | W: ${c.width} | ${itemView.width}")
            }
        }



        super.onChildDraw(c, recyclerView, viewHolder, lockDx, dY, actionState, isCurrentlyActive)
    }

    private fun drawButtons(
        c: Canvas,
        itemView: View,
        buffer: java.util.ArrayList<UnderlayButton>,
        pos: Int,
        dX: Float
    ) {
        var right = itemView.right
        val dButtonWidth = -1 * dX / buffer.size

        for (button in buffer) {
            val left = right - dButtonWidth
            button.onDraw(
                c,
                RectF(
                    left,
                    itemView.top.toFloat(),
                    right.toFloat(),
                    itemView.bottom.toFloat()
                ),
                pos
            )

            right = left.toInt()
        }
    }

    abstract fun instantiateUnderlayButton(
        viewHolder: RecyclerView.ViewHolder,
        buffer: ArrayList<UnderlayButton>
    )

    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    class UnderlayButton(
        private var text: String = "", private var imageId: Int = 0, private var color: Int? = null,
        private var underlayButtonClickListener: UnderlayButtonClickListener? = null
    ) {
        private var pos: Int = 0
        private var clickRegion: RectF? = null

        fun onClick(x: Float, y: Float): Boolean =
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                underlayButtonClickListener?.onClick()
                true
            } else false

        fun onDraw(c: Canvas, rectF: RectF, pos: Int) {
            val p = Paint()

            // Draw background
            p.color = color!!
            c.drawRect(rectF, p)

            // Draw Text
            val textPaint = TextPaint()
            textPaint.color = Color.WHITE
            textPaint.textSize = LayoutHelper.getPx(c, 12).toFloat()
            val sl = StaticLayout(
                text, textPaint, rectF.width().toInt(),
                Layout.Alignment.ALIGN_CENTER, 1f, 1f, false
            )
            c.save()

            val r = Rect()
            val cHeight = rectF.height()
            val y = cHeight / 2f + r.height() / 2f - r.bottom - sl.height / 2
            c.translate(rectF.left, rectF.top + y)
            sl.draw(c)
            c.restore()

            clickRegion = rectF
            this.pos = pos
        }

        interface UnderlayButtonClickListener {
            fun onClick()
        }
    }
}