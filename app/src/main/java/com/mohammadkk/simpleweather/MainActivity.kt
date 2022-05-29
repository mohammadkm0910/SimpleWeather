package com.mohammadkk.simpleweather

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.location.LocationManager
import android.os.*
import android.provider.Settings
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.mohammadkk.simpleweather.adapter.ForecastAdapter
import com.mohammadkk.simpleweather.adapter.HistoryAdapter
import com.mohammadkk.simpleweather.database.HistoryCity
import com.mohammadkk.simpleweather.databinding.ActivityMainBinding
import com.mohammadkk.simpleweather.databinding.GridWeatherBinding
import com.mohammadkk.simpleweather.databinding.WarningDialogBinding
import com.mohammadkk.simpleweather.helper.*
import com.mohammadkk.simpleweather.model.City
import com.mohammadkk.simpleweather.model.Forecast
import com.mohammadkk.simpleweather.service.BuildApi
import com.mohammadkk.simpleweather.service.NetworkConnection
import cz.msebera.android.httpclient.Header
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var include: GridWeatherBinding
    private lateinit var df: DecimalFormat
    private val networkConnection: NetworkConnection by lazy {
        NetworkConnection(this)
    }
    private val historyCity: HistoryCity by lazy {
        HistoryCity(this)
    }
    private var forecastItems: ArrayList<Forecast> = ArrayList()
    private var cityList = arrayListOf<City>()
    private lateinit var listSearchAdapter: ArrayAdapter<String>
    private lateinit var historyAdapter: HistoryAdapter
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
        initHistoryAdapter()
        initDrawer()
        initSearch()
        findWeather(baseConfig.lastLocation)
        initRefreshApp()
        getTranslate()
    }
    override fun onResume() {
        super.onResume()
        findWeather(baseConfig.lastLocation, true)
    }
    private fun initDrawer() {
        val toggle = ActionBarDrawerToggle(this, binding.root, binding.mainActionbar, 0, 0)
        binding.root.addDrawerListener(toggle)
        toggle.syncState()
        val animationDrawable = binding.mainNav.background as AnimationDrawable
        animationDrawable.start()
    }
    private fun initSearch() {
        binding.edtCity.setOnEditorActionListener { v, actionId, event ->
            val query = v.text.toString()
            if (query.trim().isEmpty()) {
                if (query.isNotEmpty()) binding.edtCity.setText("")
                createCustomToast("متنی وارد نشده است!")
            } else if (actionId == EditorInfo.IME_ACTION_SEARCH || event.keyCode == KeyEvent.KEYCODE_ENTER) {
                findWeather(query.trim())
                val imm = v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                binding.root.closeDrawer(GravityCompat.START)
            }
            true
        }
    }
    private fun initLanguage() {
        val index = baseConfig.appLanguage
        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(resources.getStringArray(R.array.list_language), index) { dialog, which ->
                baseConfig.appLanguage = which
                val intent = intent
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
                dialog.dismiss()
            }
            .create()
            .show()
    }
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val locale = when (newBase.baseConfig.appLanguage) {
                0 -> Locale("en")
                1 -> Locale("fa")
                2 -> Locale.getDefault()
                else -> Locale.getDefault()
            }
            val context = wrap(newBase, locale)
            super.attachBaseContext(context)
        } else super.attachBaseContext(newBase)
    }
    private fun wrap(context: Context, newLocale: Locale): ContextWrapper {
        var ctx = context
        val res = ctx.resources
        val configuration = res.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(newLocale)
            val localeList = LocaleList(newLocale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
            ctx = ctx.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = newLocale
            @Suppress("DEPRECATION")
            res.updateConfiguration(configuration, res.displayMetrics)
        }
        return ContextWrapper(ctx)
    }
    private fun initRefreshApp() {
        binding.refreshApp.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                findWeather(baseConfig.lastLocation)
                if (editCity.isNotEmpty()) {
                    binding.edtCity.setText("")
                }
                binding.refreshApp.isRefreshing = false
            }, 500)
        }
    }
    private fun initHistoryAdapter() {
        cityList = historyCity.getAllData()
        historyAdapter = HistoryAdapter(this) {name ->
            findWeather(name)
            binding.root.closeDrawer(GravityCompat.START)
        }
        historyAdapter.clear()
        historyAdapter.allAll(cityList)
        listSearchAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyCity.getAllCity())
        binding.historySearchList.layoutManager = LinearLayoutManager(this@MainActivity)
        binding.historySearchList.adapter = historyAdapter
        binding.edtCity.setAdapter(listSearchAdapter)
    }
    override fun onBackPressed() {
        if (binding.root.isDrawerOpen(GravityCompat.START)) {
            binding.root.closeDrawer(GravityCompat.START)
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
                if (hasPermission(permission.ACCESS_FINE_LOCATION) && hasPermission(permission.ACCESS_COARSE_LOCATION)) {
                    getCurrentLocation()
                } else {
                   ActivityCompat.requestPermissions(this, arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
                }
            }
            R.id.allDeleteHistoryItem -> {
                val layout = WarningDialogBinding.inflate(layoutInflater)
                val alert = MaterialAlertDialogBuilder(this)
                alert.setView(layout.root)
                val dialog = alert.create()
                dialog.show()
                layout.btnConfirmationDialog.setOnClickListener {
                    if (historyCity.destroyHistory()) {
                        historyAdapter.clear()
                        binding.edtCity.setText("")
                        listSearchAdapter.notifyDataSetChanged()
                        listSearchAdapter.clear()
                    }
                    dialog.dismiss()
                }
                layout.btnCancelDialog.setOnClickListener { dialog.dismiss() }
            }
            R.id.aboutUsItem -> {
                val message = TextView(this).apply {
                    setPadding(
                        dipDimension(16f), dipDimension(10f),
                        dipDimension(8f), dipDimension(16f)
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(getColorRes(R.color.grey_800))
                    setText(R.string.about_message_dialog)
                }
                message.requestLayout()
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_us)
                    .setView(message)
                    .setPositiveButton(R.string.confirmation) { d, _ -> d.dismiss() }
                    .create()
                    .show()
            }
            R.id.changeLanguage -> {
                initLanguage()
            }
            R.id.finshItem -> {
                finishAndRemoveTask()
                exitProcess(-1)
            }
        }
        return true
    }
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (isGpsEnabled && isNetworkEnabled) {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener {
                try {
                    val location = it.result
                    if (location != null) {
                        val gps = listOf(location.latitude, location.longitude)
                        latAndLonConvertCityName(gps[0], gps[1])
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
                                latAndLonConvertCityName(lat, lon)
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
            Snackbar.make(binding.root, "موبایل در حالت هواپیماست!!", Snackbar.LENGTH_LONG)
                .setAction("بله") {
                    Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        startActivity(this)
                    }
                }.show()
            return false
        } else if (!networkConnection.isInternet()) {
            Snackbar.make(binding.root, "موبایل اینترنت ندارد!!", Snackbar.LENGTH_LONG)
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
    private fun latAndLonConvertCityName(lat: Double, lon: Double) {
        val client = AsyncHttpClient()
        if (isNetworkInfo()) {
            client.get(BuildApi.getMainGPSAddress(lat, lon), object : JsonHttpResponseHandler() {
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
        val client = AsyncHttpClient()
        if (isNetworkInfo()) {
            if (!isResume) binding.progressLoading.visibility = View.VISIBLE
            client.get(BuildApi.getMainWeather(location), object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Array<out Header>?, response: JSONObject?) {
                    super.onSuccess(statusCode, headers, response)
                    try {
                        if (baseConfig.lastLocation != location) {
                            baseConfig.lastLocation = location
                        }
                        if (historyCity.insertData(location)) {
                            historyAdapter.add(historyCity.getLastDate())
                            listSearchAdapter.notifyDataSetChanged()
                            listSearchAdapter.add(historyCity.getLastCity())
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
                        findForecast(lat, lon, sunrise, sunset)
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
                    if (!isResume) binding.progressLoading.visibility = View.GONE
                }
            })
        }
    }
    private fun getTranslate() {

    }
    private fun findForecast(lat: String, lon: String, sunrise: Long, sunset: Long) {
        val client = AsyncHttpClient()
        client.get(BuildApi.getMainForecast(lat, lon, getString(R.string.lang)), object : JsonHttpResponseHandler() {
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
        var i = 0
        do {
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
            i++
        } while (i < 8)
        return forecast
    }
    private val editCity: String get() = binding.edtCity.text.toString().trim()
}