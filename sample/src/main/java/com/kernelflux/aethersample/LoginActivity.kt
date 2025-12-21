package com.kernelflux.aethersample

import android.widget.Button
import android.widget.TextView
import com.kernelflux.aether.login.api.ILoginService
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * 登录服务示例页面
 *
 * @author Aether Framework
 */
class LoginActivity : BaseActivity() {

    private var loginService: ILoginService? = null

    override fun getContentResId(): Int = R.layout.activity_login

    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Login"

        loginService = FluxRouter.getService(ILoginService::class.java)

        val statusText = findViewById<TextView>(R.id.status_text)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val logoutButton = findViewById<Button>(R.id.btn_logout)
        val checkButton = findViewById<Button>(R.id.btn_check)

        updateStatus(statusText)

        loginButton.setOnClickListener {
            loginService?.login(
                activity = this,
                callback = object : com.kernelflux.aether.login.api.LoginCallback {
                    override fun onSuccess(userInfo: com.kernelflux.aether.login.api.UserInfo) {
                        runOnUiThread {
                            statusText.text = "Login Success:\n${userInfo.nickname}\n${userInfo.userId}"
                            updateStatus(statusText)
                        }
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            statusText.text = "Login Error: ${error.message ?: ""}"
                        }
                    }

                    override fun onCancel() {
                        runOnUiThread {
                            statusText.text = "Login Cancelled"
                        }
                    }
                }
            )
        }

        logoutButton.setOnClickListener {
            loginService?.logout()
            updateStatus(statusText)
            statusText.text = "Logged out"
        }

        checkButton.setOnClickListener {
            updateStatus(statusText)
        }
    }

    private fun updateStatus(textView: TextView) {
        val isLoggedIn = loginService?.isLoggedIn() ?: false
        val user = loginService?.getCurrentUser()
        textView.text = if (isLoggedIn && user != null) {
            "Status: Logged In\nUser: ${user.nickname}\nID: ${user.userId}"
        } else {
            "Status: Not Logged In"
        }
    }
}
