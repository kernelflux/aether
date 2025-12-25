# Aether Framework

A powerful Android modular development framework based on SPI mechanism.

**[中文版](README.zh-CN.md) | [English](README.md)**

## 📁 Module Structure

### Base Group
- `base/aether-utils` - Pure utility classes (no Android dependency)
- `base/aether-ui` - UI base components
- `base/aether-common` - Common utilities (Android related)

### Core Group
- `core/aether-imageloader-api` - Image loading interface
- `core/aether-imageloader-glide` - Glide image loading implementation
- `core/aether-network-api` - Network request interface
- `core/aether-network-okhttp` - OkHttp network implementation
- `core/aether-log-api` - Logging interface
- `core/aether-log-xlog` - XLog high-performance logging implementation (based on Mars xlog)
- `core/aether-log-android` - Android logging implementation (lightweight)
- `core/aether-kv-api` - Key-value storage interface
- `core/aether-kv-mmkv` - MMKV storage implementation

### Feature Group
- `feature/aether-payment-api` - Payment service interface
- `feature/aether-payment-alipay` - Alipay payment implementation
- `feature/aether-payment-wechat` - WeChat payment implementation
- `feature/aether-payment-google` - Google payment implementation
- `feature/aether-share-api` - Share service interface
- `feature/aether-share-wechat` - WeChat share implementation
- `feature/aether-login-api` - Login service interface
- `feature/aether-login-oauth` - OAuth login implementation

## 🏗️ Architecture Design

### Module Grouping
```
aether/
├── base/          # Base Group (utility classes and UI components)
├── core/          # Core Group (infrastructure services)
├── feature/       # Feature Group (business feature services)
└── sample/        # Sample application
```

### Dependency Relationship
```
Feature Group (Business Modules)
  ↓
Core Group (Infrastructure Services)
  ↓
Base Group (Base Utilities)
```

## 🚀 Quick Start

### Add Dependencies

```kotlin
dependencies {
    // Base modules
    implementation(project(":base:aether-utils"))
    implementation(project(":base:aether-ui"))
    implementation(project(":base:aether-common"))
    
    // Core modules
    implementation(project(":core:aether-network-api"))
    implementation(project(":core:aether-network-okhttp"))
    
    // Feature modules
    implementation(project(":feature:aether-payment-api"))
    implementation(project(":feature:aether-payment-alipay"))
}
```

## 📚 Usage Examples

### Using Utility Classes
```kotlin
import com.kernelflux.aether.utils.StringUtils
import com.kernelflux.aether.utils.DateUtils

val isEmpty = StringUtils.isEmpty(str)
val now = DateUtils.formatNow()
```

### Using UI Base Components
```kotlin
import com.kernelflux.aether.ui.BaseActivity
import com.kernelflux.aether.ui.BaseFragment

class MyActivity : BaseActivity() {
    override fun initView() {
        setContentView(R.layout.activity_main)
    }
}
```

### Using Logging Service
```kotlin
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.aether.log.api.FileConfig
import com.kernelflux.fluxrouter.core.FluxRouter
import java.io.File

// Initialize in Application.onCreate()
val logger = FluxRouter.getService(ILogger::class.java)
logger?.init(
    context = this,
    defaultConfig = LoggerConfig(
        level = LogLevel.DEBUG,
        consoleEnabled = true,
        fileEnabled = true,
        fileConfig = FileConfig(
            logDir = File(filesDir, "logs").absolutePath,
            cacheDir = File(cacheDir, "log_cache").absolutePath,
            namePrefix = "aether",
            maxFileSize = 10 * 1024 * 1024, // 10MB
            maxAliveTime = 7 * 24 * 60 * 60 * 1000L, // 7 days
            cacheDays = 3,
            compressEnabled = true,
            customHeaderInfo = mapOf(
                "Device" to Build.MODEL,
                "App Version" to "1.0.0"
            )
        )
    )
)

// Use logging
logger?.d("Tag", "Debug message")
logger?.i("Tag", "Info message")
logger?.e("Tag", "Error message", exception)
```

### Using Payment Service
```kotlin
import com.kernelflux.aether.payment.api.IPaymentService
import com.kernelflux.fluxrouter.core.FluxRouter

val paymentService = FluxRouter.getService(IPaymentService::class.java)
paymentService?.pay(activity, order, callback)
```

## 🌍 Internationalization Support

Aether framework uses **Android standard Resources system** for multi-language support.

### Usage

```kotlin
import com.kernelflux.aether.payment.api.ResourceHelper
import com.kernelflux.aether.payment.api.PaymentResourceKeys

val message = ResourceHelper.getString(
    context,
    PaymentResourceKeys.PAYMENT_SUCCESS,
    "Payment successful"
)
```

## 📖 Documentation

- [Module Grouping Guide](./MODULE_GROUPING_COMPLETE.md)

## 📄 License

See [LICENSE](./LICENSE) file.
