package com.mohammadkk.simpleweather

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.location.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.mohammadkk.simpleweather.adapter.ForecastAdapter
import com.mohammadkk.simpleweather.adapter.HistoryAdapter
import com.mohammadkk.simpleweather.database.HistoryCity
import com.mohammadkk.simpleweather.databinding.ActivityMainBinding
import com.mohammadkk.simpleweather.databinding.GridWeatherBinding
import com.mohammadkk.simpleweather.dialog.WarningDialog
import com.mohammadkk.simpleweather.helper.*
import com.mohammadkk.simpleweather.model.City
import com.mohammadkk.simpleweather.model.Forecast
import com.mohammadkk.simpleweather.service.NetworkConnection
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private var isRequirePermission = false
    private lateinit var binding: ActivityMainBinding
    private lateinit var include: GridWeatherBinding
    private lateinit var df: DecimalFormat
    private lateinit var networkConnection: NetworkConnection
    private lateinit var cacheApp: CacheApp
    private var forecastItems: ArrayList<Forecast> = ArrayList()
    private var cityList = arrayListOf<City>()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyCity: HistoryCity
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        include = binding.gridWeather
        setContentView(binding.root)
        setSupportActionBar(binding.mainActionbar)
        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale(getString(R.string.lang)))
        decimalSymbols.decimalSeparator = '.'
        df = DecimalFormat("#.##", decimalSymbols)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        networkConnection = NetworkConnection(this)
        cacheApp = CacheApp(this)
        historyCity = HistoryCity(this)
        initHistoryAdapter()
        initDrawer()
        initSearch()
        initRefreshApp()
    }
    override fun onResume() {
        super.onResume()
        findWeather(cacheApp.getLocation(), true)
    }
    private fun initDrawer() {
        val toggle = ActionBarDrawerToggle(this, binding.mainDrawer, binding.mainActionbar, 0, 0)
        binding.mainDrawer.addDrawerListener(toggle)
        toggle.syncState()
        val animationDrawable = binding.mainNav.background as AnimationDrawable
        animationDrawable.start()
    }
    private fun initSearch() {
        binding.edtCity.setOnEditorActionListener { v, actionId, _ ->
            val query = v.text.toString().trim()
            if (query.isEmpty()) {
                Toasty(this).show("متن وارد شده خالی است!")
            } else if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                findWeather(query)
                val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.mainDrawer.closeDrawer(GravityCompat.START)
            }
            false
        }
        binding.edtCity.setOnKeyListener { v, keyCode, _ ->
            val query = binding.edtCity.text.toString().trim()
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (query.isEmpty()) {
                    Toasty(this).show("متن وارد شده خالی است!")
                } else {
                    findWeather(query)
                    val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    binding.mainDrawer.closeDrawer(GravityCompat.START)
                }
            }
            false
        }
    }
    private fun initLanguage() {
        val index = if (getString(R.string.lang) == "en") 0 else 1
        AlertDialog.Builder(this)
            .setTitle("انتخاب زبان")
            .setSingleChoiceItems(arrayOf("انگلیسی", "فارسی"), index) { dialog, which ->
                if (which == 0) {
                    setLocale("en")
                    recreate()
                } else if (which == 1) {
                    setLocale("fa")
                    recreate()
                }
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        val config = baseContext.resources.configuration
        val displayMetrics = baseContext.resources.displayMetrics
        Locale.setDefault(locale)
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, displayMetrics)
    }
    private fun initRefreshApp() {
        binding.refreshApp.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                findWeather(cacheApp.getLocation())
                if (getEdtCity().isNotEmpty()) {
                    binding.edtCity.setText("")
                }
                binding.refreshApp.isRefreshing = false
            }, 500)
        }
    }
    private fun initHistoryAdapter() {
        cityList = historyCity.getAllData()
        cityList.reverse()
        historyAdapter = HistoryAdapter(this, cityList) {name ->
            findWeather(name)
            binding.mainDrawer.closeDrawer(GravityCompat.START)
        }
        binding.historySearchList.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.historySearchList.adapter = historyAdapter
        val listSearchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyCity.getAllCity())
        binding.edtCity.setAdapter(listSearchAdapter)
    }
    override fun onBackPressed() {
        if (binding.mainDrawer.isDrawerOpen(GravityCompat.START)) {
            binding.mainDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_option_main_actionbar, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.getLocationItem -> {
                val locationPermissionOne = ContextCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val locationPermissionTwo = ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (locationPermissionOne && locationPermissionTwo) {
                    getCurrentLocation()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
                }
            }
            R.id.allDeleteHistoryItem -> {
                WarningDialog {
                    historyCity.destroyHistory { isCan ->
                        if (isCan) {
                            initHistoryAdapter()
                        }
                    }
                }.show(supportFragmentManager, "WarningDialog")
            }
            R.id.aboutUsItem -> {
                val message = TextView(this).apply {
                    setPadding(
                        dipDimension(16f), dipDimension(10f),
                        dipDimension(8f), dipDimension(16f)
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(getColorRes(R.color.grey_800))
                    text = getString(R.string.about_message_dialog)
                }

                message.requestLayout()
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.about_us))
                    .setView(message)
                    .setPositiveButton(getString(R.string.confirmation)) { d, _ -> d.dismiss() }
                    .create()
                    .show()
            }
            R.id.changeLanguage -> {
                initLanguage()
            }
            R.id.finshItem -> finish()
        }
        return true
    }
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (isGpsEnabled || isNetworkEnabled) {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                try {
                    val location = it.result
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        latAndLonConvertCityName(lat.toString(), lon.toString())
                    } else {
                        val locationRequest = LocationRequest.create().apply {
                            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                            interval = 10000
                            fastestInterval = 1000
                            numUpdates = 1
                        }
                        val locationCallback = object : LocationCallback() {
                            override fun onLocationResult(p0: LocationResult) {
                                val loc = p0.lastLocation
                                val lat = loc.latitude
                                val lon = loc.longitude
                                latAndLonConvertCityName(lat.toString(), lon.toString())
                            }
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    }
                } catch (e: Exception) {}
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("درخواست GSP")
                .setMessage("لطفا gps موبایل خود را فعال کنید")
                .setCancelable(false)
                .setPositiveButton("بله") { dialog, _ ->
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        startActivity(this)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("نه", null)
                .create()
                .show()
        }
    }
    private fun isNetworkInfo(): Boolean {
        if (networkConnection.isAirplaneMode() && !networkConnection.isInternet()) {
            Snackbar.make(binding.mainDrawer, "موبایل در حالت هواپیماست!!", Snackbar.LENGTH_LONG)
                .setAction("بله") {
                    Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        startActivity(this)
                    }
                }.show()
            return false
        } else if (!networkConnection.isInternet()) {
            Snackbar.make(binding.mainDrawer, "موبایل اینترنت ندارد!!", Snackbar.LENGTH_LONG)
                .setAction("بله") {
                    Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        startActivity(this)
                    }
                }.show()
            return false
        } else {
            return true
        }
    }
    private fun latAndLonConvertCityName(lat: String, lon: String) {
        val url = "http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&lang=fa&units=metric&appid=a17639e2d998a74bd1dc8aa859c64f95"
        val client = AsyncHttpClient()
        if (isNetworkInfo()) {
            client.get(url, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
                    super.onSuccess(statusCode, headers, response)
                    try {
                        val root = JSONObject(response.toString())
                        val name = root.getString("name")
                        findWeather(name, false)
                    } catch (e: Exception) {

                    }
                }
                override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?, errorResponse: JSONObject?) {
                    super.onFailure(statusCode, headers, throwable, errorResponse)
                }
            })
        }

    }
    private fun findWeather(location: String, isResume: Boolean = false) {
        val url = "http://api.openweathermap.org/data/2.5/weather?lang=fa&units=metric&q=$location&APPID=a17639e2d998a74bd1dc8aa859c64f95"
        val client = AsyncHttpClient()
        if (isNetworkInfo()) {
            if (!isResume) binding.progressFinshed.visibility = View.VISIBLE
            client.get(url, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
                    super.onSuccess(statusCode, headers, response)
                    try {
                        if (cacheApp.getLocation() != location) {
                            cacheApp.saveLocation(location)
                        }
                        if (getEdtCity().isNotEmpty()) {
                            historyCity.insertData(location) {isCan ->
                                if (isCan) {
                                    initHistoryAdapter()
                                }
                            }
                        }
                        val root = JSONObject(response.toString())
                        val coord = root.getJSONObject("coord")
                        val weather = root.getJSONArray("weather").getJSONObject(0)
                        val main = root.getJSONObject("main")
                        val sys = root.getJSONObject("sys")
                        val lon = coord.getString("lon")
                        val lat = coord.getString("lat")
                        val country = sys.getString("country")
                        val city = root.getString("name")
                        val local = Locale("", country)
                        val newCountry = local.getDisplayCountry(Locale(getString(R.string.lang)))
                        val description = weather.getString("description")
                        val icon = weather.getString("icon")
                        val iconSrc = resources.getIdentifier("@drawable/w$icon", null, packageName)
                        val iconResult = ContextCompat.getDrawable(this@MainActivity, iconSrc)
                        val temp = df.format(main.getDouble("temp"))
                        val visibility = df.format((root.getInt("visibility") /1000))
                        val feelLike = df.format(main.getDouble("feels_like"))
                        val cloud = df.format(root.getJSONObject("clouds").getInt("all"))
                        val sunrise = sys.getLong("sunrise")
                        val sunset = sys.getLong("sunset")
                        findForecast(lon, lat, sunrise, sunset)
                        val pressure = df.format(main.getInt("pressure"))
                        val humidity = df.format(main.getInt("humidity"))
                        val windSpeed = df.format((root.getJSONObject("wind").getDouble("speed") * 3.6))
                        supportActionBar?.title = "$newCountry, $city"
                        binding.textDescription.text = description
                        binding.textCurrentTemp.text = String.format("%s°", temp)
                        binding.iconWeather.setImageDrawable(iconResult)
                        binding.textVisibility.text = createHtml("${getString(R.string.visibility)} <b>$visibility km</b>")
                        binding.textRealFeel.text = createHtml("${getString(R.string.title_real_feel)} <b>$feelLike°</b>")
                        include.textCloudCover.text = String.format("%s%%", cloud)
                        include.textPressure.text = String.format("%s hPa", pressure)
                        include.textHygrometer.text = String.format("%s%%", humidity)
                        include.textWindSpeed.text = String.format("%s km/h", windSpeed)
                    } catch (e: Exception) {
                    }
                }
                override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?, errorResponse: JSONObject?) {
                    super.onFailure(statusCode, headers, throwable, errorResponse)
                }
                override fun onFinish() {
                    super.onFinish()
                    if (!isResume) binding.progressFinshed.visibility = View.GONE
                }
            })
        }
    }
    private fun findForecast(lon: String, lat: String, sunrise: Long, sunset: Long) {
        val url = "http://api.openweathermap.org/data/2.5/onecall?lat=$lat&lon=$lon&lang=${getString(R.string.lang)}&units=metric&exclude=current&appid=a17639e2d998a74bd1dc8aa859c64f95"
        val client = AsyncHttpClient()
        client.get(url, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
                super.onSuccess(statusCode, headers, response)
                val root = JSONObject(response.toString())
                val timezone = root.getString("timezone")
                val daily = root.getJSONArray("daily")
                forecastItems = getForecast(daily, timezone)
                binding.forecastList.layoutManager = LinearLayoutManager(this@MainActivity)
                ForecastAdapter(this@MainActivity, forecastItems).apply {
                    binding.forecastList.adapter = this
                }
                val day = daily.getJSONObject(0)
                val temp = day.getJSONObject("temp")
                val tempMin = df.format(temp.getDouble("min"))
                val tempMax = df.format(temp.getDouble("max"))
                binding.textMinTemp.text = String.format("%s°", tempMin)
                binding.textMaxTemp.text = String.format("%s°", tempMax)
                include.textSunrise.text = sunrise.dateTimePattern(timezone, getString(R.string.lang))
                include.textSunset.text = sunset.dateTimePattern(timezone, getString(R.string.lang))
            }
            override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?, errorResponse: JSONObject?) {
                super.onFailure(statusCode, headers, throwable, errorResponse)
            }
        })
    }
    private fun getForecast(daily: JSONArray, timezone: String): ArrayList<Forecast> {
        val forecast = ArrayList<Forecast>()
        for (i in 0 until 8) {
            val day = daily.getJSONObject(i)
            val weather = day.getJSONArray("weather").getJSONObject(0)
            val temp = day.getJSONObject("temp")
            val tempMin = df.format(temp.getDouble("min"))
            val tempMax = df.format(temp.getDouble("max"))
            val icon = weather.getString("icon")
            val description = weather.getString("description")
            val dayName = day.getLong("dt").getDay(timezone, this)
            val item = Forecast(dayName,description, tempMin, tempMax, icon)
            forecast.add(item)
        }
        return forecast
    }
    private fun getEdtCity(): String = binding.edtCity.text.toString().trim()
}