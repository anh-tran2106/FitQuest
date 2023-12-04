package ca.unb.mobiledev.fitquest

import android.content.res.Resources
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.github.mikephil.charting.animation.Easing
import java.time.LocalDate
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import nl.joery.timerangepicker.TimeRangePicker
import java.time.format.DateTimeFormatter
import android.content.ContentValues
import android.content.res.Configuration
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast


class RecordSleepActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_sleep)

        val username = intent.getStringExtra("username")!!

        supportActionBar?.elevation = 0F
        supportActionBar?.title = "Sleep"

        updateTimes()
        updateDuration()
        val picker: TimeRangePicker = findViewById(R.id.timeRangePicker)

        picker.setOnTimeChangeListener(object : TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                updateTimes()
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
                updateDuration()
            }
        })

        picker.setOnDragChangeListener(object : TimeRangePicker.OnDragChangeListener {
            override fun onDragStart(thumb: TimeRangePicker.Thumb): Boolean {
                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, true)
                }
                return true
            }

            override fun onDragStop(thumb: TimeRangePicker.Thumb) {
                if(thumb != TimeRangePicker.Thumb.BOTH) {
                    animate(thumb, false)
                }

                Log.d(
                    "TimeRangePicker",
                    "Start time: " + picker.startTime
                )
                Log.d(
                    "TimeRangePicker",
                    "End time: " + picker.endTime
                )
                Log.d(
                    "TimeRangePicker",
                    "Total duration: " + picker.duration
                )
            }
        })

        sendDataToScreen(username)
        handleSetTimeClick()
    }

    private fun updateTimes() {
        val picker: TimeRangePicker = findViewById(R.id.timeRangePicker)
        val end_time: TextView = findViewById(R.id.end_time)
        end_time.text = picker.endTime.toString()
        val start_time: TextView = findViewById(R.id.start_time)
        start_time.text = picker.startTime.toString()
    }

    private fun updateDuration() {
        val picker: TimeRangePicker = findViewById(R.id.timeRangePicker)
        val duration: TextView = findViewById(R.id.duration)
        duration.text = picker.duration.toString()
    }

    private fun animate(thumb: TimeRangePicker.Thumb, active: Boolean) {
        val bedtime_layout = findViewById<LinearLayout>(R.id.bedtime_layout)
        val wake_layout = findViewById<LinearLayout>(R.id.wake_layout)
        val activeView = if(thumb == TimeRangePicker.Thumb.START) bedtime_layout else wake_layout
        val inactiveView = if(thumb == TimeRangePicker.Thumb.START) wake_layout else bedtime_layout
        val direction = if(thumb == TimeRangePicker.Thumb.START) 1 else -1

        activeView
            .animate()
            .translationY(if(active) (activeView.measuredHeight / 2f)*direction else 0f)
            .setDuration(300)
            .start()
        inactiveView
            .animate()
            .alpha(if(active) 0f else 1f)
            .setDuration(300)
            .start()
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


                    val isNightMode = applicationContext.resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

                    if (isNightMode) {
                        barDataSet.valueTextColor = Color.rgb(255, 255, 255)
                        barChart.xAxis.textColor = Color.rgb(255, 255, 255)
                        barChart.axisLeft.textColor = Color.rgb(255, 255, 255)
                        barChart.axisRight.textColor = Color.rgb(255, 255, 255)
                        barChart.legend.textColor = Color.rgb(255, 255, 255)
                    }

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