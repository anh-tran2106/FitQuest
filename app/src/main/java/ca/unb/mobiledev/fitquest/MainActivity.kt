package ca.unb.mobiledev.fitquest

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val db = Firebase.firestore

    private val requestPermission =

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Do something if permission granted
            if (isGranted) {
                Log.i("DEBUG", "permission granted")
            } else {
                Log.i("DEBUG", "permission denied")
            }
        }

    private lateinit var toggle: ActionBarDrawerToggle

    private var sensorManager: SensorManager? = null

    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0F
        supportActionBar?.title = intent.getStringExtra("username")

        val navView = findViewById<NavigationView>(R.id.navView)
        navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.miItem1 -> Toast.makeText(applicationContext,
                    "Clicked Item 1", Toast.LENGTH_SHORT).show()
                R.id.miItem2 -> Toast.makeText(applicationContext,
                    "Clicked Item 2", Toast.LENGTH_SHORT).show()
                R.id.miItem3 -> Toast.makeText(applicationContext,
                    "Clicked Item 3", Toast.LENGTH_SHORT).show()
            }
            true
        }

        val stepCardView = findViewById<com.google.android.material.card.MaterialCardView>(R.id.stepCardView)
        stepCardView.setOnClickListener {
            val intent = Intent(this@MainActivity, StepCounterActivity::class.java)
            startActivity(intent)
        }

        loadData()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onStart() {
        super.onStart()
        updateToDate(intent.getStringExtra("username")!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        running = true


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        }
        else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val currentDate = LocalDate.now()

        val stepsTaken : TextView = findViewById(R.id.stepsTaken)
        val stepProgressBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.stepProgressBar)

        if (running) {
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()

            stepsTaken.text = ("$currentSteps")

            stepProgressBar.setProgressCompat(currentSteps, true)
        }
    }

    private fun resetSteps() {
        val stepsTaken : TextView = findViewById(R.id.stepsTaken)
        previousTotalSteps = totalSteps
        stepsTaken.text = 0.toString()
        saveData()
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", previousTotalSteps)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("previousTotalSteps", 0f)
        Log.d("MainActivity", "$savedNumber")
        previousTotalSteps = savedNumber


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun updateToDate(username: String) {
        val currentTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val userRef = db.collection("users").document(username)
        val allDaysRef = db.collection("users").document("${username}.allDays")
        allDaysRef.get().addOnCompleteListener { task ->
            // Add a new document with a generated ID
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val dateData = documentSnapshot.data

                    // If current date doesn't exist in allDays, reset step counter
                    if (dateData != null && !dateData.containsKey(currentTime)) {
                        resetSteps()
                    }
                }
            }
            userRef
                .update(
                    "allDays.${currentTime}.stepCounter", totalSteps - previousTotalSteps,
                    "allDays.${currentTime}.waterCounter", 0
                )
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this@MainActivity, "Today added!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@MainActivity,
                        "Error while adding today",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }
}