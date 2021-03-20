package it.antoniocasto.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.TextView
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

class MainActivity : AppCompatActivity() {

    private lateinit var tvMetricLabel: TextView
    private lateinit var tvDateLabel: TextView
    private lateinit var radioButtonPositive: RadioButton
    private lateinit var radioButtonMax: RadioButton

    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>
    private lateinit var sparkView: SparkView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Connect id components
        tvMetricLabel = findViewById(R.id.tvMetricLabel)
        tvDateLabel = findViewById(R.id.tvDateLabel)
        radioButtonPositive = findViewById(R.id.radioButtonPositive)
        radioButtonMax = findViewById(R.id.radioButtonMax)
        sparkView = findViewById(R.id.sparkView)

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
                //TODO: Update spinner with state names

            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })

    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        // Create a new SparkAdapter with the data
        val adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter

        // Update radio buttons to select the positive cases and max time by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true

        // Display metric for the most recent date
        updateInfoForDate(dailyData.last()) //Chronological order

    }

    private fun updateInfoForDate(covidData: CovidData) {

        tvMetricLabel.text = NumberFormat.getInstance().format(covidData.positiveIncrease)
        val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)

    }
}