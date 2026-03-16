# 银发管家 - 产品功能详细设计文档

## 一、产品概述

### 1.1 产品名称
银发管家（Silver Hair Butler）

### 1.2 产品定位
专为老年人设计的智能健康管理与紧急救助 Android 应用，通过语音交互、紧急呼叫、用药提醒等功能，为老年人提供全方位的健康守护。

### 1.3 目标用户
- 60岁以上老年人
- 独居老人
- 慢性病患者（高血压、糖尿病等）
- 需要定期服药的老人

### 1.4 核心价值
- **安全守护**：一键紧急呼叫，快速联系家人和急救中心
- **健康管理**：用药提醒、健康咨询、语音陪伴
- **便捷操作**：大字体、语音交互、简化操作
- **亲情连接**：快速联系子女、社区医院

---

## 二、功能模块详细设计

### 2.1 首页（Home Screen）

#### 2.1.1 功能描述
应用主界面，展示核心功能入口和快捷操作。

#### 2.1.2 界面设计
- **顶部**：应用标题"银发管家" + 当前日期时间
- **紧急呼叫卡片**：红色大按钮，醒目显示"紧急呼叫"
- **功能网格**：4个主要功能入口
  - 语音聊天（蓝色）
  - 用药记录（绿色）
  - 紧急联系人（橙色）
  - 设置（灰色）
- **底部**：快捷操作栏

#### 2.1.3 交互设计
- 所有按钮尺寸 >= 80dp，方便老年人点击
- 点击反馈：按钮按下时颜色变深
- 语音播报：点击按钮时播报功能名称

---

### 2.2 紧急呼叫（Emergency Call）

#### 2.2.1 功能描述
提供快速拨打紧急电话的功能，包括120急救和紧急联系人。

#### 2.2.2 界面设计
- **顶部**：红色标题栏"紧急呼叫"
- **120急救按钮**：
  - 尺寸：全宽，高度120dp
  - 颜色：红色（#D32F2F）
  - 图标：急救图标
  - 文字："拨打 120 急救"，24sp，加粗
- **紧急联系人列表**：
  - 卡片式设计，白色背景
  - 显示：姓名、关系、电话号码
  - 操作：编辑、删除、拨打按钮

#### 2.2.3 联系人数据结构
```kotlin
data class EmergencyContact(
    val id: String,           // 唯一标识
    val name: String,         // 姓名（如：儿子、120急救）
    val phone: String,        // 电话号码
    val relationship: String  // 关系（如：子女、急救、医疗）
)
```

#### 2.2.4 拨打逻辑（关键设计）
**问题**：120拨打和普通联系人拨打的区别
**根本原因**：Android 系统对紧急号码（120/110/119）有特殊安全限制，无法直接自动拨打
**解决方案**：采用"引导式拨打"策略
```kotlin
// 辅助函数：紧急号码拨打 - 使用引导式策略
fun dialEmergencyNumber(phoneNumber: String) {
    // 使用 ACTION_DIAL 打开拨号盘，预填号码
    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(dialIntent)
    
    // 显示引导提示
    Toast.makeText(context, "请在拨号盘上点击绿色拨打按钮", Toast.LENGTH_LONG).show()
}

// 拨打电话的核心方法 - 采用务实的策略
fun makePhoneCallInternal(phoneNumber: String) {
    when {
        // 紧急号码（120/110/119）- 使用引导式拨打
        phoneNumber in listOf("120", "110", "119") -> {
            dialEmergencyNumber(phoneNumber)
        }
        // 普通号码 - 尝试直接拨打
        else -> {
            tryDirectDial(phoneNumber)
        }
    }
}
```

**拨打方式说明**：
- **紧急号码（120/110/119）**：打开拨号盘并预填号码，引导用户手动点击拨打
- **普通号码（儿子/女儿等）**：尝试直接拨打，如果失败则回退到拨号盘
            Toast.makeText(context, "请在拨号盘上点击拨打按钮", ...)
        }
    }, 800)
}
```

#### 2.2.5 权限要求
- `CALL_PHONE`：直接拨打电话（可选）
- `ACTION_DIAL`：打开拨号盘（不需要权限）

---

### 2.3 语音聊天（Voice Chat）

#### 2.3.1 功能描述
通过语音与AI助手对话，查询天气、健康咨询、用药提醒等。

#### 2.3.2 界面设计
- **顶部**：蓝色标题栏"语音聊天"
- **聊天记录区**：
  - 用户消息：右侧，蓝色气泡
  - AI回复：左侧，白色气泡
  - 自动滚动到最新消息
- **快捷问题按钮**：3个常用问题，横向排列
  - "今天天气怎么样？"
  - "适合出门散步吗？"
  - "我想去公园运动"
- **麦克风按钮**：
  - 尺寸：80dp圆形
  - 状态：绿色（待机）/ 红色（录音中）

#### 2.3.3 语音识别方案
**当前实现**：只保留方案2（RecognizerIntent），删除其他方案
**原因**：其他方案依赖 Google SpeechRecognizer，国内手机大多不支持
**推荐方案**：方案2使用系统语音识别界面，最稳定可靠

**未来优化**：集成科大讯飞语音识别 SDK
- 官网：https://www.xfyun.cn/
- 优势：中文识别准确率业界领先，支持离线识别
- 免费额度：50000次/天

#### 2.3.4 方案2实现（推荐）
```kotlin
// 使用 ActivityResultLauncher 获取结果
val speechResultLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == ComponentActivity.RESULT_OK) {
        val matches = result.data?.getStringArrayListExtra(
            RecognizerIntent.EXTRA_RESULTS
        )
        if (matches != null && matches.isNotEmpty()) {
            val userText = matches[0]
            // 处理识别结果
            processVoiceInput(userText)
        }
    }
}

fun startListening2() {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                 RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话")
    }
    speechResultLauncher.launch(intent)
}
```

#### 2.3.5 AI响应逻辑
```kotlin
fun generateAIResponse(input: String): String {
    return when {
        input.contains("天气") -> fetchWeatherData()
        input.contains("运动") -> "适度运动对身体很好..."
        input.contains("吃药") -> "请按时服药..."
        else -> "我听到了您说的话..."
    }
}
```

#### 2.3.6 天气查询功能
**当前实现**：模拟数据（随机生成）
**数据内容**：
- 城市：北京
- 温度：18-28°C（随机）
- 天气：晴朗/多云/阴天/小雨（随机）
- 湿度：30-70%（随机）
- 风力：微风/和风/清风（随机）
- 空气质量：优/良/轻度污染（随机）

**实际接入方案**：
```kotlin
// 接入和风天气 API
suspend fun fetchRealWeather(): WeatherData {
    val response = weatherApi.getCurrentWeather(
        city = "北京",
        appKey = "YOUR_API_KEY"
    )
    return parseWeatherResponse(response)
}
```

---

### 2.4 用药记录（Medication Record）

#### 2.4.1 功能描述
管理用药信息，包括药品名称、剂量、服用时间、药效等。

#### 2.4.2 数据结构
```kotlin
data class Medication(
    val id: String,                    // 唯一标识
    val name: String,                  // 药品名称
    val dosage: String,                // 每次服用剂量（如：1片）
    val frequency: String,             // 服用频率（如：每日一次）
    val times: List<String>,           // 服用时间列表（如：["08:00"]
    val startDate: String,             // 开始日期
    val endDate: String?,              // 结束日期（可选）
    val notes: String?,                // 备注（可选）
    val reminderEnabled: Boolean,      // 是否开启提醒
    val reminderMinutesBefore: Int,    // 提前提醒分钟数
    val effect: String?,               // 药效/作用（新增）
    val duration: String?              // 药效持续时间（新增）
)
```

#### 2.4.3 界面设计（已重构）
- **顶部**：绿色标题栏"用药记录" + 添加按钮
- **药品列表**：LazyColumn，卡片式布局
- **卡片设计**（完全重写）：
  - 使用 `Box` + `Column` 替代 `Card` + `Row`
  - 白色背景 + 灰色边框（2dp）
  - 圆角：12dp
  - 药品名称区域：蓝色背景 + 蓝色文字
  - 作用信息：绿色文字，带 💊 图标
  - 剂量/频率/时间：深灰色文字，带 📋⏰🕐 图标
  - 操作按钮：播报（蓝色）、删除（红色）、已服用（绿色）

#### 2.4.4 关键修复历史
**问题1**：用药记录显示空白
**原因1**：Compose UI 渲染问题，文字颜色与背景融合
**解决方案1**：完全重写 MedicationCard 组件
- 使用 `Box` 包裹确保背景色正确
- 添加边框 `.border(2.dp, Color(0xFFE0E0E0))`
- 药品名称使用蓝色背景 + 蓝色文字
- 其他信息使用深色文字，确保高对比度
- 添加 emoji 图标增强可读性

**问题2**：LazyColumn 高度为0
**原因2**：LazyColumn 没有设置 weight
**解决方案2**：添加 `weight(1f)` 使 LazyColumn 占用剩余空间

#### 2.4.5 样例数据
```kotlin
Medication(
    id = "1",
    name = "降压药",
    dosage = "1 片",
    frequency = "每日一次",
    times = listOf("08:00"),
    startDate = "2024-01-01",
    endDate = null,
    notes = "早上饭后服用",
    reminderEnabled = true,
    reminderMinutesBefore = 10,
    effect = "降低血压，预防心血管疾病",
    duration = "24 小时"
)
```

---

### 2.5 设置（Settings）

#### 2.5.1 功能描述
应用设置和关于信息。

#### 2.5.2 界面设计
- **顶部**：灰色标题栏"设置"
- **关于应用卡片**：
  - 应用名称：银发管家
  - 版本号：编译时间（yyyy-MM-dd HH:mm）
  - 应用描述

---

## 三、技术架构

### 3.1 技术栈
- **开发语言**：Kotlin
- **UI框架**：Jetpack Compose
- **最低SDK**：Android 8.0 (API 26)
- **目标SDK**：Android 14 (API 34)

### 3.2 核心依赖
```gradle
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.compose:compose-bom:2024.02.00'
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
}
```

### 3.3 权限声明
```xml
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## 四、UI设计规范

### 4.1 字体规范
| 元素 | 字号 | 字重 |
|------|------|------|
| 标题栏 | 28sp | Bold |
| 大按钮文字 | 24sp | Bold |
| 卡片标题 | 24sp | Bold |
| 正文内容 | 20sp | Normal |
| 辅助文字 | 18sp | Normal |

### 4.2 颜色规范
| 用途 | 颜色值 |
|------|--------|
| 紧急/危险 | #D32F2F（红色） |
| 主要功能 | #2196F3（蓝色） |
| 成功/健康 | #4CAF50（绿色） |
| 警告 | #FF9800（橙色） |
| 背景 | #F5F5F5（浅灰） |
| 卡片背景 | #FFFFFF（白色） |
| 主要文字 | #333333（深灰） |
| 次要文字 | #666666（中灰） |
| 辅助文字 | #999999（浅灰） |

### 4.3 尺寸规范
| 元素 | 尺寸 |
|------|------|
| 大按钮高度 | 120dp |
| 标准按钮高度 | 56dp |
| 卡片圆角 | 12dp |
| 卡片内边距 | 16dp |
| 列表间距 | 12dp |
| 图标大小（大） | 48dp |
| 图标大小（中） | 32dp |
| 图标大小（小） | 24dp |
| 麦克风按钮 | 80dp |

---

## 五、已知问题与解决方案

### 5.1 紧急呼叫问题
**问题**：120拨打只打开拨号盘，不自动拨打
**原因**：
1. Android系统对自动拨打有限制
2. 需要`CALL_PHONE`权限
3. 小米手机有额外安全限制

**解决方案**：
1. 优先使用`ACTION_DIAL`（不需要权限）
2. 同时尝试`ACTION_CALL`（需要权限）
3. 提示用户手动点击拨打按钮
4. 引导用户在系统设置中授予权限

### 5.2 语音识别问题
**问题**：只有方案2有效
**原因**：
1. 方案1、3、7依赖SpeechRecognizer初始化
2. 部分设备SpeechRecognizer不可用
3. 需要网络连接

**解决方案**：
1. 默认使用方案2（RecognizerIntent）
2. 提供多种方案供用户选择
3. 检查设备是否支持语音识别

### 5.3 用药记录空白问题
**问题**：用药记录只显示按钮，不显示文字
**原因**：LazyColumn没有设置weight，高度为0

**解决方案**：
```kotlin
LazyColumn(
    modifier = Modifier.weight(1f) // 占用剩余空间
)
```

### 5.4 天气查询问题
**问题**：使用模拟数据，不是真实天气
**原因**：需要接入第三方天气API，需要API Key

**解决方案**：
1. 接入和风天气API
2. 或使用OpenWeatherMap
3. 需要申请API Key
4. 添加网络权限

---

## 六、待办事项

### 6.1 高优先级
- [ ] 接入真实天气API
- [ ] 优化120拨打逻辑，实现自动拨打
- [ ] 添加数据持久化（Room数据库）
- [ ] 完善用药提醒功能

### 6.2 中优先级
- [ ] 添加跌倒检测功能
- [ ] 添加诈骗电话识别
- [ ] 优化语音交互体验
- [ ] 添加健康数据图表

### 6.3 低优先级
- [ ] 支持多语言
- [ ] 添加主题切换
- [ ] 支持字体大小调节
- [ ] 添加使用教程

---

## 七、附录

### 7.1 文件结构
```
app/
├── src/main/java/com/example/elderlyguardian/
│   ├── MainActivity.kt          # 主界面
│   ├── data/
│   │   └── Models.kt            # 数据模型
│   ├── services/
│   │   ├── MedicationReminderService.kt
│   │   ├── VoiceMonitoringService.kt
│   │   └── FallDetectionService.kt
│   └── receivers/
│       ├── MedicationReminderReceiver.kt
│       └── ScamDetectionReceiver.kt
├── src/main/res/
│   ├── drawable/                # 图标资源
│   ├── mipmap-xxxhdpi/          # 应用图标
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── themes.xml
└── build.gradle
```

### 7.2 版本历史
| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2026-03-08 | 初始版本，基础功能实现 |

### 7.3 联系方式
- 开发者：[您的姓名]
- 邮箱：[您的邮箱]
- 项目地址：[GitHub地址]

---

**文档版本**：v1.0  
**最后更新**：2026-03-08  
**编写者**：AI Assistant
