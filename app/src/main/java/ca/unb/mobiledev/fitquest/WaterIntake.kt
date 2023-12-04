package ca.unb.mobiledev.fitquest

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
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
import java.time.format.DateTimeFormatter

class WaterIntake : AppCompatActivity() {
    private lateinit var barChart: BarChart
    private val db = Firebase.firestore
    private var maxWater = 0
    private var waterCounter = 0
    private var expPoint = 0
    private var maxExpPoint = 100
    private var level = 0
    private var coin = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_intake)

        val username = intent.getStringExtra("username")!!

        supportActionBar?.elevation = 0F
        supportActionBar?.title = "Water"

        loadData(username)

        val maxWaterView: TextView = findViewById(R.id.maxWater)
        val setWaterTarget: Button = findViewById(R.id.setTargetBtn)
        setWaterTarget.setOnClickListener{
            val view: View = LayoutInflater.from(this@WaterIntake).inflate(R.layout.layout_max_water_dialog, null)
            val editText: TextInputEditText = view.findViewById(R.id.editText)
            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(this@WaterIntake)
                .setTitle("Set Target")
                .setView(view)
                .setPositiveButton("Ok") { _, _ ->
                    val targetInput = editText.text.toString()
                    if (targetInput.isNotEmpty()) {
                        maxWater = targetInput.toInt()
                        maxWaterView.text = maxWater.toString()

                        Toast.makeText(this, "Target set to $maxWater", Toast.LENGTH_SHORT).show()
                        updateWaterTarget(intent.getStringExtra("username")!!)
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

        val waterCounterTextView : TextView = findViewById(R.id.waterCount_textView)
        val addWaterButton : Button = findViewById(R.id.addWater_Button)
        val minusWaterButton : Button = findViewById(R.id.minusWater_Button)

        addWaterButton.setOnClickListener {
            waterCounter++
            waterCounterTextView.text = waterCounter.toString()
            updateWaterCounter(username)

            incrementExp(20)
            updateToDate(username)
        }

        minusWaterButton.setOnClickListener {
            if(waterCounter > 0 && expPoint > 0) {
                waterCounter--
                waterCounterTextView.text = waterCounter.toString()
                updateWaterCounter(username)

                incrementExp(-20)
                updateToDate(username)
            }
        }
        sendDataToScreen(username)
    }

    override fun onStart() {
        super.onStart()
        val username = intent.getStringExtra("username")!!
        loadData(username)
    }

    override fun onStop() {
        super.onStop()
        updateWaterCounter(intent.getStringExtra("username")!!)
    }

    private fun loadData(username: String) {
        val currentTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val userRef = db.collection("users").document(username)

        val waterCounterView: TextView = findViewById(R.id.waterCount_textView)
        val maxWaterView: TextView = findViewById(R.id.maxWater)


        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot.exists()) {
                    if ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] != null) {
                        waterCounter = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] as (HashMap<*, *>))["waterCounter"].toString().toInt()
                        waterCounterView.text = waterCounter.toString()
                    }

                    maxWater = documentSnapshot.data?.get("maxWater").toString().toInt()
                    maxWaterView.text = maxWater.toString()

                    expPoint = documentSnapshot.data?.get("exp").toString().toInt()
                    maxExpPoint = documentSnapshot.data?.get("maxExp").toString().toInt()
                    level = documentSnapshot.data?.get("level").toString().toInt()
                    coin = documentSnapshot.data?.get("coin").toString().toInt()
                }
            }
        }
    }

    private fun updateWaterTarget(username: String) {
        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener {
            userRef
                .update(
                    "maxWater", maxWater,
                )
                .addOnSuccessListener {
                    Toast.makeText(this@WaterIntake, "Water Target Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@WaterIntake,
                        "Error while updating water target",
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
        val userRef = db.collection("users").document(username)

        val barChart = findViewById<BarChart>(R.id.waterChart)
        val dataList: ArrayList<BarEntry> = ArrayList()
        val xAxisLables: MutableList<String> = ArrayList()
        var waterCounter: Float
        var counter = 0
        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot.exists()) {

                    // Update water data for 7 days up to the current date to the chart
                    for (i in 6 downTo 0) {
                        waterCounter = 0f
                        if ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTimeNoFormat.minusDays(i.toLong()).format(formatter)] != null) {
                            waterCounter = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTimeNoFormat.minusDays(i.toLong()).format(formatter)] as (HashMap<*, *>))["waterCounter"].toString().toFloat()
                        }
                        dataList.add(BarEntry(counter.toFloat(), waterCounter))
                        xAxisLables.add(currentTimeNoFormat.minusDays(i.toLong()).format(monthDateFormatter))
                        counter++;
                    }

                    val barDataSet = BarDataSet(dataList,"Glasses")
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
                }
            }
        }
    }
    private fun updateWaterCounter(username: String) {
        val currentTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener { task ->
            userRef
                .update(
                    "allDays.${currentTime}.waterCounter", waterCounter,
                )
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@WaterIntake,
                        "Error while updating",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }
    private fun incrementExp(expPointIncrement : Int) {

        expPoint += expPointIncrement
        if (expPoint >= maxExpPoint) {
            levelUp()
        }

    }
    private fun levelUp() {
        level++
        expPoint = 0
        maxExpPoint += 50 * (level - 1)
        coin += 50

        Toast.makeText(this@WaterIntake, "Level Up!", Toast.LENGTH_SHORT).show()
        updateToDate(intent.getStringExtra("username")!!)
    }

    private fun updateToDate(username: String) {
        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener { task ->
            userRef
                .update(
                    "coin", coin,
                    "exp", expPoint,
                    "level", level,
                    "maxExp", maxExpPoint,
                )
                .addOnSuccessListener {
//                    Toast.makeText(this@WaterIntake, "Updated Exp", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@WaterIntake,
                        "Error",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }
}

