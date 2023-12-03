package ca.unb.mobiledev.fitquest

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


class ShopActivity : AppCompatActivity() {
    private val db = Firebase.firestore

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

                    coin = documentSnapshot.data?.get("coin").toString().toInt()
                    myCoinTextView.text = coin.toString()
                }
            }
        }
    }

    private fun handleHealthPotionCardViewClick() {
        val healthPotionCardView: com.google.android.material.card.MaterialCardView = findViewById(R.id.healthPotionCardView)

        healthPotionCardView.setOnClickListener {
            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Purchase Confirmation")
                .setMessage("Do you really want to buy a health potion?\n(Consumed instantly upon purchase)")
                .setPositiveButton("YES!!?") { _, _ ->
                    Toast.makeText(
                        this@ShopActivity,
                        "Yaay",
                        Toast.LENGTH_SHORT
                    ).show()
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
        val goldenAppleCardViewClick: com.google.android.material.card.MaterialCardView = findViewById(R.id.goldenAplleCardView)

        goldenAppleCardViewClick.setOnClickListener {
            val alertDialog: AlertDialog = MaterialAlertDialogBuilder(this)
                .setTitle("Purchase Confirmation")
                .setMessage("Do you really want to buy a golden apple?\n(Consumed instantly upon purchase)")
                .setPositiveButton("YES!!?") { _, _ ->
                    Toast.makeText(
                        this@ShopActivity,
                        "Yaay",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("No", null).create()

            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor( resources.getColor(R.color.primaryTextColor, theme))
            }
            alertDialog.show()
        }
    }
}