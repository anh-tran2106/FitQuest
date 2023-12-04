package ca.unb.mobiledev.fitquest

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StepCounterActivity : AppCompatActivity() {
    private val db = Firebase.firestore

    private var totalSteps = 0f
    private var stepTarget = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_counter)

        val username = intent.getStringExtra("username")!!

        supportActionBar?.elevation = 0F
        supportActionBar?.title = "Steps"

        loadData(username)

        val maxStepTextView: TextView = findViewById(R.id.maxStep_stepScreen)
        val stepProgressCircular: com.mikhaellopez.circularprogressbar.CircularProgressBar = findViewById(R.id.stepProgressCircular_stepScreen)
        val setStepTargetButton: Button = findViewById(R.id.setStepTarget_stepScreen)
        setStepTargetButton.setOnClickListener {
            val view: View = LayoutInflater.from(this@StepCounterActivity).inflate(R.layout.layout_max_step_dialog, null)
            val editText: TextInputEditText = view.findViewById(R.id.editText)
            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(this@StepCounterActivity)
                .setTitle("Set Target")
                .setView(view)
                .setPositiveButton("Ok") { _, _ ->
                    val targetInput = editText.text.toString()
                    if (targetInput.isNotEmpty()) {
                        stepTarget = targetInput.toInt()
                        maxStepTextView.text = stepTarget.toString()
                        stepProgressCircular.progressMax = stepTarget.toFloat()

                        Toast.makeText(this, "Target set to $stepTarget", Toast.LENGTH_SHORT).show()
                        updateStepTarget(intent.getStringExtra("username")!!)
                    } else {
                        Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null).create()

            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
            }

            alertDialog.show()
        }
        sendDataToScreen(username)
    }

    override fun onStart() {
        super.onStart()

        val username = intent.getStringExtra("username")!!

        loadData(username)
    }

    private fun loadData(username: String) {
        val currentTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val userRef = db.collection("users").document(username)

        val stepCounterTextView: TextView = findViewById(R.id.stepsTaken_stepScreen)
        val maxStepTextView: TextView = findViewById(R.id.maxStep_stepScreen)
        val stepProgressCircular: com.mikhaellopez.circularprogressbar.CircularProgressBar = findViewById(R.id.stepProgressCircular_stepScreen)



        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot.exists()) {
                    if ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] != null) {
                        totalSteps = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] as (HashMap<*, *>))["stepCounter"].toString().toFloat()
                        stepCounterTextView.text = totalSteps.toInt().toString()
                        stepProgressCircular.apply {
                            setProgressWithAnimation(totalSteps)
                        }
                    }

                    stepTarget = documentSnapshot.data?.get("maxStep").toString().toInt()
                    maxStepTextView.text = stepTarget.toString()
                    stepProgressCircular.progressMax = stepTarget.toFloat()
                }
            }
        }
    }

    private fun updateStepTarget(username: String) {
        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener {
            userRef
                .update(
                    "maxStep", stepTarget,
                )
                .addOnSuccessListener {
//                    Toast.makeText(this@StepCounterActivity, "Step Target Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@StepCounterActivity,
                        "Error while updating step target",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }

    private fun sendDataToScreen(username: String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val monthDateFormatter = DateTimeFormatter.ofPattern("M/d")
        val currentTimeNoFormat = LocalDate.now()
        val currentTime = LocalDate.now().format(formatter)
        val userRef = db.collection("users").document(username)

        val barChart = findViewById<BarChart>(R.id.stepChart_stepScreen)
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
                            stepCounter = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTimeNoFormat.minusDays(i.toLong()).format(formatter)] as (HashMap<*, *>))["stepCounter"].toString().toFloat()
                        }
                        dataList.add(BarEntry(counter.toFloat(), stepCounter))
                        xAxisLables.add(currentTimeNoFormat.minusDays(i.toLong()).format(monthDateFormatter))
                        counter++;
                    }

                    val barDataSet = BarDataSet(dataList,"Steps")
                    barDataSet.valueFormatter = DefaultValueFormatter(0)
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

                    // Update distance and calories burnt according to total steps
                    if ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] != null) {
                        stepCounter = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] as (HashMap<*, *>))["stepCounter"].toString().toFloat()
                        convertStepToKm(stepCounter)
                        convertStepToCal(stepCounter)
                    }
                }
            }
        }
    }

    // Convert Total Steps to Km and change the Text View
    private fun convertStepToKm(totalSteps: Float) {
        val magicNumber = 1312.335958
        val distanceTextView: TextView = findViewById(R.id.distance_StepScreen)
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING

        val km = (totalSteps) / magicNumber
        distanceTextView.text = df.format(km).toDouble().toString()
    }

    private fun convertStepToCal(totalSteps: Float) {
        val magicNumber = 28.985507
        val calTextView: TextView = findViewById(R.id.cal_stepScreen)

        val cal = totalSteps / magicNumber
        calTextView.text = cal.toInt().toString()
    }
}