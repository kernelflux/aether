# Aether - Android敏捷开发框架

Aether是一个基于SPI（Service Provider Interface）机制的Android模块化开发框架，通过KSP注解技术实现模块的自动发现和注册，让开发者可以轻松组合功能模块和业务模块来构建应用。

## 架构设计

### 核心思想

- **SPI机制**：通过接口与实现分离，实现模块解耦
- **自动发现**：使用KSP注解处理器自动发现和注册服务提供者
- **模块化**：功能模块和业务模块独立，可插拔替换
- **易用性**：统一的API接口，简单易用

### 模块结构

```
Aether/
├── aether-core/                    # 核心模块（SPI机制、服务注册）
├── 功能模块/
│   ├── aether-image-spi/           # 图片加载SPI接口
│   ├── aether-image-impl-glide/    # Glide实现
│   ├── aether-network-spi/          # 网络请求SPI接口
│   ├── aether-network-impl-okhttp/ # OkHttp实现
│   ├── aether-log-spi/              # 日志SPI接口
│   └── aether-log-impl-android/     # Android Log实现
├── 业务模块/
│   ├── aether-payment-spi/          # 支付SPI接口
│   ├── aether-payment-impl-alipay/  # 支付宝实现
│   ├── aether-share-spi/            # 分享SPI接口
│   ├── aether-share-impl-wechat/    # 微信分享实现
│   ├── aether-login-spi/            # 登录SPI接口
│   └── aether-login-impl-oauth/     # OAuth登录实现
└── sample/                          # 示例应用
```

## 快速开始

### 1. 初始化框架

在Application中初始化Aether：

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 自动注册所有使用@ServiceProvider注解的服务
        ServiceProviderInitializer.initialize(this)
        
        // 初始化Aether框架
        Aether.init(this)
    }
}
```

### 2. 使用功能模块

#### 图片加载

```kotlin
// 获取图片加载服务
val imageLoader = Aether.getService<IImageLoader>()

// 加载图片
imageLoader?.load(imageView, "https://example.com/image.jpg")

// 加载圆形图片
imageLoader?.loadCircle(imageView, "https://example.com/avatar.jpg")

// 加载圆角图片
imageLoader?.loadRound(imageView, "https://example.com/image.jpg", radius = 8f)
```

#### 网络请求

```kotlin
// 获取网络客户端
val networkClient = Aether.getService<INetworkClient>()

// 初始化网络配置
networkClient?.init(NetworkConfig(
    baseUrl = "https://api.example.com/",
    connectTimeout = 30_000
))

// 发起GET请求
networkClient?.get(
    url = "/api/user",
    params = mapOf("id" to "123"),
    responseType = UserResponse::class.java,
    callback = object : NetworkCallback<UserResponse> {
        override fun onSuccess(data: UserResponse) {
            // 处理成功
        }
        override fun onError(error: Throwable) {
            // 处理错误
        }
    }
)
```

#### 日志

```kotlin
// 获取日志服务
val logger = Aether.getService<ILogger>()

// 设置日志级别
logger?.setLogLevel(LogLevel.DEBUG)

// 使用日志
logger?.d("TAG", "Debug message")
logger?.e("TAG", "Error message", exception)
```

### 3. 使用业务模块

#### 登录

```kotlin
val loginService = Aether.getService<ILoginService>()

loginService?.login(
    activity = this,
    callback = object : LoginCallback {
        override fun onSuccess(userInfo: UserInfo) {
            // 登录成功
        }
        override fun onError(error: Throwable) {
            // 登录失败
        }
        override fun onCancel() {
            // 用户取消
        }
    }
)
```

#### 支付

```kotlin
val paymentService = Aether.getService<IPaymentService>()

paymentService?.pay(
    activity = this,
    order = PaymentOrder(
        orderId = "order_123",
        amount = 99.99,
        subject = "商品名称"
    ),
    callback = object : PaymentCallback {
        override fun onSuccess(orderId: String, amount: Double) {
            // 支付成功
        }
        override fun onError(error: Throwable) {
            // 支付失败
        }
        override fun onCancel() {
            // 用户取消
        }
    }
)
```

#### 分享

```kotlin
val shareService = Aether.getService<IShareService>()

shareService?.share(
    activity = this,
    content = ShareContent(
        type = ShareType.LINK,
        title = "分享标题",
        content = "分享内容",
        linkUrl = "https://example.com"
    )
)
```

## 扩展开发

### 创建自定义服务提供者

1. **实现SPI接口**

```kotlin
@ServiceProvider(priority = 100)
class MyImageLoader : IImageLoader {
    override fun getProviderId(): String = "my_loader"
    
    override fun initialize(context: Context) {
        // 初始化逻辑
    }
    
    // 实现接口方法...
}
```

2. **添加依赖**

在模块的`build.gradle.kts`中：

```kotlin
dependencies {
    implementation(project(":aether-core"))
    implementation(project(":aether-image-spi"))
    ksp(project(":aether-core"))
}
```

3. **使用**

在Application中会自动注册，直接使用：

```kotlin
val imageLoader = Aether.getService<IImageLoader>()
```

## 设计优势

1. **解耦**：接口与实现分离，易于替换
2. **可扩展**：轻松添加新的功能模块和业务模块
3. **自动发现**：使用KSP自动注册，无需手动配置
4. **优先级**：支持多个实现，按优先级选择
5. **统一API**：所有服务通过统一的Aether API获取

## 技术栈

- **Kotlin**: 主要开发语言
- **KSP**: 注解处理和代码生成
- **Gradle**: 构建工具
- **SPI模式**: 服务提供者接口模式

## 参考

- 参考了业界主流方案如ARouter、WMRouter等的设计思想
- 采用SPI机制实现模块解耦
- 使用KSP替代APT，提升编译性能

## License

Copyright © 2025 Aether Framework

