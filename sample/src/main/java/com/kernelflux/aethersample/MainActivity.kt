package com.kernelflux.aethersample

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Aether框架主页面 - 服务列表
 *
 * @author Aether Framework
 */
class MainActivity : BaseActivity() {

    private val services = listOf(
        ServiceItem(
            "Log Service",
            "Logging Service Demo",
            LogActivity::class.java
        ),
        ServiceItem(
            "Image Loader",
            "Image Loader Service",
            ImageLoaderActivity::class.java
        ),
        ServiceItem(
            "Network",
            "Network Request Service",
            NetworkActivity::class.java
        ),
        ServiceItem(
            "KV Store",
            "Key-Value Store Service",
            KVActivity::class.java
        ),
        ServiceItem(
            "Login",
            "Login Service",
            LoginActivity::class.java
        ),
        ServiceItem(
            "Payment",
            "Payment Service",
            PaymentActivity::class.java
        ),
        ServiceItem(
            "Share",
            "Share Service",
            ShareActivity::class.java
        )
    )

    override fun getContentResId(): Int = R.layout.activity_main


    override fun onInitView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ServiceAdapter(services) { service ->
            startActivity(Intent(this, service.activityClass))
        }
    }

    data class ServiceItem(
        val name: String,
        val description: String,
        val activityClass: Class<*>
    )

    class ServiceAdapter(
        private val items: List<ServiceItem>,
        private val onItemClick: (ServiceItem) -> Unit
    ) : RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameView: TextView = itemView.findViewById(R.id.service_name)
            val descView: TextView = itemView.findViewById(R.id.service_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_service, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.nameView.text = item.name
            holder.descView.text = item.description
            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
