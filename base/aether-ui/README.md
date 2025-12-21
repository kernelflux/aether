# Aether UI Module

UI基础组件模块，提供通用的Android UI基础类。

## 特点

- ✅ **基础组件**：BaseActivity、BaseFragment、BaseView、BaseDialog
- ✅ **统一接口**：所有基础类提供统一的初始化方法
- ✅ **易于扩展**：子类只需实现必要的方法

## 内容

### BaseActivity
基础Activity类
```kotlin
class MyActivity : BaseActivity() {
    override fun initView() {
        setContentView(R.layout.activity_main)
    }
    
    override fun initData() {
        // 初始化数据
    }
    
    override fun initListener() {
        // 设置监听器
    }
}
```

### BaseFragment
基础Fragment类
```kotlin
class MyFragment : BaseFragment() {
    override fun getLayoutId(): Int = R.layout.fragment_main
    
    override fun initView(view: View) {
        // 初始化视图
    }
    
    override fun initData() {
        // 初始化数据
    }
    
    override fun initListener() {
        // 设置监听器
    }
}
```

### BaseView
基础View类
```kotlin
class MyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseView(context, attrs) {
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // 初始化View
    }
}
```

### BaseDialog
基础Dialog类
```kotlin
class MyDialog(context: Context) : BaseDialog(context) {
    override fun getLayoutId(): Int = R.layout.dialog_main
    
    override fun initView() {
        // 初始化视图
    }
    
    override fun initData() {
        // 初始化数据
    }
    
    override fun initListener() {
        // 设置监听器
    }
}
```

## 使用

```kotlin
dependencies {
    api(project(":aether-ui"))
}
```

## 依赖

- `aether-utils` - 工具类模块
- `androidx.appcompat` - AndroidX兼容库
- `androidx.fragment` - Fragment支持
- `androidx.activity` - Activity支持
