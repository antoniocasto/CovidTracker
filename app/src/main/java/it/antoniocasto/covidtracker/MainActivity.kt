package it.antoniocasto.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.spark.SparkView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES = "All (nationwide)"


class MainActivity : AppCompatActivity() {

    private lateinit var adapter: CovidSparkAdapter
    private lateinit var tvMetricLabel: TextView
    private lateinit var tvDateLabel: TextView
    private lateinit var radioButtonPositive: RadioButton
    private lateinit var radioButtonMax: RadioButton
    private lateinit var radioGroupTimeSelection: RadioGroup
    private lateinit var radioGroupMetricSelection: RadioGroup
    private lateinit var sparkView: SparkView


    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>
    private lateinit var currentlyShownData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Connect id components
        tvMetricLabel = findViewById(R.id.tvMetricLabel)
        tvDateLabel = findViewById(R.id.tvDateLabel)
        radioButtonPositive = findViewById(R.id.radioButtonPositive)
        radioButtonMax = findViewById(R.id.radioButtonMax)
        sparkView = findViewById(R.id.sparkView)
        radioGroupTimeSelection = findViewById(R.id.radioGroupTimeSelection)
        radioGroupMetricSelection = findViewById(R.id.radioGroupMetricSelection)

        //Prepare retrofit to obtain structured data
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        //Fetch national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {

                Log.i(TAG, "onResponse $response")

                val nationalData = response.body()

                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }

                // Put here because we want the UI being reactive when we have data to show
                setupEventListeners()

                //Put old data first using reversed ordering
                nationalDailyData = nationalData.reversed()
                Log.i(TAG, "Update graph with national data")

                updateDisplayWithData(nationalDailyData)

            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })

        //Fetch the state data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(call: Call<List<CovidData>>, response: Response<List<CovidData>>) {

                Log.i(TAG, "onResponse $response")

                val statesData = response.body()

                if (statesData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                //Put old data first using reversed ordering
                perStateDailyData = statesData.reversed().groupBy { it.state }

                Log.i(TAG, "Update spinner with state names")
                //Update spinner with state names

                updateSpinnerWithStateData(perStateDailyData.keys)

            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })

    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList = stateNames.toMutableList()
        stateAbbreviationList.sort()
        stateAbbreviationList.add(0, ALL_STATES)

        // Add state list as data source fot the spinner
    }

    private fun setupEventListeners() {

        // Add a listener for the user scrubbing on the chart
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->

            if (itemData is CovidData) { //Safety check. If it's something else the UI is not responding
                updateInfoForDate(itemData)
            }

        }

        // Respond to radio button selected events
        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButton3Month -> updateDisplayTimeScale(TimeScale.THREEMONTH)
                R.id.radioButtonMonth -> updateDisplayTimeScale(TimeScale.MONTH)
                R.id.radioButtonMax -> updateDisplayTimeScale(TimeScale.MAX)
            }
        }

        radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            // Different just for exercise
            when (checkedId) {
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
            }
        }
    }

    private fun updateDisplayTimeScale(timeScale: TimeScale) {
        adapter.daysAgo = timeScale
        adapter.notifyDataSetChanged()
    }

    private fun updateDisplayMetric(metric: Metric) {
        // Update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)
        sparkView.lineColor = colorInt
        tvMetricLabel.setTextColor(colorInt)

        // Update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset number and date shown in the bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData

        // Create a new SparkAdapter with the data
        adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter

        // Update radio buttons to select the positive cases and max time by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true

        // Display metric for the most recent date in the bottom part of the UI
        updateDisplayMetric(Metric.POSITIVE) //Chronological order

    }

    private fun updateInfoForDate(covidData: CovidData) {

        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }

        tvMetricLabel.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)

    }
}