package ca.unb.mobiledev.fitquest

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.time.LocalDate
import java.time.LocalDateTime
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry;

class WaterIntake : AppCompatActivity() {
    private lateinit var barChart: BarChart
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_intake)

        val sharedPreferences = getSharedPreferences("SP_INFO", MODE_PRIVATE)
        var waterCount= sharedPreferences.getString("WATERCOUNT", "0")
        var editor = sharedPreferences.edit()

        var target = 0;

        val addButton: Button = findViewById(R.id.addWater_Button)
        val waterCount_Text: TextView = findViewById(R.id.waterCount_textView)
        waterCount_Text.text = waterCount
        addButton.setOnClickListener{
            waterCount_Text.text = (Integer.parseInt(waterCount_Text.text as String) + 1).toString()
            editor.putString("WATERCOUNT", waterCount_Text.text as String)
            editor.apply()
            if(Integer.parseInt(waterCount_Text.text as String) >= target){
                Toast.makeText(this, "Congrats! You reached your target!", Toast.LENGTH_SHORT).show()
            }
        }
        val setTarget: Button = findViewById(R.id.setTargetBtn)
        setTarget.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Set Target")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setView(input)

            builder.setPositiveButton("OK") { _, _ ->
                val targetInput = input.text.toString()
                if (targetInput.isNotEmpty()) {
                    target = targetInput.toInt()
                    Toast.makeText(this, "Target set to $target", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            }
            builder.show()
        }

        val minusButton: Button = findViewById(R.id.minusWater_Button)

        minusButton.setOnClickListener {
            if(Integer.parseInt(waterCount_Text.text as String) > 0) {
                waterCount_Text.text =
                    (Integer.parseInt(waterCount_Text.text as String) - 1).toString()
                editor.putString("WATERCOUNT", waterCount_Text.text as String)
                editor.apply()
            }
            else{
                Toast.makeText(this, "Action not available!", Toast.LENGTH_SHORT).show()
            }
        }



        val currentDate = LocalDateTime.now().toString()
        var pastTime: String? = sharedPreferences.getString("PASTTIME", "2023-01-01").toString()
        if (pastTime != currentDate){
            editor.putString("PASTTIME", currentDate)
            waterCount_Text.text = "0"
            editor.putString("WATERCOUNT",  "0")
            editor.apply()
        }

        barChart = findViewById<BarChart>(R.id.waterChart)
        val list:ArrayList<BarEntry> = ArrayList()

        list.add(BarEntry(0.toFloat(), 13.toFloat()))
        list.add(BarEntry(1.toFloat(), 23.toFloat()))
        list.add(BarEntry(2.toFloat(), 4.toFloat()))
        list.add(BarEntry(3.toFloat(), 28.toFloat()))
        list.add(BarEntry(4.toFloat(), 51.toFloat()))

        val barDataSet = BarDataSet(list,"Cups of Water")
        barDataSet.setColor(Color.GREEN, 255)

        val barData = BarData(barDataSet)

        barChart.setFitBars(true)
        barChart.data = barData
        barChart.description.text ="Daily Water Intake"
        barChart.animateY(2000)
    }
}

