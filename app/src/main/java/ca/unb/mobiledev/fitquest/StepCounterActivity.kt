package ca.unb.mobiledev.fitquest

import android.content.ContentValues
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
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
                    Toast.makeText(this@StepCounterActivity, "Step Target Updated", Toast.LENGTH_SHORT).show()
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

}