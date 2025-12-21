# Aether Utils Module

纯工具类模块，提供常用的工具方法，**无Android依赖**。

## 特点

- ✅ **纯Java/Kotlin**：不依赖Android Framework
- ✅ **轻量级**：只依赖Kotlin标准库
- ✅ **通用性**：可在任何Java/Kotlin项目中使用

## 内容

### StringUtils
字符串工具类
```kotlin
StringUtils.isEmpty(str)
StringUtils.isNotEmpty(str)
StringUtils.truncate(str, 10)
StringUtils.camelToUnderscore("camelCase") // "camel_case"
```

### DateUtils
日期时间工具类
```kotlin
DateUtils.formatNow() // "2024-01-01 12:00:00"
DateUtils.format(date, "yyyy-MM-dd")
DateUtils.isToday(date)
DateUtils.daysBetween(date1, date2)
```

### MathUtils
数学工具类
```kotlin
MathUtils.clamp(value, 0, 100)
MathUtils.lerp(start, end, 0.5f)
MathUtils.map(value, 0f, 100f, 0f, 1f)
```

### CollectionUtils
集合工具类
```kotlin
CollectionUtils.isEmpty(list)
CollectionUtils.first(list)
CollectionUtils.filter(list) { it > 0 }
```

## 使用

```kotlin
dependencies {
    api(project(":aether-utils"))
}
```

## 设计原则

- ✅ 纯工具方法，无状态
- ✅ 不依赖Android Framework
- ✅ 可在任何Java/Kotlin环境使用
