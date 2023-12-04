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
import android.view.View
import android.widget.Button
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
    private var maxStep = 0
    private var maxStepAchieved = false
    private var waterCounter = 0
    private var maxWater = 0
    private var sleepTime = 0f

    private var healthPoint = 100
    private var expPoint = 0
    private var maxExpPoint = 100
    private var level = 0
    private var coin = 0

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

        val navView = findViewById<NavigationView>(R.id.navView)

        navView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.miItem1 -> Toast.makeText(applicationContext,
                    "Coming soon!", Toast.LENGTH_SHORT).show()
                R.id.miItem2 -> {
                    val intent = Intent(this@MainActivity, ShopActivity::class.java)
                    intent.putExtra("username", this.intent.getStringExtra("username"))
                    startActivity(intent)
                }
                R.id.miItem3 -> Toast.makeText(applicationContext,
                    "Coming soon!", Toast.LENGTH_SHORT).show()
            }
            true
        }

        val header: View = navView.getHeaderView(0)
        val usernameNavHeader: TextView = header.findViewById(R.id.username_navHeader)
        usernameNavHeader.text = intent.getStringExtra("username")!!

        val stepCardView = findViewById<com.google.android.material.card.MaterialCardView>(R.id.stepCardView)
        stepCardView.setOnClickListener {
            val intent = Intent(this@MainActivity, StepCounterActivity::class.java)
            intent.putExtra("username", this.intent.getStringExtra("username"))
            startActivity(intent)
        }
        val waterCardView = findViewById<com.google.android.material.card.MaterialCardView>(R.id.waterCardView)
        waterCardView.setOnClickListener {
            val intent = Intent(this@MainActivity, WaterIntake::class.java)
            intent.putExtra("username",this.intent.getStringExtra("username"))
            startActivity(intent)
        }
        val sleepCardView = findViewById<com.google.android.material.card.MaterialCardView>(R.id.sleepCardView)
        sleepCardView.setOnClickListener {
            val intent = Intent(this@MainActivity, RecordSleepActivity::class.java)
            intent.putExtra("username",this.intent.getStringExtra("username"))
            startActivity(intent)
        }

        val sleepButton = findViewById<Button>(R.id.recordSleepButton)
        sleepButton.setOnClickListener {
            val intent = Intent(this@MainActivity, RecordSleepActivity::class.java)
            intent.putExtra("username",this.intent.getStringExtra("username"))
            startActivity(intent)
        }

        loadData(intent.getStringExtra("username")!!)
        changeWaterCounter(this.intent.getStringExtra("username")!!)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onStart() {
        super.onStart()
        loadData(intent.getStringExtra("username")!!)
        updateToDate(intent.getStringExtra("username")!!)
    }

    override fun onStop() {
        super.onStop()
        updateToDate(intent.getStringExtra("username")!!)
        checkStepGoalAchieved()
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
        val stepsTaken : TextView = findViewById(R.id.stepsTaken)
        val stepProgressBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.stepProgressBar)

        if (running) {
            totalSteps = event!!.values[0]
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()

            stepsTaken.text = ("$currentSteps")

            stepProgressBar.setProgressCompat(currentSteps, true)
        }
    }

    private fun changeWaterCounter(username: String) {
        val waterCounterTextView : TextView = findViewById(R.id.waterTaken)
        val addWaterButton : Button = findViewById(R.id.addWater)
        val removeWaterButton : Button = findViewById(R.id.removeWater)

        addWaterButton.setOnClickListener {
            waterCounter++
            waterCounterTextView.text = waterCounter.toString()

            incrementExp(20)
            updateWaterCounter(username)
        }

        removeWaterButton.setOnClickListener {
            if (waterCounter > 0 && expPoint > 0) {
                waterCounter--
                waterCounterTextView.text = waterCounter.toString()

                incrementExp(-20)
                updateWaterCounter(username)
            }
        }
    }

    private fun resetStepsForLongClick() {
        val stepsTaken : TextView = findViewById(R.id.stepsTaken)
        val stepProgressBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.stepProgressBar)

        stepProgressBar.setOnClickListener {
            Toast.makeText(this@MainActivity, "Long tap to reset step progress", Toast.LENGTH_SHORT).show()
        }

        stepProgressBar.setOnLongClickListener {
            previousTotalSteps = totalSteps
            stepsTaken.text = 0.toString()
            stepProgressBar.setProgressCompat(0, true)
            saveData()
            true
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

    // Load All User Info
    private fun loadData(username : String) {
        val currentTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("previousTotalSteps", 0f)
        Log.d("MainActivity", "$savedNumber")
        previousTotalSteps = savedNumber

        val userRef = db.collection("users").document(username)

        val waterCounterTextView : TextView = findViewById(R.id.waterTaken)
        val currentHealthTextView : TextView = findViewById(R.id.currentHealth)
        val currentExpTextView : TextView = findViewById(R.id.currentExp)
        val maxExpTextView : TextView = findViewById(R.id.maxExp)
        val levelTextView : TextView = findViewById(R.id.level)
        val coinTextView : TextView = findViewById(R.id.coin)
        val maxStepTextView : TextView = findViewById(R.id.maxStep)
        val maxWaterTextView: TextView = findViewById(R.id.maxWater)
        val healthBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.healthBar)
        val expBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.expBar)
        val stepProgressBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.stepProgressBar)


        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot.exists()) {
                    if ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] != null) {
                        waterCounter = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] as (HashMap<*, *>))["waterCounter"].toString().toInt()
                        waterCounterTextView.text = waterCounter.toString()

                        maxStepAchieved = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] as (HashMap<*, *>))["maxStepAchieved"] as Boolean
                        sleepTime = ((documentSnapshot.data?.get("allDays") as (HashMap<*, *>))[currentTime] as (HashMap<*, *>))["sleepTime"].toString().toFloat()
                    }

                    healthPoint = documentSnapshot.data?.get("health").toString().toInt()
                    currentHealthTextView.text = healthPoint.toString()
                    healthBar.setProgressCompat(healthPoint, true)

                    expPoint = documentSnapshot.data?.get("exp").toString().toInt()
                    currentExpTextView.text = expPoint.toString()
                    expBar.setProgressCompat(expPoint, true)

                    maxExpPoint = documentSnapshot.data?.get("maxExp").toString().toInt()
                    maxExpTextView.text = maxExpPoint.toString()
                    expBar.max = maxExpPoint

                    level = documentSnapshot.data?.get("level").toString().toInt()
                    levelTextView.text = level.toString()

                    coin = documentSnapshot.data?.get("coin").toString().toInt()
                    coinTextView.text = coin.toString()

                    maxStep = documentSnapshot.data?.get("maxStep").toString().toInt()
                    maxStepTextView.text = maxStep.toString()
                    stepProgressBar.max = maxStep

                    maxWater = documentSnapshot.data?.get("maxWater").toString().toInt()
                    maxWaterTextView.text = maxWater.toString()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun updateToDate(username: String) {
        val currentTime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener { task ->
            // Add a new document with a generated ID
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot != null && documentSnapshot.exists()) {

                    // If current date map doesn't exist in allDays, reset step counter
                    if (!(documentSnapshot.data?.get("allDays") as (HashMap<*, *>)).containsKey(currentTime)) {
                        resetSteps()
                    }
                }
            }
            userRef
                .update(
                    "coin", coin,
                    "exp", expPoint,
                    "health", healthPoint,
                    "level", level,
                    "maxExp", maxExpPoint,
                    "maxStep", maxStep,
                    "maxWater", maxWater,
                    "allDays.${currentTime}.stepCounter", totalSteps - previousTotalSteps,
                    "allDays.${currentTime}.waterCounter", waterCounter,
                    "allDays.${currentTime}.maxStepAchieved", maxStepAchieved,
                    "allDays.${currentTime}.sleepTime", sleepTime
                )
                .addOnSuccessListener {
//                    Toast.makeText(this@MainActivity, "Today added!", Toast.LENGTH_SHORT).show()
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

    private fun incrementExp(expPointIncrement : Int) {
        val currentExpTextView : TextView = findViewById(R.id.currentExp)
        val expBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.expBar)

        expPoint += expPointIncrement
        if (expPoint >= maxExpPoint) {
            levelUp()
        }
        else {
            currentExpTextView.text = expPoint.toString()
            expBar.setProgressCompat(expPoint, true)
        }
    }
    private fun levelUp() {
        level++
        expPoint = 0
        maxExpPoint += 50 * (level - 1)
        coin += 50

        val levelTextView : TextView = findViewById(R.id.level)
        val currentExpTextView : TextView = findViewById(R.id.currentExp)
        val maxExpTextView : TextView = findViewById(R.id.maxExp)
        val coinTextView : TextView = findViewById(R.id.coin)
        val expBar : com.google.android.material.progressindicator.LinearProgressIndicator = findViewById(R.id.expBar)

        levelTextView.text = level.toString()
        currentExpTextView.text = expPoint.toString()
        maxExpTextView.text = maxExpPoint.toString()
        coinTextView.text = coin.toString()
        expBar.setProgressCompat(expPoint, true)
        expBar.max = maxExpPoint

        Toast.makeText(this@MainActivity, "Level Up!", Toast.LENGTH_SHORT).show()
        updateToDate(intent.getStringExtra("username")!!)
    }

    private fun checkStepGoalAchieved() {
        if ((totalSteps - previousTotalSteps) >= maxStep && !maxStepAchieved) {
            incrementExp(100)
            maxStepAchieved = true
            updateToDate(intent.getStringExtra("username")!!)
            Toast.makeText(this@MainActivity, "Step Goal Achieved!", Toast.LENGTH_SHORT).show()
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
                        this@MainActivity,
                        "Error while updating",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }
}