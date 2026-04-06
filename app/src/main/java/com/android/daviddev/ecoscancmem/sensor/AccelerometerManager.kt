package com.android.daviddev.ecoscancmem.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class AccelerometerManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    //  TYPE_LINEAR_ACCELERATION já exclui a gravidade —
    //  não é necessário aplicar low-pass filter manual

    var onReadingChanged: ((x: Float, y: Float, z: Float) -> Unit)? = null

    val isAvailable: Boolean get() = accelerometer != null

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(
                this, it, SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        onReadingChanged?.invoke(
            event.values[0],
            event.values[1],
            event.values[2]
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}