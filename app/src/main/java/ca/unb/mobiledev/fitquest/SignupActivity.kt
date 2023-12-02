package ca.unb.mobiledev.fitquest

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import ca.unb.mobiledev.fitquest.databinding.ActivitySignupBinding
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupButton.setOnClickListener{
            val signupUsername = binding.signupUsername.text.toString()
            val signupPassword = binding.signupPassword.text.toString()

            if(signupUsername.isNotEmpty() && signupPassword.isNotEmpty()){
                signupUser(signupUsername, signupPassword)
            }
            else{
                Toast.makeText(this@SignupActivity, "Please Fill Out All Fields!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirect.setOnClickListener {
            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
            finish()
        }

        supportActionBar?.hide()
    }

    private fun signupUser(username: String, password: String) {
        val usersRef = db.collection("users")
        val query = usersRef.whereEqualTo("username", username)
        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (documentSnapshot in task.result!!) {
                    val user = documentSnapshot.getString("username")

                    if (user == username) {
                        Toast.makeText(this@SignupActivity, "Username already exists!", Toast.LENGTH_SHORT).show()
                    }
                }
            }



            if (task.result!!.size() == 0) {
                val allDays: Map<String, Any> = emptyMap()
                val user = hashMapOf(
                    "username" to username,
                    "password" to password,
                    "health"   to 100,
                    "exp"      to 0,
                    "maxExp"   to 100,
                    "level"    to 1,
                    "coin"     to 0,
                    "maxStep"  to 2500,
                    "maxWater" to 8,
                    "allDays"  to allDays
                )

                // Add a new document with a generated ID
                usersRef
                    .document(username).set(user)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this@SignupActivity, "Signup Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignupActivity,  LoginActivity::class.java))

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@SignupActivity, "Signup Unsuccessful!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, e.toString())
                    }
            }
        }
    }
}