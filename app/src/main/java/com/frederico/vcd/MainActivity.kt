package com.frederico.vcd

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.frederico.vcd.databinding.ActivityMainBinding
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.model.GradientColor
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.utils.ViewPortHandler
import java.nio.charset.Charset
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val choose_colors = arrayOf(
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.BLACK,
        Color.MAGENTA,
    )
    private val request_code = 112
    private var data: HashMap<String, ArrayList<Info>>? = null
    private var timePoints: HashMap<String, Array<Point>>? = null
    private val sets = ArrayList<ILineDataSet>()

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()){
        val input = contentResolver.openInputStream(it)?.readBytes()
        val result = String(input!!, Charset.defaultCharset())

        data = readFile(result)
        timePoints = points(result)
    }

    private val selectInfo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val result = it.data?.getStringExtra("result")
        if(result != null){
            val scope = result.substringBeforeLast('.')
            Toast.makeText(this, "Selected: " + result, Toast.LENGTH_SHORT).show()
            var selection : Info? = null

            for (info in data!![scope]!!) {
                if (info.scope == result) selection = info
            }

            val entries = ArrayList<Entry>()
            if(selection!!.size > 1){
                var lastX = -1
                timePoints!![selection!!.symbol.toString()]!!.forEach { point: Point ->
                    entries.add(Entry(point.time.toFloat(), 1f - sets.size * 2))

                    if(lastX != point.time){
                        entries.add(Entry(point.time.toFloat(), 0f - sets.size*2, point.value))
                        lastX = point.time
                        entries.add((entries[entries.size-2]))
                    }
                }
            }else {
                var lastDataHadData = ""
                timePoints!![selection.symbol.toString()]!!.forEach { point: Point ->

                    when(lastDataHadData) {
                        "XXXXX" -> {
                            entries.add(Entry(point.time.toFloat(), 1f - sets.size * 2, "END"))
                            entries.add(Entry(point.time.toFloat(), 0f - sets.size * 2 ))
                        }
                        "ZZZZZ" -> {
                            entries.add(Entry(point.time.toFloat(), .5f - sets.size * 2, "END"))
                            entries.add(Entry(point.time.toFloat(), 0f - sets.size * 2 ))
                        }
                    }

                    val entry =
                        when (point.value) {
                            -1 -> Entry(point.time.toFloat(), 1f - sets.size * 2,  "XXXXX") //if(selection.type == "wire")  "TRISTATE" else
                            -2 -> Entry(point.time.toFloat(), 0.5f - sets.size * 2,  "ZZZZZ")
                            else -> Entry(point.time.toFloat(), point.value.toFloat() - sets.size * 2)
                        }

                    entries.add(entry)
                    lastDataHadData = if (entry.data != null) entry.data.toString() else ""
                }
            }
            val lineDataSet = LineDataSet(entries, result)

            lineDataSet.apply {
                color = choose_colors.random()
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    if(color == Color.BLACK && resources.configuration.isNightModeActive){
                        color = Color.WHITE
                        valueTextColor = Color.WHITE
                    }
                }
                setDrawCircles(false)
                lineWidth = 2f

                setDrawFilled(true)
                fillColor = color
                fillFormatter =
                    IFillFormatter { dataSet, _ -> dataSet?.yMin ?: 0f }

            }

            binding.chart.axisLeft.axisMinimum = (sets.size*(-2) - 1).toFloat()

            sets.add(lineDataSet)

            binding.chart.apply{
                renderer = ComplexLineChartRenderer(this, animator, viewPortHandler) /*{
                    Log.println(Log.VERBOSE, "CHART_DRAW", it.toString())
                }*/
            }

            binding.chart.data = LineData(sets)

            binding.chart.xAxis.axisMaximum += .01f
            binding.chart.xAxis.resetAxisMaximum() //eu sei que parece que não faz sentido mas se inverter nao funciona
            binding.chart.invalidate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf (android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        var hasPermissions = true
        for (perm in permissions){
            if(ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) hasPermissions = false
        }
        if(!hasPermissions){
            ActivityCompat.requestPermissions(this, permissions, request_code)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.chart.apply {
            setDrawGridBackground(false)
            //binding.chart.xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return if(value == value.toInt().toFloat()) value.toInt().toString()
                    else ""
                }
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R  ){
                if(this.resources.configuration.isNightModeActive) {
                        xAxis.textColor = Color.WHITE
                        legend.textColor = Color.WHITE
                }
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawLabels(false)
                axisMaximum = 2f
                axisMinimum = -1f
            }
            axisRight.isEnabled = false
            setMaxVisibleValueCount(0)
            description.isEnabled = false
            invalidate()
        }
    }

    /**
     * A native method that is implemented by the 'vcd' native library,
     * which is packaged with this application.
     */
    external fun randomValue(scale:Int): Int
    external fun readFile(filename:String): HashMap<String, ArrayList<Info>>
    external fun points(filename:String): HashMap<String, Array<Point>>
    //external fun testes() : HashMap<String, Info>

    companion object {
        // Used to load the 'vcd' library on application startup.
        init {
            System.loadLibrary("vcd")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.find_file -> {
                getContent.launch(("*/*"))
                true
            }
            R.id.add_signal -> {
                if(data == null) Toast.makeText(this, "Select a VCD before adding signals", Toast.LENGTH_SHORT).show()
                else{
                    val scopes = ArrayList<String>()
                    data!!.values.forEach {
                        it.forEach {
                            scopes.add(it.scope)
                        }
                    }
                    val intent = Intent(this,  ChooseSignal::class.java).apply {
                        putExtra("scopes", scopes)
                    }
                    selectInfo.launch(intent)
                }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }
}