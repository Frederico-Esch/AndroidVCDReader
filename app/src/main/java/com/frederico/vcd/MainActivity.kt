package com.frederico.vcd

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.GestureDetectorCompat
import com.frederico.vcd.databinding.ActivityMainBinding
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.nio.charset.Charset
import java.security.KeyStore
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val choose_colors = arrayOf(
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.BLACK,
        Color.MAGENTA,
    )
    val request_code = 112
    var selectedFile = ""
    var data: HashMap<String, ArrayList<Info>>? = null
    var timePoints: HashMap<String, Array<Point>>? = null
    val sets = ArrayList<ILineDataSet>()

    val getContent = registerForActivityResult(ActivityResultContracts.GetContent()){
        val input = contentResolver.openInputStream(it)?.readBytes()
        val result = String(input!!, Charset.defaultCharset())

        Log.println(Log.VERBOSE, "VCD-CRASH", result)

        data = readFile(result)
        timePoints = points(result)
    }

    val selectInfo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        val result = it.data?.getStringExtra("result")
        if(result != null){
            val scope = result.substringBeforeLast('.')
            Toast.makeText(this, "Selected: " + result, Toast.LENGTH_SHORT).show()
            var selection : Info? = null

            for (info in data!![scope]!!) {
                if (info.scope == result) selection = info
            }

            val entries = ArrayList<Entry>()
            timePoints!![selection!!.symbol.toString()]!!.forEach { point:Point ->
                entries.add(Entry(point.time.toFloat(), point.value.toFloat() - sets.size*2))
            }
            val lineDataSet = LineDataSet(entries, result)
            lineDataSet.color = choose_colors.random()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                if(lineDataSet.color == Color.BLACK && this.resources.configuration.isNightModeActive){
                    lineDataSet.color = Color.WHITE
                }
            }
            lineDataSet.setDrawCircles(false)
            lineDataSet.lineWidth = 2f

            binding.chart.axisLeft.axisMinimum = (sets.size*(-2) - 1).toFloat()

            sets.add(lineDataSet)

            binding.chart.data = LineData(sets)
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


        binding.chart.setDrawGridBackground(false)
        //binding.chart.xAxis.setDrawGridLines(false)
        binding.chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return if(value == value.toInt().toFloat()) value.toInt().toString()
                else ""
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R  ){
            if(this.resources.configuration.isNightModeActive) {
                binding.chart.xAxis.textColor = Color.WHITE
                binding.chart.legend.textColor = Color.WHITE
            }
        }
        binding.chart.axisLeft.setDrawGridLines(false)
        binding.chart.axisLeft.setDrawLabels(false)
        binding.chart.axisLeft.axisMaximum = 2f
        binding.chart.axisLeft.axisMinimum = -1f
        binding.chart.axisRight.isEnabled = false
        binding.chart.setMaxVisibleValueCount(0)
        binding.chart.description.isEnabled = false
        binding.chart.invalidate()

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
                    var scopes = ArrayList<String>()
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