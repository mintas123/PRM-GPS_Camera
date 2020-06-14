package pl.pjatk.s16604.project2.services

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class LocationJobService : JobService() {
    private var jobCancelled = false

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job started")
        startBackgroundWork(params)

        return false
    }

    private fun startBackgroundWork(params: JobParameters?) {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                if (jobCancelled){
                    return
                }
                Log.d(TAG, "thread started")
                sleep(3000)
                Log.d(TAG,"thread finished")
                jobFinished(params,false)
            }
        }
        thread.start()
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobCancelled = true
        Log.d(TAG, "Job cancelled before completion")
        return false
    }


    companion object {
        const val TAG = "XX_LOCATION_JOB"


    }
}