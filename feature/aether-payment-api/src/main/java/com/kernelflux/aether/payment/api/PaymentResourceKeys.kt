package com.kernelflux.aether.payment.api

/**
 * 支付模块资源键定义
 * 
 * 这些是字符串资源的名称（resource name），应用层需要在 res/values/strings.xml 中定义对应的资源
 * 
 * 使用方式：
 * ```kotlin
 * com.kernelflux.aether.payment.api.ResourceHelper.getString(context, PaymentResourceKeys.PAYMENT_SUCCESS, "Payment successful")
 * ```
 * 
 * 注意：框架层不提供资源文件，应用层需要在自己的 res/values/ 目录中定义这些资源
 * 
 * @author Aether Framework
 */
object PaymentResourceKeys {
    
    // 支付方式名称
    const val PAYMENT_TYPE_WECHAT = "payment_type_wechat"
    const val PAYMENT_TYPE_ALIPAY = "payment_type_alipay"
    const val PAYMENT_TYPE_GOOGLE_PAY = "payment_type_google_pay"
    
    // 支付状态
    const val PAYMENT_SUCCESS = "payment_success"
    const val PAYMENT_FAILED = "payment_failed"
    const val PAYMENT_CANCELLED = "payment_cancelled"
    const val PAYMENT_TIMEOUT = "payment_timeout"
    const val PAYMENT_PROCESSING = "payment_processing"
    
    // 错误消息
    const val ERROR_PAYMENT_DATA_EMPTY = "payment_error_data_empty"
    const val ERROR_ENVIRONMENT_NOT_READY = "payment_error_environment_not_ready"
    const val ERROR_NETWORK_ERROR = "payment_error_network"
    const val ERROR_TIMEOUT = "payment_error_timeout"
    const val ERROR_PAYMENT_FAILED = "payment_error_failed"
    const val ERROR_ORDER_NOT_FOUND = "payment_error_order_not_found"
    const val ERROR_PRODUCT_NOT_FOUND = "payment_error_product_not_found"
    const val ERROR_QUERY_FAILED = "payment_error_query_failed"
    const val ERROR_LAUNCH_FAILED = "payment_error_launch_failed"
    const val ERROR_VERIFICATION_FAILED = "payment_error_verification_failed"
    const val ERROR_SYSTEM_ERROR = "payment_error_system"
    const val ERROR_UNKNOWN = "payment_error_unknown"
    
    // 环境检查
    const val ENV_READY = "payment_env_ready"
    const val ENV_NOT_READY = "payment_env_not_ready"
    const val ENV_WECHAT_NOT_INSTALLED = "payment_env_wechat_not_installed"
    const val ENV_WECHAT_VERSION_NOT_SUPPORTED = "payment_env_wechat_version_not_supported"
    const val ENV_GOOGLE_PLAY_NOT_CONNECTED = "payment_env_google_play_not_connected"
    
    // 功能不支持
    const val FEATURE_NOT_SUPPORTED_QUERY = "payment_feature_not_supported_query"
    const val FEATURE_NOT_SUPPORTED_VERIFY = "payment_feature_not_supported_verify"
    
    // 订单验证
    const val VERIFICATION_SUCCESS = "payment_verification_success"
    const val VERIFICATION_FAILED = "payment_verification_failed"
    const val VERIFICATION_PROCESSING = "payment_verification_processing"
}
