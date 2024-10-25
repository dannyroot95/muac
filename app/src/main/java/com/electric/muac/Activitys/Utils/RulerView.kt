package com.electric.muac.Activitys.Utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class RulerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centralTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)  // Pintura para el texto central
    private var color: Int = Color.RED
    private var measurement: Float = 0.0f

    init {
        paint.color = color
        paint.strokeWidth = 2f  // Espesor de las líneas de graduación
        textPaint.color = Color.WHITE
        textPaint.textSize = 30f  // Tamaño del texto de los números

        centralTextPaint.color = Color.WHITE  // Puedes cambiar el color si lo deseas
        centralTextPaint.textSize = 50f  // Tamaño del texto más grande para el texto central
        centralTextPaint.textAlign = Paint.Align.CENTER  // Alineación central para el texto

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(color)

        // Dibuja el texto en el centro de la vista
        canvas.drawText("$measurement cm", width / 2f, height / 2f + centralTextPaint.textSize / 2, centralTextPaint)

        // Dibujar líneas de graduación y números
        val numberOfLines = 26 * 5  // Cada unidad tiene 5 sub-unidades (0.1 cada una)
        val spacing = width.toFloat() / numberOfLines

        for (i in 0..numberOfLines) {
            val x = i * spacing
            if (i % 5 == 0) {
                // Dibuja las líneas más largas para cada unidad completa (cada 5 sub-unidades)
                canvas.drawLine(x, 0f, x, 30f, paint)
                canvas.drawLine(x, height.toFloat(), x, height - 30f, paint)
                // Dibuja números cada 5 unidades completas (cada 25 sub-unidades)
                if (i % 25 == 0) {
                    val number = i / 5
                    canvas.drawText("$number", x - textPaint.measureText("$number") / 2, 50f, textPaint)
                    canvas.drawText("$number", x - textPaint.measureText("$number") / 2, height - 20f, textPaint)
                }
            } else {
                // Dibuja el carácter para sub-unidades tanto en la parte superior como en la inferior
                canvas.drawText("'", x - textPaint.measureText("'") / 2, 24f, textPaint)  // Ajustar este valor para bajarlo
                canvas.drawText("'", x - textPaint.measureText("'") / 2, height + 14f, textPaint)  // Ajustar este valor para subirlo
            }
        }
    }


    fun setMeasurement(value: Float) {
        measurement = value
        color = when {
            measurement <= 11.4f -> {
                textPaint.color = Color.WHITE  // Texto blanco sobre fondo rojo
                centralTextPaint.color = Color.WHITE
                Color.RED
            }
            measurement <= 12.4f -> {
                textPaint.color = Color.BLACK  // Texto negro sobre fondo amarillo
                centralTextPaint.color = Color.BLACK
                Color.YELLOW
            }
            else -> {
                textPaint.color = Color.WHITE  // Texto blanco sobre fondo verde
                centralTextPaint.color = Color.WHITE
                Color.rgb(0,145,53)  // Verde específico
            }
        }
        paint.color = color
        invalidate()
    }

}
