package com.example.smartcompteurtaxi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var surnameEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var licenseTypeEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var generateQrButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nameEditText = findViewById(R.id.name_edittext)
        surnameEditText = findViewById(R.id.surname_edittext)
        ageEditText = findViewById(R.id.age_edittext)
        licenseTypeEditText = findViewById(R.id.license_type_edittext)
        saveButton = findViewById(R.id.save_button)
        qrCodeImageView = findViewById(R.id.qr_code_imageview)
        generateQrButton = findViewById(R.id.generate_qr_button)

        loadProfileData()

        saveButton.setOnClickListener {
            saveProfileData()
        }

        generateQrButton.setOnClickListener {
            generateQrCode()
        }
    }

    private fun loadProfileData() {
        val sharedPref = getSharedPreferences("profile", Context.MODE_PRIVATE)
        nameEditText.setText(sharedPref.getString("name", ""))
        surnameEditText.setText(sharedPref.getString("surname", ""))
        ageEditText.setText(sharedPref.getString("age", ""))
        licenseTypeEditText.setText(sharedPref.getString("licenseType", ""))
    }

    private fun saveProfileData() {
        val sharedPref = getSharedPreferences("profile", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("name", nameEditText.text.toString())
            putString("surname", surnameEditText.text.toString())
            putString("age", ageEditText.text.toString())
            putString("licenseType", licenseTypeEditText.text.toString())
            apply()
        }
        Toast.makeText(this, "Profil enregistré", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Close the activity when the back button is pressed
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun generateQrCode() {
        val driverInfo = "${nameEditText.text}\n" +
                "${surnameEditText.text}\n" +
                "${ageEditText.text}\n" +
                licenseTypeEditText.text.toString()

        if (driverInfo.isBlank()) {
            Toast.makeText(this, "Veuillez remplir les informations du profil", Toast.LENGTH_SHORT).show()
            return
        }

        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(driverInfo, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            qrCodeImageView.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}