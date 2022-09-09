package com.frederico.vcd

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class ComplexLineChartRenderer(
    chart : LineDataProvider,
    animator: ChartAnimator,
    viewPortHandler : ViewPortHandler,
    /*val log : (Any) -> Unit */ ) :LineChartRenderer ( chart, animator, viewPortHandler){

    override fun drawExtras(c: Canvas?) {
        val dataSets = mChart.lineData.dataSets
        for (i in dataSets.indices){
            val dataSet = dataSets[i]
            if (!dataSet.isVisible || dataSet.entryCount < 1) continue

            applyValueTextStyle(dataSet)

            val transformer = mChart.getTransformer(dataSet.axisDependency)

            mXBounds[mChart] = dataSet
            val positions = transformer.generateTransformedValuesLine(
                dataSet,
                mAnimator.phaseX, mAnimator.phaseY,
                mXBounds.min, mXBounds.max
            )
            val formatter = dataSet.valueFormatter

            var lastEntry : Float = -1f
            for (j in positions.indices step 2) {
                //log(j)
                val x = positions[j]
                val y = positions[j + 1]
                if (!mViewPortHandler.isInBoundsRight(x)) break
                if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y)) {
                    continue
                }
                val entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min)
                if(entry.data  == null ) continue
                if(lastEntry == -1f){
                    lastEntry = x
                    continue
                }

                val content : String = if (entry.data is String) entry.data as String else (entry.data as Int).toString(2)
                val heightPos = (positions[j-1] + y)/2 - if(entry.data is String) 20 else 0
                if (content == "END") continue
                drawValue(
                    c,
                    content,//formatter.getPointLabel(entry),
                    (lastEntry + x)/2,
                    heightPos,
                    dataSet.getValueTextColor(j / 2)
                )
                lastEntry = x
            }
        }
    }
}