package ru.alexeyivannikov.lab22

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import ru.alexeyivannikov.lab22.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startNewGame()
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {


            if (startPoint == null) {
                startPoint = generatePoint(location)
            }
            startPoint?.let {
                val curPoint = Coordinates(location.latitude, location.longitude)
                updateGameState(curPoint, it)
            }


        }

    }

    private var locationManager: LocationManager? = null

    private var startPoint: Coordinates? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setOnSettingsClickListener()

    }

    override fun onResume() {
        super.onResume()
        binding.btnNewGuess.setOnClickListener {
            startNewGame()
        }
    }

    private fun startNewGame() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_notification_permission), Toast.LENGTH_SHORT
                ).show()
                return
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_notification_permission), Toast.LENGTH_SHORT
                ).show()
                return
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }

        } else {
            Toast.makeText(this, getString(R.string.point_init_notification), Toast.LENGTH_SHORT)
                .show()
            initGame()
        }
    }

    private fun setOnSettingsClickListener() {
        binding.btnSettings.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:ru.alexeyivannikov.lab22".toUri()
            )
            startActivity(intent)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initGame() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        Log.d("LOC_TAG", locationManager.toString())
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            0.001f,
            locationListener
        )

        binding.tvGameStatus.visibility = View.VISIBLE
        binding.tvDistance.visibility = View.VISIBLE
        binding.tvGameStatus.text = resources.getText(R.string.game_status_in_progress)

    }

    private fun generatePoint(beginningLocation: Location): Coordinates {
        val D = 0.04
        var x = Random.nextFloat() * 0.04
        if (Random.nextBoolean()) {
            x *= -1
        }
        var y = sqrt(D * D - x * x)
        if (Random.nextBoolean()) {
            y *= -1
        }
        val coordinates =
            Coordinates(beginningLocation.latitude + x, beginningLocation.longitude + y)
        return coordinates
    }

    private fun updateGameState(curPoint: Coordinates, targetPoint: Coordinates) {
        val d = 6_371_000
        val curXRad = Math.toRadians(curPoint.x)
        val curYRad = Math.toRadians(curPoint.y)
        val targetXRad = Math.toRadians(targetPoint.x)
        val targetYRad = Math.toRadians(targetPoint.y)
        val arg =
            sin(curXRad) * sin(targetXRad) + cos(targetXRad) * cos(curXRad) * cos(curYRad - targetYRad)

        val distance = d * acos(arg)

        binding.tvDistance.text = String.format(
            Locale.getDefault(),
            resources.getText(R.string.distance).toString(),
            distance
        )

        if (distance <= 100) {
            finishGame()
        }
    }

    private fun finishGame() {
        binding.tvGameStatus.text = resources.getText(R.string.game_status_finished)
        Toast.makeText(this, getString(R.string.win_notification), Toast.LENGTH_SHORT).show()

    }


    override fun onStop() {
        super.onStop()
        locationManager?.removeUpdates(locationListener)
    }

    private data class Coordinates(val x: Double, val y: Double)
}