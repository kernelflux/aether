package com.kernelflux.aethersample

import android.widget.Button
import android.widget.TextView
import com.kernelflux.aether.share.api.IShareService
import com.kernelflux.fluxrouter.core.FluxRouter

/**
 * 分享服务示例页面
 *
 * @author Aether Framework
 */
class ShareActivity : BaseActivity() {

    private var shareService: IShareService? = null

    override fun getContentResId(): Int = R.layout.activity_share


    override fun onInitView() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Share"

        shareService = FluxRouter.getService(IShareService::class.java)

        val statusText = findViewById<TextView>(R.id.status_text)
        val shareLinkButton = findViewById<Button>(R.id.btn_share_link)
        val shareTextButton = findViewById<Button>(R.id.btn_share_text)

        shareLinkButton.setOnClickListener {
            statusText.text = "Sharing link..."
            shareService?.share(
                activity = this,
                content = com.kernelflux.aether.share.api.ShareContent(
                    type = com.kernelflux.aether.share.api.ShareType.LINK,
                    title = "Aether Framework",
                    content = "A powerful Android modular development framework",
                    linkUrl = "https://github.com/kernelflux/aether"
                )
            )
            statusText.text = "Share link triggered"
        }

        shareTextButton.setOnClickListener {
            statusText.text = "Sharing text..."
            shareService?.share(
                activity = this,
                content = com.kernelflux.aether.share.api.ShareContent(
                    type = com.kernelflux.aether.share.api.ShareType.TEXT,
                    title = "Aether Framework",
                    content = "A powerful Android modular development framework based on SPI mechanism"
                )
            )
            statusText.text = "Share text triggered"
        }
    }
}
