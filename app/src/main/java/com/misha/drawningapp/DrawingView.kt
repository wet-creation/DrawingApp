package com.misha.drawningapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private lateinit var mDrawPath: CustomPath
    private lateinit var mCanvasBitmap:Bitmap
    private lateinit var mDrawPaint: Paint
    private lateinit var mCanvasPaint: Paint
    private var mBrushSize = 0F
    private var color = Color.BLACK
    private lateinit var canvas: Canvas
    private val mPaths = ArrayList<CustomPath>()
    private val mPathsUndo = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap= Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(mCanvasBitmap, 0f,0f,mCanvasPaint)
        for (e in mPaths){
            mDrawPaint.strokeWidth = e.brushThickness
            mDrawPaint.color = e.color
            canvas?.drawPath(e,mDrawPaint)
        }

        if (!mDrawPath.isEmpty){
            mDrawPaint.strokeWidth = mDrawPath.brushThickness
            mDrawPaint.color = mDrawPath.color
            canvas?.drawPath(mDrawPath,mDrawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath.color = color
                mDrawPath.brushThickness = mBrushSize

                mDrawPath.reset()
                mDrawPath.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE ->{
                mDrawPath.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP ->{
                mPaths.add(mDrawPath)
                mDrawPath = CustomPath(color,mBrushSize)

            }
            else -> return false
        }
        invalidate()


        return true
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint.color = color
        mDrawPaint.style = Paint.Style.STROKE
        mDrawPaint.strokeJoin = Paint.Join.ROUND
        mDrawPaint.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }
    fun setSizeForBrush(size:Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            size, resources.displayMetrics)
        mDrawPaint.strokeWidth = mBrushSize
    }
    fun setColor(newColor: String){
        color = Color.parseColor(newColor)
        mDrawPaint.color = color
    }
    fun undo(){
        if (mPaths.size>0) {
            mPathsUndo.add(mPaths.elementAt(mPaths.size - 1))
            mPaths.removeAt(mPaths.size - 1)
            invalidate()
        }
    }
    fun returnUndo(){
        if (mPathsUndo.size>0) {
            mPaths.add(mPathsUndo.elementAt(mPathsUndo.size - 1))
            mPathsUndo.removeAt(mPathsUndo.size - 1)
            invalidate()
        }
    }
    fun clearAll(){
        mPathsUndo.addAll(mPaths)
        mPaths.clear()
        invalidate()
    }




    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path()


}