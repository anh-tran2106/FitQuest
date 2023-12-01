package ca.unb.mobiledev.fitquest

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalDateTime

class WaterIntake : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_intake)

        val sharedPreferences = getSharedPreferences("SP_INFO", MODE_PRIVATE)
        var waterCount= sharedPreferences.getString("WATERCOUNT", "0")
        var editor = sharedPreferences.edit()

        val addButton: Button = findViewById(R.id.addWater_Button)
        val waterCount_Text: TextView = findViewById(R.id.waterCount_textView)
        waterCount_Text.text = waterCount
        addButton.setOnClickListener{
            waterCount_Text.text = (Integer.parseInt(waterCount_Text.text as String) + 1).toString()
            editor.putString("WATERCOUNT", waterCount_Text.text as String)
            editor.apply()
            if(Integer.parseInt(waterCount_Text.text as String) >= 8){
                Toast.makeText(this, "Congrats! You did it!", Toast.LENGTH_SHORT).show()
            }
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
    }

}