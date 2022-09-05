package com.frederico.vcd

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.frederico.vcd.databinding.ActivityChooseSignalBinding
import com.frederico.vcd.databinding.ActivityMainBinding

class ChooseSignal : AppCompatActivity() {

    private lateinit var binding: ActivityChooseSignalBinding
    lateinit var scopes: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scopes = intent.getStringArrayListExtra("scopes")!!
        binding = ActivityChooseSignalBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_choose_signal)
        setContentView(binding.root)

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.setHasFixedSize(true)
        binding.list.adapter = AdapterInfo(this, scopes) {
            val selected = scopes[it]
            val intent_result = Intent()
            intent_result.putExtra("result", selected)
            setResult(Activity.RESULT_OK, intent_result)
            finish()
        }
    }
}