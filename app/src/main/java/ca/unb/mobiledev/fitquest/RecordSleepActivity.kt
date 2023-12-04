package ca.unb.mobiledev.fitquest

import android.content.ContentValues
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.mikephil.charting.animation.Easing
import java.time.LocalDate
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import nl.joery.timerangepicker.TimeRangePicker
import java.time.format.DateTimeFormatter
class RecordSleepActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_sleep)

        val username = intent.getStringExtra("username")!!

        val picker: nl.joery.timerangepicker.TimeRangePicker = findViewById(R.id.timeRangePicker)
        picker.setOnTimeChangeListener(object : TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                Log.d("TimeRangePicker", "Start time: " + startTime)
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                Log.d("TimeRangePicker", "End time: " + endTime.hour)
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
                Log.d("TimeRangePicker", "Duration: " + duration.hour)
            }
        })

        picker.setOnDragChangeListener(object : TimeRangePicker.OnDragChangeListener {
            override fun onDragStart(thumb: TimeRangePicker.Thumb): Boolean {
                // Do something on start dragging
                return true // Return false to disallow the user from dragging a handle.
            }

            override fun onDragStop(thumb: TimeRangePicker.Thumb) {
                // Do something on stop dragging
            }
        })

        sendDataToScreen(username)
        handleSetTimeClick()
    }
    private fun sendDataToScreen(username: String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val monthDateFormatter = DateTimeFormatter.ofPattern("M/d")
        val currentTimeNoFormat = LocalDate.now()
        val currentTime = LocalDate.now().format(formatter)
        val userRef = db.collection("users").document(username)

        val barChart = findViewById<BarChart>(R.id.sleepChart)
        val dataList: ArrayList<BarEntry> = ArrayList()
        val xAxisLables: MutableList<String> = ArrayList()
        var stepCounter: Float
        var counter = 0
        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot.exists()) {

                    // Update step data for 7 days up to the current date to the chart
                    for (i in 6 downTo 0) {
                        stepCounter = 0f
                        if ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTimeNoFormat.minusDays(i.toLong()).format(formatter)] != null) {
                            stepCounter = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTimeNoFormat.minusDays(i.toLong()).format(formatter)] as (HashMap<*, *>))["sleepTime"].toString().toFloat()
                        }
                        dataList.add(BarEntry(counter.toFloat(), stepCounter))
                        xAxisLables.add(currentTimeNoFormat.minusDays(i.toLong()).format(monthDateFormatter))
                        counter++;
                    }

                    val barDataSet = BarDataSet(dataList,"Hours of Sleep")
                    barDataSet.setDrawValues(false)
                    barDataSet.valueFormatter = DefaultValueFormatter(2)
                    barDataSet.color = Color.rgb(65, 75, 178)
                    val barData = BarData(barDataSet)

                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLables)
                    barChart.setFitBars(true)
                    barChart.data = barData
                    barChart.description.text =""
                    barChart.animateY(700, Easing.EaseOutSine)
                    barChart.setTouchEnabled(false)
                }
            }
        }
    }

    private fun handleSetTimeClick() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val currentTime = LocalDate.now().format(formatter)

        val setTimeButton: Button = findViewById(R.id.setTimeBtn)
        val timeRangePicker: nl.joery.timerangepicker.TimeRangePicker = findViewById(R.id.timeRangePicker)
        val username = intent.getStringExtra("username")!!

        val userRef = db.collection("users").document(username)

        setTimeButton.setOnClickListener {
            userRef.get().addOnCompleteListener {
                userRef
                    .update(
                        "allDays.${currentTime}.sleepTime", timeRangePicker.durationMinutes / 60.0
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this@RecordSleepActivity, "Sleep Time Set", Toast.LENGTH_SHORT).show()

                        sendDataToScreen(username)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@RecordSleepActivity,
                            "Error while setting sleep time",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(ContentValues.TAG, e.toString())
                    }
            }
        }
    }
}