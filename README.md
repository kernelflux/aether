# Aether - Android Agile Development Framework

Aether is an Android modular development framework based on the SPI (Service Provider Interface) mechanism. It uses KSP annotation technology to achieve automatic module discovery and registration, allowing developers to easily combine functional modules and business modules to build applications.

## Architecture Design

### Core Concepts

- **SPI Mechanism**: Decouples modules by separating interfaces from implementations
- **Auto Discovery**: Uses KSP annotation processor to automatically discover and register service providers
- **Modularity**: Functional modules and business modules are independent and can be plugged in or replaced
- **Usability**: Unified API interface, simple and easy to use

### Module Structure

```
Aether/
├── Functional Modules/
│   ├── aether-imageloader-api/     # Image loader API interface
│   ├── aether-imageloader-glide/   # Glide implementation
│   ├── aether-network-api/         # Network request API interface
│   ├── aether-network-okhttp/      # OkHttp implementation
│   ├── aether-log-api/             # Logging API interface
│   └── aether-log-android/         # Android Log implementation
├── Business Modules/
│   ├── aether-payment-api/         # Payment API interface
│   ├── aether-payment-alipay/      # Alipay implementation
│   ├── aether-share-api/           # Share API interface
│   ├── aether-share-wechat/        # WeChat share implementation
│   ├── aether-login-api/           # Login API interface
│   └── aether-login-oauth/         # OAuth login implementation
└── sample/                         # Sample application
```

## Quick Start

### 1. Initialize Framework

Initialize Aether in your Application:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Automatically register all services annotated with @ServiceProvider
        ServiceProviderInitializer.initialize(this)
        
        // Initialize Aether framework
        Aether.init(this)
    }
}
```

### 2. Using Functional Modules

#### Image Loading

```kotlin
// Get image loader service
val imageLoader = FluxRouter.getService<IImageLoader>()

// Simple load
imageLoader?.load("https://example.com/image.jpg")?.into(imageView)

// With configuration
imageLoader?.load("https://example.com/image.jpg")
    ?.placeholder(R.drawable.placeholder)
    ?.error(R.drawable.error)
    ?.circle()
    ?.into(imageView)

// Lifecycle-aware (Activity/Fragment/View)
imageLoader?.with(activity)
    ?.load("https://example.com/image.jpg")
    ?.radius(8f)
    ?.into(imageView)
```

#### Network Request

```kotlin
// Get network client
val networkClient = Aether.getService<INetworkClient>()

// Initialize network configuration
networkClient?.init(NetworkConfig(
    baseUrl = "https://api.example.com/",
    connectTimeout = 30_000
))

// Make GET request
networkClient?.get(
    url = "/api/user",
    params = mapOf("id" to "123"),
    responseType = UserResponse::class.java,
    callback = object : NetworkCallback<UserResponse> {
        override fun onSuccess(data: UserResponse) {
            // Handle success
        }
        override fun onError(error: Throwable) {
            // Handle error
        }
    }
)
```

#### Logging

```kotlin
// Get logging service
val logger = Aether.getService<ILogger>()

// Set log level
logger?.setLogLevel(LogLevel.DEBUG)

// Use logger
logger?.d("TAG", "Debug message")
logger?.e("TAG", "Error message", exception)
```

### 3. Using Business Modules

#### Login

```kotlin
val loginService = Aether.getService<ILoginService>()

loginService?.login(
    activity = this,
    callback = object : LoginCallback {
        override fun onSuccess(userInfo: UserInfo) {
            // Login successful
        }
        override fun onError(error: Throwable) {
            // Login failed
        }
        override fun onCancel() {
            // User cancelled
        }
    }
)
```

#### Payment

```kotlin
val paymentService = Aether.getService<IPaymentService>()

paymentService?.pay(
    activity = this,
    order = PaymentOrder(
        orderId = "order_123",
        amount = 99.99,
        subject = "Product Name"
    ),
    callback = object : PaymentCallback {
        override fun onSuccess(orderId: String, amount: Double) {
            // Payment successful
        }
        override fun onError(error: Throwable) {
            // Payment failed
        }
        override fun onCancel() {
            // User cancelled
        }
    }
)
```

#### Share

```kotlin
val shareService = Aether.getService<IShareService>()

shareService?.share(
    activity = this,
    content = ShareContent(
        type = ShareType.LINK,
        title = "Share Title",
        content = "Share Content",
        linkUrl = "https://example.com"
    )
)
```

## Extension Development

### Creating Custom Service Providers

1. **Implement SPI Interface**

```kotlin
@ServiceProvider(priority = 100)
class MyImageLoader : IImageLoader {
    override fun getProviderId(): String = "my_loader"
    
    override fun initialize(context: Context) {
        // Initialization logic
    }
    
    // Implement interface methods...
}
```

2. **Add Dependencies**

In your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":aether-core"))
    implementation(project(":aether-imageloader-api"))
    ksp(project(":aether-core"))
}
```

3. **Usage**

Automatically registered in Application, use directly:

```kotlin
val imageLoader = Aether.getService<IImageLoader>()
```

## Design Advantages

1. **Decoupling**: Interface and implementation separation, easy to replace
2. **Extensible**: Easily add new functional modules and business modules
3. **Auto Discovery**: Uses KSP for automatic registration, no manual configuration needed
4. **Priority**: Supports multiple implementations, selected by priority
5. **Unified API**: All services obtained through unified Aether API

## Technology Stack

- **Kotlin**: Primary development language
- **KSP**: Annotation processing and code generation
- **Gradle**: Build tool
- **SPI Pattern**: Service Provider Interface pattern

## References

- Referenced design concepts from mainstream solutions like ARouter, WMRouter, etc.
- Uses SPI mechanism to achieve module decoupling
- Uses KSP instead of APT to improve compilation performance

## License

Copyright © 2025 kernelflux

Licensed under the MIT License.
