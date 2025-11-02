package com.example.smartcompteurtaxi

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class ProfileActivity : AppCompatActivity() {

    private lateinit var qrCodeImageView: ImageView
    private lateinit var generateQrButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val nameTextView = findViewById<TextView>(R.id.name_textview)
        val surnameTextView = findViewById<TextView>(R.id.surname_textview)
        val ageTextView = findViewById<TextView>(R.id.age_textview)
        val licenseTypeTextView = findViewById<TextView>(R.id.license_type_textview)
        qrCodeImageView = findViewById(R.id.qr_code_imageview)
        generateQrButton = findViewById(R.id.generate_qr_button)

        nameTextView.text = getString(R.string.driver_name)
        surnameTextView.text = getString(R.string.driver_surname)
        ageTextView.text = getString(R.string.driver_age)
        licenseTypeTextView.text = getString(R.string.driver_license_type)

        generateQrButton.setOnClickListener {
            generateQrCode()
        }
    }

    private fun generateQrCode() {
        val driverInfo = "${getString(R.string.driver_name)}\n" +
                "${getString(R.string.driver_surname)}\n" +
                "${getString(R.string.driver_age)}\n" +
                getString(R.string.driver_license_type)

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