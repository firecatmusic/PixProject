package com.yzzzd.pixproject

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.fxn.pix.Options
import com.fxn.pix.Pix
import com.yzzzd.pixproject.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returnValue = result.data?.getStringArrayListExtra(Pix.IMAGE_RESULTS)
            if (returnValue?.isNotEmpty() == true) {
                val imageFile = File(returnValue[0])
                binding.imageView.setImageURI(imageFile.toUri())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonCamera.setOnClickListener { openCamera() }
    }

    private fun openCamera() {
        Pix.open(this, Options.Mode.Both, activityResultLauncher)
    }
}