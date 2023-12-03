package ca.unb.mobiledev.fitquest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ShopActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        supportActionBar?.elevation = 0F
        supportActionBar?.title = "Shop"
    }
}