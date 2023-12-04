package ca.unb.mobiledev.fitquest

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


class ShopActivity : AppCompatActivity() {
    private val db = Firebase.firestore

    private var health = 0
    private var coin = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        supportActionBar?.elevation = 0F
        supportActionBar?.title = "Shop"

        val username = intent.getStringExtra("username")!!

        handleHealthPotionCardViewClick()
        handleGoldenAppleCardViewClick()
        loadData(username)
    }

    private fun loadData(username: String) {
        val userRef = db.collection("users").document(username)

        val myCoinTextView: TextView = findViewById(R.id.myCoin_shopScreen)

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documentSnapshot = task.result
                if (documentSnapshot.exists()) {
                    health = documentSnapshot.data?.get("health").toString().toInt()
                    coin = documentSnapshot.data?.get("coin").toString().toInt()
                    myCoinTextView.text = coin.toString()
                }
            }
        }
    }

    // Update new Coin value to database
    private fun updateCoin(username: String) {
        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener {
            userRef
                .update(
                    "coin", coin,
                )
                .addOnSuccessListener {
//                    Toast.makeText(this@ShopActivity, "Coin value Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@ShopActivity,
                        "Error while updating Coin value",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }

    // Update new Health value to database
    private fun updateHealth(username: String) {
        val userRef = db.collection("users").document(username)
        userRef.get().addOnCompleteListener {
            userRef
                .update(
                    "health", health,
                )
                .addOnSuccessListener {
//                    Toast.makeText(this@ShopActivity, "Health value Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@ShopActivity,
                        "Error while updating Health value",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d(ContentValues.TAG, e.toString())
                }
        }
    }


    private fun handleHealthPotionCardViewClick() {
        val HEALTH_POTION_PRICE = 50
        val HEAL_AMOUNT = 20

        val healthPotionCardView: com.google.android.material.card.MaterialCardView = findViewById(R.id.healthPotionCardView)

        healthPotionCardView.setOnClickListener {
            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Purchase Confirmation")
                .setMessage("Do you really want to buy a health potion?\n(Consumed instantly upon purchase)")
                .setPositiveButton("YES!!?") { _, _ ->

                    purchase(HEALTH_POTION_PRICE, HEAL_AMOUNT)

                }
                .setNegativeButton("No", null).create()

            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
            }
            alertDialog.show()
        }
    }

    private fun handleGoldenAppleCardViewClick() {
        val GOLDEN_APPLE_PRICE = 200
        val HEAL_AMOUNT = 100

        val goldenAppleCardViewClick: com.google.android.material.card.MaterialCardView = findViewById(R.id.goldenAplleCardView)

        goldenAppleCardViewClick.setOnClickListener {
            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Purchase Confirmation")
                .setMessage("Do you really want to buy a golden apple?\n(Consumed instantly upon purchase)")
                .setPositiveButton("YES!!?") { _, _ ->

                    purchase(GOLDEN_APPLE_PRICE, HEAL_AMOUNT)

                }
                .setNegativeButton("No", null).create()

            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
            }
            alertDialog.show()
        }
    }

    private fun purchase(price: Int, healAmount: Int) {
        val username = intent.getStringExtra("username")!!
        val myCoinTextView: TextView = findViewById(R.id.myCoin_shopScreen)

        val remainder = coin - price

        if (remainder < 0) {
            Toast.makeText(this@ShopActivity, "Not Enough Coin...", Toast.LENGTH_SHORT).show()
        }
        else {
            coin = remainder
            myCoinTextView.text = coin.toString()
            updateCoin(username)
            heal(healAmount)
            Toast.makeText(this@ShopActivity, "Purchase complete", Toast.LENGTH_SHORT).show()
        }
    }

    private fun heal(healAmount: Int) {
        val username = intent.getStringExtra("username")!!
        val newHealth = health + healAmount
        if (newHealth > 100) {
            health = 100
        }
        else {
            health = newHealth
        }
        updateHealth(username)
    }
}