package com.kernelflux.aethersample

import android.widget.Button
import android.widget.TextView
import com.kernelflux.aether.log.spi.ILogger
import com.kernelflux.aether.network.spi.INetworkClient
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * 网络请求服务示例页面
 *
 * @author Aether Framework
 */
class NetworkActivity : BaseActivity() {

    private var networkClient: INetworkClient? = null
    private var logger: ILogger? = null

    override fun getContentResId(): Int = R.layout.activity_network

    override fun onInitView() {

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Network"

        networkClient = FluxRouter.getService(INetworkClient::class.java)
        logger = FluxRouter.getService(ILogger::class.java)

        val resultText = findViewById<TextView>(R.id.result_text)
        val getButton = findViewById<Button>(R.id.btn_get)
        val postButton = findViewById<Button>(R.id.btn_post)

        getButton.setOnClickListener {
            resultText.text = "Loading..."
            networkClient?.get(
                url = "https://api.github.com/users/octocat",
                params = emptyMap(),
                headers = emptyMap(),
                responseType = String::class.java,
                callback = object : com.kernelflux.aether.network.spi.NetworkCallback<String> {
                    override fun onSuccess(data: String) {
                        runOnUiThread {
                            resultText.text = "Success:\n$data"
                        }
                        logger?.d("NetworkActivity", "GET success: $data")
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            resultText.text = "Error: ${error.message}"
                        }
                        logger?.e("NetworkActivity", "GET error", error)
                    }
                }
            )
        }

        postButton.setOnClickListener {
            resultText.text = "Loading..."
            networkClient?.post(
                url = "https://httpbin.org/post",
                body = mapOf("name" to "test", "value" to "123"),
                headers = emptyMap(),
                responseType = String::class.java,
                callback = object : com.kernelflux.aether.network.spi.NetworkCallback<String> {
                    override fun onSuccess(data: String) {
                        runOnUiThread {
                            resultText.text = "Success:\n$data"
                        }
                        logger?.d("NetworkActivity", "POST success: $data")
                    }

                    override fun onError(error: Throwable) {
                        runOnUiThread {
                            resultText.text = "Error: ${error.message}"
                        }
                        logger?.e("NetworkActivity", "POST error", error)
                    }
                }
            )
        }
    }
}
