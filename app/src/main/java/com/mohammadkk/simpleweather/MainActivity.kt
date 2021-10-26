package com.mohammadkk.simpleweather

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.AnimationDrawable
import android.location.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.mohammadkk.simpleweather.adapter.ForecastAdapter
import com.mohammadkk.simpleweather.adapter.HistoryAdapter
import com.mohammadkk.simpleweather.database.HistoryCity
import com.mohammadkk.simpleweather.dialog.WarningDialog
import com.mohammadkk.simpleweather.helper.*
import com.mohammadkk.simpleweather.model.City
import com.mohammadkk.simpleweather.model.Forecast
import com.mohammadkk.simpleweather.service.NetworkConnection
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.client.cache.Resource
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.grid_weather.*
import kotlinx.android.synthetic.main.nav_header_drawer.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var df: DecimalFormat
    private lateinit var networkConnection: NetworkConnection
    private lateinit var cacheApp: CacheApp
    private lateinit var airplanModeSnackbar: Snackbar
    private lateinit var isDisabledInternetSnackbar: Snackbar
    private var forecastItems: ArrayList<Forecast> = ArrayList()
    private var cityList = arrayListOf<City>()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyCity: HistoryCity
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainActionbar)
        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale(getString(R.string.lang)))
        decimalSymbols.decimalSeparator = '.'
        df = DecimalFormat("#.##", decimalSymbols)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        networkConnection = NetworkConnection(this)
        cacheApp = CacheApp(this)
        historyCity = HistoryCity(this)
        airplanModeSnackbar = Snackbar.make(mainDrawer, "موبایل در حالت هواپیماست!!", Snackbar.LENGTH_LONG)
            .setAction("بله") {
                Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(this)
                }
            }
        isDisabledInternetSnackbar = Snackbar.make(mainDrawer, "اتصال به اینترنت قطع است!!", Snackbar.LENGTH_LONG)
            .setAction("بله") {
                Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    startActivity(this)
                }
            }
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
        val toggle = ActionBarDrawerToggle(this, mainDrawer, mainActionbar, 0, 0)
        mainDrawer.addDrawerListener(toggle)
        toggle.syncState()
        val animationDrawable = mainNav.background as AnimationDrawable
        animationDrawable.start()
    }
    private fun initSearch() {
        edtCity.setOnEditorActionListener { v, actionId, _ ->
            val query = v.text.toString().trim()
            if (query.isEmpty()) {
                Toasty(applicationContext).show("متن وارد شده خالی است!")
            } else if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                findWeather(query)
                val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                mainDrawer.closeDrawer(GravityCompat.START)
            }
            false
        }
        edtCity.setOnKeyListener { v, keyCode, _ ->
            val query = edtCity.text.toString().trim()
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (query.isEmpty()) {
                    Toasty(applicationContext).show("متن وارد شده خالی است!")
                } else {
                    findWeather(query)
                    val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    mainDrawer.closeDrawer(GravityCompat.START)
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
        refreshApp.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                findWeather(cacheApp.getLocation())
                if (getEdtCity().isNotEmpty()) {
                    edtCity.setText("")
                }
                refreshApp.isRefreshing = false
            }, 500)
        }
    }
    private fun initHistoryAdapter() {
        cityList = historyCity.getAllData()
        cityList.reverse()
        historyAdapter = HistoryAdapter(this, cityList) {name ->
            findWeather(name)
            mainDrawer.closeDrawer(GravityCompat.START)
        }
        historySearchList.layoutManager = LinearLayoutManager(this@MainActivity)
        historySearchList.adapter = historyAdapter
        val listSearchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyCity.getAllCity())
        edtCity.setAdapter(listSearchAdapter)
    }
    override fun onBackPressed() {
        if (mainDrawer.isDrawerOpen(GravityCompat.START)) {
            mainDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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
                    ActivityCompat.requestPermissions(this, arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_LOCATION_PERMISSION)
                }
            }
            R.id.allDeleteHistoryItem -> {
                WarningDialog {
                    historyCity.destoryHistory {isCan ->
                        if (isCan) {
                            initHistoryAdapter()
                        }
                    }
                }.show(supportFragmentManager, "WarningDialog")
            }
            R.id.aboutUsItem -> {
                AlertDialog.Builder(this)
                    .setTitle("درباره ما")
                    .setMessage("این برنامه در حال حاضر درباره پیش بینی آب و هواست و وضعیت جاری هوا منطقه را نیز بر می گرداند و سازنده این اپ نیز محمد کریمی کلات است")
                    .setPositiveButton(getString(R.string.confirmation)) {dialog, _ ->
                        dialog.dismiss()
                    }
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.isNotEmpty()) {
            if ((grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
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
    private fun latAndLonConvertCityName(lat: String, lon: String) {
        val url = "http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&lang=fa&units=metric&appid=a17639e2d998a74bd1dc8aa859c64f95"
        val client = AsyncHttpClient()
        if (networkConnection.isAirplaneMode() && !networkConnection.isInternet()) {
            airplanModeSnackbar.show()
        } else if (!networkConnection.isInternet()) {
            isDisabledInternetSnackbar.show()
        } else {
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
        if (networkConnection.isAirplaneMode() && !networkConnection.isInternet()) {
            airplanModeSnackbar.show()
        } else if (!networkConnection.isInternet()) {
            isDisabledInternetSnackbar.show()
        } else {
            if (!isResume) progressFinshed.visibility = View.VISIBLE
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
                        textDescription.text = description
                        textCurrentTemp.text = String.format("%s°", temp)
                        iconWeather.setImageDrawable(iconResult)
                        textVisibility.text = createHtml("${getString(R.string.visibility)} <b>$visibility km</b>")
                        textRealFeel.text = createHtml("${getString(R.string.title_real_feel)} <b>$feelLike°</b>")
                        textCloudCover.text = String.format("%s%%", cloud)
                        textPressure.text = String.format("%s hPa", pressure)
                        textHygrometer.text = String.format("%s%%", humidity)
                        textWindSpeed.text = String.format("%s km/h", windSpeed)
                    } catch (e: Exception) {
                    }
                }
                override fun onFailure(statusCode: Int, headers: Array<out Header>?, throwable: Throwable?, errorResponse: JSONObject?) {
                    super.onFailure(statusCode, headers, throwable, errorResponse)
                }
                override fun onFinish() {
                    super.onFinish()
                    if (!isResume) progressFinshed.visibility = View.GONE
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
                forecastList.layoutManager = LinearLayoutManager(this@MainActivity)
                ForecastAdapter(this@MainActivity, forecastItems).apply {
                    forecastList.adapter = this
                }
                val day = daily.getJSONObject(0)
                val temp = day.getJSONObject("temp")
                val tempMin = df.format(temp.getDouble("min"))
                val tempMax = df.format(temp.getDouble("max"))
                textMinTemp.text = String.format("%s°", tempMin)
                textMaxTemp.text = String.format("%s°", tempMax)
                textSunrise.text = sunrise.dateTimePattern(timezone, getString(R.string.lang))
                textSunset.text = sunset.dateTimePattern(timezone, getString(R.string.lang))
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
    private fun getEdtCity(): String = edtCity.text.toString().trim()
    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 100
    }
}