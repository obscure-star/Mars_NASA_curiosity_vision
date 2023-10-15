package com.example.nasaapii

import android.app.DatePickerDialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.example.nasaapii.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.time.ZonedDateTime

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val constraintLayout get() = binding.constraintLayout
    private val marsImageView get() = binding.marsImage
    private val cameraDropDown get() = binding.cameraDropDown
    private val showDatePickerButton get() = binding.showDatePickerButton
    private val showImageButton get() = binding.showImageButton
    private val photoId get() = binding.photoId
    private val cameras = arrayOf("FHAZ", "RHAZ", "MAST", "CHEMCAM", "MAHLI", "MARDI", "NAVCAM")
    private var selectedCamera = "FHAZ"
    private var date: ZonedDateTime = ZonedDateTime.parse("2015-06-03T00:00:00+01:00[Europe/Paris]")
    lateinit var marsApiPhotosArray: JSONArray
    lateinit var marsPhotoObject: JSONObject


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameras)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cameraDropDown.adapter = adapter

        setOnCameraSelectedListener()
        setOnclickListener()

    }

    private fun setOnCameraSelectedListener() {
        cameraDropDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedCamera = cameras[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle case when nothing is selected
            }
        }
    }

    private fun setOnclickListener() {
        showDatePickerButton.setOnClickListener {
            showDatePickerDialog()
        }
        showImageButton.setOnClickListener {
            showImage()
        }
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(this, { view, year, month, dayOfMonth ->
            val polishedDate = polishDate(dayOfMonth, month, year)
            showDatePickerButton.text = polishedDate
            date = ZonedDateTime.parse( "${polishedDate}T00:00:00+01:00[Europe/Paris]")
        }, date.year, date.monthValue-1, date.dayOfMonth)
        datePickerDialog.show()
    }

    private fun polishDate(day:Int, month: Int, year: Int): String {
        val monthString = if (month+1 >= 10)  "${month+1}" else "0${month+1}"
        val dayString = if (day >= 10) "$day" else "0${day}"
        return "${year}-${monthString}-${dayString}"
    }

    private fun showImage() {
        setMarsApiPhotoObject()
    }

    private fun setMarsApiPhotoObject(){
        try {
            val client = AsyncHttpClient()
            val apiUrl = "https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?earth_date=${polishDate(date.dayOfMonth, date.monthValue-1, date.year)}&api_key=sQED2livPnNjTteyknaMFfoDSnY8ezfpojKu3ofS"

            client.get(apiUrl, object : JsonHttpResponseHandler() {
                override fun onSuccess(
                    statusCode: Int,
                    headers: Headers,
                    json: JsonHttpResponseHandler.JSON
                ) {
                    Log.d("mars", "Response successful: $json")
                    marsApiPhotosArray = json.jsonObject.getJSONArray("photos")
                    setFirstPhotoWithCamera()
                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Headers?,
                    errorResponse: String,
                    throwable: Throwable?
                ) {
                    Log.d("Mars Error", errorResponse)
                    // Handle the error response, e.g., display an error message to the user
                    showErrorSnackBar(Error.INVALID_DATE_OR_CAMERA)
                }
            })
        } catch (e: Exception) {
            // Handle the exception here, for example, log it or perform error handling
            Log.e("Mars API Exception", "An exception occurred: ${e.message}")
            // Handle the error, e.g., display an error message to the user
            showErrorSnackBar(Error.INVALID_DATE_OR_CAMERA)
        }
    }

    private fun setFirstPhotoWithCamera(){
        Log.d("mars", "Searching photos $marsApiPhotosArray")
        if (marsApiPhotosArray.length() < 1){
            showErrorSnackBar(Error.INVALID_DATE)
            return
        }
        for (index in 0 until marsApiPhotosArray.length()){
            val marsPhotoObjectSelected = marsApiPhotosArray.getJSONObject(index)
            val cameraName = marsPhotoObjectSelected.getJSONObject("camera").get("name")
            if (cameraName == selectedCamera){
                marsPhotoObject = marsPhotoObjectSelected
                setImage()
                photoId.text = marsPhotoObject.get("id").toString()
                return
            }
        }
        showErrorSnackBar(Error.INVALID_CAMERA)
        marsPhotoObject = marsApiPhotosArray.getJSONObject(0)
    }

    private fun showErrorSnackBar(error: Error){
        Snackbar.make(constraintLayout, error.message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_light))
            .setTextColor(resources.getColor(android.R.color.white))
            .show()
    }

    private fun setImage() {
        Glide.with(this)
            .load(convertToHttps(marsPhotoObject.get("img_src").toString()))
            .fitCenter()
            .into(marsImageView)
    }

    private fun convertToHttps(url: String): String {
        // Check if the URL starts with "http://"
        if (url.startsWith("http://")) {
            // Replace "http://" with "https://"
            return url.replaceFirst("http://", "https://")
        }
        // URL is already using "https://", no change needed
        return url
    }
}

enum class Error(val message: String){
    INVALID_DATE(message = "Error: No photos in this date!"),
    INVALID_CAMERA(message = "Error: Camera doesn't have photos!"),
    INVALID_DATE_OR_CAMERA(message = "Error: Invalid date or camera!")
}