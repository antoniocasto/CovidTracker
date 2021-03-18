package it.antoniocasto.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Prepare retrofit to obtain structured data
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)

        //Fetch national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {

                Log.i(TAG, "onResponse $response")

                val nationalData = response.body()

                if (nationalData == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                    return
                }
                //Put old data first using reversed ordering
                nationalDailyData = nationalData.reversed()

                Log.i(TAG, "Update graph with national data")
                //TODO: Update graph with national data

            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }

        })

        //Fetch the state data
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {

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
}