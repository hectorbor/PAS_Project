package com.mssde.pas_project.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

class RiegoPredictor(context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    private val mean = floatArrayOf(
        54.830f, 22.239f, 6.744f, 23.781f, 14.663f, 49.311f,
        7.496f, 57.218f, 24.694f, 2.959f, 1.499f, 2.498f
    )
    private val scale = floatArrayOf(
        26.100f, 9.995f, 1.013f, 10.628f, 11.138f, 28.605f,
        8.957f, 21.614f, 14.408f, 1.989f, 1.113f, 0.845f
    )

    init {
        val modelBytes = context.assets.open("riego_cesped.onnx").readBytes()
        session = env.createSession(modelBytes)
    }

    private fun normalize(value: Float, index: Int) = (value - mean[index]) / scale[index]

    fun predecir(
        humedadSuelo: Float,
        tempSuelo: Float,
        ph: Float,
        tempMax: Float,
        tempMin: Float,
        probLluvia: Float,
        lluvia24h: Float,
        humedadAire: Float,
        viento: Float,
        diasSinRiego: Float,
        estacion: Float,  // 0=invierno, 1=primavera, 2=verano, 3=otoño
        eto: Float
    ): Pair<Boolean, Float> {

        val inputData = floatArrayOf(
            normalize(humedadSuelo, 0),
            normalize(tempSuelo, 1),
            normalize(ph, 2),
            normalize(tempMax, 3),
            normalize(tempMin, 4),
            normalize(probLluvia, 5),
            normalize(lluvia24h, 6),
            normalize(humedadAire, 7),
            normalize(viento, 8),
            normalize(diasSinRiego, 9),
            normalize(estacion, 10),
            normalize(eto, 11)
        )

        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(inputData),
            longArrayOf(1, 12)
        )

        val inputs = mapOf("input" to inputTensor)
        val output = session.run(inputs)
        val resultado = (output[0].value as Array<FloatArray>)[0][0]

        inputTensor.close()
        output.close()

        return Pair(resultado >= 0.5f, resultado)
    }

    fun close() {
        session.close()
        env.close()
    }
}