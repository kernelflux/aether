# Aether Framework

A powerful Android modular development framework based on SPI mechanism.

## 📁 模块结构

### 基础模块组（Base Group）
- `base/aether-utils` - 纯工具类（无Android依赖）
- `base/aether-ui` - UI基础组件
- `base/aether-common` - 通用工具（Android相关）

### 核心模块组（Core Group）
- `core/aether-imageloader-api` - 图片加载接口
- `core/aether-imageloader-glide` - Glide图片加载实现
- `core/aether-network-api` - 网络请求接口
- `core/aether-network-okhttp` - OkHttp网络实现
- `core/aether-log-api` - 日志接口
- `core/aether-log-xlog` - XLog高性能日志实现（基于Mars xlog）
- `core/aether-log-android` - Android日志实现（轻量级）
- `core/aether-kv-api` - 键值存储接口
- `core/aether-kv-mmkv` - MMKV存储实现

### 功能模块组（Feature Group）
- `feature/aether-payment-api` - 支付服务接口
- `feature/aether-payment-alipay` - 支付宝支付实现
- `feature/aether-payment-wechat` - 微信支付实现
- `feature/aether-payment-google` - 谷歌支付实现
- `feature/aether-share-api` - 分享服务接口
- `feature/aether-share-wechat` - 微信分享实现
- `feature/aether-login-api` - 登录服务接口
- `feature/aether-login-oauth` - OAuth登录实现

## 🏗️ 架构设计

### 模块分组
```
aether/
├── base/          # 基础模块组（工具类和UI组件）
├── core/          # 核心模块组（基础设施服务）
├── feature/       # 功能模块组（业务功能服务）
└── sample/        # 示例应用
```

### 依赖关系
```
业务模块（Feature Group）
  ↓
核心模块（Core Group）
  ↓
基础模块（Base Group）
```

## 🚀 快速开始

### 添加依赖

```kotlin
dependencies {
    // 基础模块
    implementation(project(":base:aether-utils"))
    implementation(project(":base:aether-ui"))
    implementation(project(":base:aether-common"))
    
    // 核心模块
    implementation(project(":core:aether-network-api"))
    implementation(project(":core:aether-network-okhttp"))
    
    // 功能模块
    implementation(project(":feature:aether-payment-api"))
    implementation(project(":feature:aether-payment-alipay"))
}
```

## 📚 使用示例

### 使用工具类
```kotlin
import com.kernelflux.aether.utils.StringUtils
import com.kernelflux.aether.utils.DateUtils

val isEmpty = StringUtils.isEmpty(str)
val now = DateUtils.formatNow()
```

### 使用UI基础组件
```kotlin
import com.kernelflux.aether.ui.BaseActivity
import com.kernelflux.aether.ui.BaseFragment

class MyActivity : BaseActivity() {
    override fun initView() {
        setContentView(R.layout.activity_main)
    }
}
```

### 使用日志服务
```kotlin
import com.kernelflux.aether.log.api.ILogger
import com.kernelflux.aether.log.api.LoggerConfig
import com.kernelflux.aether.log.api.LogLevel
import com.kernelflux.aether.log.api.FileConfig
import com.kernelflux.fluxrouter.core.FluxRouter
import java.io.File

// 在 Application.onCreate() 中初始化
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
            maxAliveTime = 7 * 24 * 60 * 60 * 1000L, // 7天
            cacheDays = 3,
            compressEnabled = true,
            customHeaderInfo = mapOf(
                "Device" to Build.MODEL,
                "App Version" to "1.0.0"
            )
        )
    )
)

// 使用日志
logger?.d("Tag", "Debug message")
logger?.i("Tag", "Info message")
logger?.e("Tag", "Error message", exception)
```

### 使用支付服务
```kotlin
import com.kernelflux.aether.payment.api.IPaymentService
import com.kernelflux.fluxrouter.core.FluxRouter

val paymentService = FluxRouter.getService(IPaymentService::class.java)
paymentService?.pay(activity, order, callback)
```

## 🌍 国际化支持

Aether框架使用**Android标准的Resources系统**处理多语言。

### 使用方式

```kotlin
import com.kernelflux.aether.common.ResourceHelper
import com.kernelflux.aether.payment.api.PaymentResourceKeys

val message = ResourceHelper.getString(
    context,
    PaymentResourceKeys.PAYMENT_SUCCESS,
    "Payment successful"
)
```

## 📖 文档

- [模块分组说明](./MODULE_GROUPING_COMPLETE.md)

## 📄 License

See [LICENSE](./LICENSE) file.
