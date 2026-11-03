package com.example.screenprint

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var debugText: TextView
    private lateinit var touchArea: View
    private lateinit var startBtn: Button
    private lateinit var passwordLayout: LinearLayout
    private lateinit var passwordInput: EditText
    private lateinit var savePassBtn: Button
    private lateinit var loginPassBtn: Button
    private lateinit var fallbackToggleBtn: Button

    private val eventBuffer = mutableListOf<MotionEvent>()
    private var currentStep = 0
    private val requiredSamples = 3
    private var isRegisterMode = false
    private var isAuthenticating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureStorage.init(this)
        setupUI()
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        statusText = TextView(this).apply {
            text = getString(R.string.welcome)
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }

        debugText = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
            setPadding(0, 0, 0, 16)
        }

        touchArea = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
            )
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.darker_gray))
        }

        startBtn = Button(this).apply {
            text = if (SecureStorage.hasRegistered()) getString(R.string.auth_title) else getString(R.string.register_title)
            setOnClickListener { prepareAction() }
        }

        fallbackToggleBtn = Button(this).apply {
            text = getString(R.string.fallback_btn)
            setOnClickListener { togglePasswordLayout() }
        }

        passwordLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 16, 0, 0)
        }

        passwordInput = EditText(this).apply {
            hint = getString(R.string.password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        savePassBtn = Button(this).apply {
            text = getString(R.string.save_btn)
            setOnClickListener { handleSavePassword() }
        }

        loginPassBtn = Button(this).apply {
            text = getString(R.string.login_btn)
            setOnClickListener { handlePasswordLogin() }
            visibility = if (SecureStorage.hasPassword()) View.VISIBLE else View.GONE
        }

        passwordLayout.addView(passwordInput)
        passwordLayout.addView(savePassBtn)
        passwordLayout.addView(loginPassBtn)

        layout.addView(statusText)
        layout.addView(debugText)
        layout.addView(touchArea)
        layout.addView(startBtn)
        layout.addView(fallbackToggleBtn)
        layout.addView(passwordLayout)
        
        setContentView(layout)

        touchArea.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
    }

    private fun prepareAction() {
        if (SecureStorage.hasRegistered()) {
            isAuthenticating = true
            isRegisterMode = false
            statusText.text = getString(R.string.auth_instructions)
            startBtn.visibility = View.GONE
        } else {
            isRegisterMode = true
            isAuthenticating = false
            currentStep = 0
            statusText.text = getString(R.string.register_instructions)
            startBtn.visibility = View.GONE
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        if (startBtn.visibility == View.VISIBLE) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                eventBuffer.clear()
                eventBuffer.add(MotionEvent.obtain(event))
            }
            MotionEvent.ACTION_MOVE -> {
                eventBuffer.add(MotionEvent.obtain(event))
            }
            MotionEvent.ACTION_UP -> {
                eventBuffer.add(MotionEvent.obtain(event))
                processTouch()
            }
        }
        return true
    }

    private fun processTouch() {
        val profile = BiometricEngine.extractProfile(eventBuffer)
        for (e in eventBuffer) e.recycle()
        eventBuffer.clear()

        if (profile == null) {
            Toast.makeText(this, getString(R.string.error_auth), Toast.LENGTH_SHORT).show()
            resetAction()
            return
        }

        if (isRegisterMode) {
            SecureStorage.saveSample(BiometricEngine.profileToString(profile), currentStep)
            currentStep++
            
            if (currentStep >= requiredSamples) {
                statusText.text = getString(R.string.register_success)
                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_LONG).show()
                debugText.text = ""
                resetAction()
                startBtn.text = getString(R.string.auth_title)
                // מבקשים סיסמה לאחר רישום
                passwordLayout.visibility = View.VISIBLE
                loginPassBtn.visibility = View.GONE
                statusText.append("\nאנא הגדר סיסמת גיבוי.")
            } else {
                val remaining = requiredSamples - currentStep
                statusText.text = getString(R.string.register_need_more, remaining)
            }
        } else if (isAuthenticating) {
            val storedStrings = SecureStorage.getSamples()
            val storedProfiles = storedStrings.mapNotNull { BiometricEngine.stringToProfile(it) }
            
            val score = SecurityMatcher.getBestMatchScore(storedProfiles, profile)
            debugText.text = "ציון התאמה: $score / 100"
            
            if (score >= 85) {
                statusText.text = getString(R.string.success_unlock)
                Toast.makeText(this, getString(R.string.success_unlock), Toast.LENGTH_LONG).show()
                touchArea.visibility = View.GONE
                isAuthenticating = false
            } else {
                statusText.text = getString(R.string.error_auth)
                Toast.makeText(this, getString(R.string.error_auth), Toast.LENGTH_SHORT).show()
                resetAction()
            }
        }
    }

    private fun handleSavePassword() {
        val pass = passwordInput.text.toString()
        if (pass.length >= 4) {
            SecureStorage.saveFallbackPassword(pass)
            Toast.makeText(this, "הסיסמה נשמרה בהצלחה", Toast.LENGTH_SHORT).show()
            passwordInput.text.clear()
            passwordLayout.visibility = View.GONE
            loginPassBtn.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 4 תווים", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePasswordLogin() {
        val pass = passwordInput.text.toString()
        if (SecureStorage.checkFallbackPassword(pass)) {
            Toast.makeText(this, getString(R.string.success_unlock), Toast.LENGTH_LONG).show()
            statusText.text = getString(R.string.success_unlock)
            touchArea.visibility = View.GONE
            startBtn.visibility = View.GONE
            passwordLayout.visibility = View.GONE
            fallbackToggleBtn.visibility = View.GONE
        } else {
            Toast.makeText(this, getString(R.string.error_password), Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePasswordLayout() {
        if (!SecureStorage.hasRegistered()) {
            Toast.makeText(this, getString(R.string.need_register_first), Toast.LENGTH_SHORT).show()
            return
        }
        if (passwordLayout.visibility == View.GONE) {
            passwordLayout.visibility = View.VISIBLE
            // אם כבר יש סיסמה, לא צריך כפתור שמירה
            savePassBtn.visibility = if (SecureStorage.hasPassword()) View.GONE else View.VISIBLE
        } else {
            passwordLayout.visibility = View.GONE
        }
    }

    private fun resetAction() {
        startBtn.visibility = View.VISIBLE
    }
}
