package com.info_turrim.testlockscreentv

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var alarmManager: AlarmManager
    private lateinit var screenOnIntent: PendingIntent
    private lateinit var screenOffIntent: PendingIntent

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            turnScreenOn()
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            turnScreenOff()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val screenOnFilter = IntentFilter(SCREEN_ON_ACTION)
        registerReceiver(screenOnReceiver, screenOnFilter)

        val screenOffFilter = IntentFilter(SCREEN_OFF_ACTION)
        registerReceiver(screenOffReceiver, screenOffFilter)
        devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(this, AdminReceiver::class.java)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MyApp:MyWakelockTag"
        )
        checkDeviceAdmin()

        findViewById<Button>(R.id.btn_lock).setOnClickListener {
            val hourLock = findViewById<EditText>(R.id.et_hour_lock).text.toString().toInt()
            val minuteLock = findViewById<EditText>(R.id.et_minute_lock).text.toString().toInt()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hourLock)
            calendar.set(Calendar.MINUTE, minuteLock)

            val screenOffIntent = Intent(SCREEN_OFF_ACTION).setPackage(packageName)
            this.screenOffIntent = PendingIntent.getBroadcast(this, 0, screenOffIntent, 0)
            alarmManager.cancel(this.screenOffIntent)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis + SCREEN_OFF_DELAY,
                AlarmManager.INTERVAL_DAY,
                this.screenOffIntent
            )

//            Toast.makeText(this, "Screen will be locked in 5 minutes", Toast.LENGTH_SHORT).show()
//            scheduleScreenOffWorker()
        }

        findViewById<Button>(R.id.btn_set_wakeup).setOnClickListener {

            val hourWakeUp = findViewById<EditText>(R.id.et_hour_on).text.toString().toInt()
            val minuteWakeUp = findViewById<EditText>(R.id.et_minute_on).text.toString().toInt()

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hourWakeUp)
            calendar.set(Calendar.MINUTE, minuteWakeUp)

            val screenOnIntent = Intent(SCREEN_ON_ACTION).setPackage(packageName)
            this.screenOnIntent = PendingIntent.getBroadcast(this, 0, screenOnIntent, 0)
            alarmManager.cancel(this.screenOnIntent)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                this.screenOnIntent
            )
//            scheduleScreenOnWorker(hour, minute)
        }

    }

    fun turnScreenOn() {
        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable admin to turn screen on"
            )
            startActivityForResult(intent, 1)
        } else {
            wakeLock.acquire(1*60*1000L /*10 minutes*/)
        }
    }

    fun turnScreenOff() {
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable admin to turn screen on"
            )
            startActivityForResult(intent, 1)
        }
    }

    private fun checkDeviceAdmin() {
        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable admin to turn screen on"
            )
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                wakeLock.acquire(1*60*1000L /*10 minutes*/)
            }
        }
    }

    private fun scheduleScreenOnWorker(hour: Int = 7, minutes: Int = 0) {
        val workRequest: WorkRequest = PeriodicWorkRequestBuilder<ScreenOnWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(hour, minutes), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    private fun scheduleScreenOffWorker() {
        val workRequest: WorkRequest = PeriodicWorkRequestBuilder<ScreenOffWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(1 * 10 * 1000L, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    private fun calculateInitialDelay(hour: Int = 7, minutes: Int = 0): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val desiredTimeMillis = getDesiredTimeMillis(hour, minutes)

        return if (currentTimeMillis > desiredTimeMillis) {
            desiredTimeMillis + TimeUnit.DAYS.toMillis(1) - currentTimeMillis
        } else {
            desiredTimeMillis - currentTimeMillis
        }
    }

    private fun getDesiredTimeMillis(hour: Int = 7, minutes: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }

    companion object {
        private const val SCREEN_ON_ACTION = "com.example.yourapp.SCREEN_ON"
        private const val SCREEN_OFF_ACTION = "com.example.yourapp.SCREEN_OFF"
        private const val SCREEN_OFF_DELAY = 1000L
    }
}
