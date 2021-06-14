package com.example.part2_chap4_calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.part2_chap4_calculator.model.History
import java.lang.NumberFormatException
import java.math.BigInteger


class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById<TextView>(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy {
        findViewById<TextView>(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById<View>(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy {
        findViewById<LinearLayout>(R.id.historyLinearLayout)
    }

    private val backSpaceButton: AppCompatButton by lazy {
        findViewById<AppCompatButton>(R.id.backSpaceButton)
    }

    lateinit var db: AppDatabase

    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBackSpaceButton()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "HistoryDB"
        ).build()

    }

    private fun initBackSpaceButton() {
        backSpaceButton.setOnClickListener {
            if (expressionTextView.text.isEmpty()) {
                return@setOnClickListener
            }
            if (expressionTextView.text.endsWith(" ")) {
                expressionTextView.text = expressionTextView.text.dropLast(3)
                isOperator = false
                hasOperator = false
                return@setOnClickListener
            }
            expressionTextView.text = expressionTextView.text.dropLast(1)
        }
    }

    fun buttonClicked(v: View) {
        when(v.id) {
            R.id.Button0 -> numberButtonClicked("0")
            R.id.Button1 -> numberButtonClicked("1")
            R.id.Button2 -> numberButtonClicked("2")
            R.id.Button3 -> numberButtonClicked("3")
            R.id.Button4 -> numberButtonClicked("4")
            R.id.Button5 -> numberButtonClicked("5")
            R.id.Button6 -> numberButtonClicked("6")
            R.id.Button7 -> numberButtonClicked("7")
            R.id.Button8 -> numberButtonClicked("8")
            R.id.Button9 -> numberButtonClicked("9")
            R.id.ButtonPlus -> operatorButtonClicked("+")
            R.id.ButtonMinus -> operatorButtonClicked("-")
            R.id.ButtonMulti -> operatorButtonClicked("*")
            R.id.ButtonDivision -> operatorButtonClicked("/")
            R.id.ButtonRemainder -> operatorButtonClicked("%")
        }
    }

    private fun numberButtonClicked(number: String) {

        if (isOperator) {
//            expressionTextView.append(" ")
            isOperator = false
        }

        val expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this, "15자리 까지 입력 가능합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (expressionText.last().isEmpty() && number == "0") {
            Toast.makeText(this, "0은 첫번째 자리에 올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        expressionTextView.append(number)
        resultTextView.text = calculateExpression()
    }

    private fun operatorButtonClicked(operator: String) {
        if (expressionTextView.text.isEmpty())
            return
        when {
            isOperator -> {
                expressionTextView.text = expressionTextView.text.dropLast(2)
                expressionTextView.append("$operator ")
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한번만 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionTextView.append(" $operator ")
                isOperator = true
                hasOperator = true
            }
        }
        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.blue)),
            expressionTextView.text.length - 2,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        expressionTextView.text = ssb
    }

    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible = true
        historyLinearLayout.removeAllViews()
        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach {
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()
    }

    fun historyClearButtonClicked(v: View) {
        historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().deleteAll()
        }).start()

    }

    fun closeButtonClicked(v: View) {
        historyLayout.isVisible = false
    }

    fun resultButtonClicked(v: View) {
        val expressionTexts = expressionTextView.text.split(" ")
        if (expressionTextView.text.isEmpty() || expressionTexts.size == 1) {
            Toast.makeText(this, "수식을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (expressionTexts.size != 3 && hasOperator) {
            Toast.makeText(this, "수식이 완성되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!expressionTexts[0].isNumber() || !expressionTexts[2].isNumber()) {
            Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val expressionText = expressionTextView.text.toString()
        val resultText = resultTextView.text.toString() // calculateExpression?

        Thread (Runnable {
            db.historyDao().insertHistory(History(null, expressionText, resultText))
        }).start()

        clearButtonClicked(v)
        expressionTextView.text = resultText
    }

    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")

        if (!hasOperator || expressionTexts.size != 3) {
            return ""
        }
        else if (!expressionTexts[0].isNumber() || !expressionTexts[2].isNumber()) {
            return ""
        }
        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()
        val op = expressionTexts[1]

        val result: BigInteger = when(op) {
            "+" -> exp1 + exp2
            "-" -> exp1 - exp2
            "*" -> exp1 * exp2
            "/" -> exp1 / exp2
            "%" -> exp1 % exp2
            else -> exp1 - exp2
        }
        return result.toString()
    }
}

private fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        true
    } catch (e: NumberFormatException) {
        false
    }
}