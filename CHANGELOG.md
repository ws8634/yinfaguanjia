# 银发管家 - 版本变更记录

## 版本号规则
采用语义化版本控制 (Semantic Versioning)：
- **X.Y.Z** 格式
  - **X (主版本号)**: 重大功能发布，不兼容的API修改
  - **Y (次版本号)**: 功能新增，向下兼容
  - **Z (修订号)**: 问题修复，向下兼容

---

## 版本历史

### v1.2.0 (2026-03-15)
**功能完善版本**

#### 新增功能
1. **用药记录持久化存储**
   - 使用SharedPreferences保存用药记录
   - 应用重启后数据不丢失
   - 技术: SharedPreferences + Gson序列化

2. **紧急呼叫功能优化**
   - 新增疾病类型选择（癫痫、心脏病、高血压、自定义）
   - 新增药品位置说明
   - 新增补充说明区域
   - 语音播报循环播放直到手动停止
   - 技术: MediaPlayer循环播放 + 自定义界面布局

3. **闹钟提醒功能完善**
   - 锁屏状态下正常提醒
   - 后台状态下正常提醒
   - 连续播报直到用户操作
   - 点击按钮后返回应用界面
   - 技术: AlarmManager + WakeLock + ForegroundService

#### 修复问题
1. **闹钟界面白屏卡顿**
   - 问题: 初始化TTS和震动阻塞UI线程
   - 解决: 使用异步加载，先显示简单界面
   - 技术: Handler.postDelayed + 异步初始化

2. **播报只播两遍停止**
   - 问题: 使用postDelayed只触发一次
   - 解决: 使用循环播放机制
   - 技术: while循环 + isSpeaking标志位

3. **点击按钮退出应用**
   - 问题: finish()后直接关闭Activity
   - 解决: 启动MainActivity并清除顶部
   - 技术: Intent.FLAG_ACTIVITY_CLEAR_TOP

4. **用药记录保存不持久化**
   - 问题: 只保存在内存中
   - 解决: 使用SharedPreferences持久化
   - 技术: SharedPreferences + Gson

5. **拨打电话不稳定**
   - 问题: 没有SIM卡时处理不完善
   - 解决: 添加SIM卡状态检查
   - 技术: TelephonyManager检查

#### 技术栈
- Kotlin + Jetpack Compose
- AlarmManager + BroadcastReceiver
- TextToSpeech语音播报
- SharedPreferences数据持久化
- Gson JSON序列化

---

### v1.1.0 (2026-03-14)
**功能增强版本**

#### 新增功能
1. **讯飞语音集成**
   - 集成讯飞SparkChain SDK
   - 支持语音唤醒和识别
   - 技术: 讯飞SparkChain SDK + AudioRecord

2. **用药记录编辑优化**
   - 自定义服用频率（每日/每周/自定义）
   - 多时间选择（滑块选择小时和分钟）
   - 闹钟提醒开关
   - 技术: Compose Slider + 自定义Dialog

3. **闹钟界面优化**
   - 大按钮设计
   - 图标优化
   - 技术: Compose Button + Icon

#### 修复问题
1. **闹钟锁屏不弹出**
   - 添加DISABLE_KEYGUARD权限
   - 使用setShowWhenLocked API

2. **按钮文字显示不全**
   - 调整weight比例
   - 优化文字大小

3. **用药记录时间保存逻辑**
   - 根据实际时间数量计算频率
   - 自动更新frequencyValue

---

### v1.0.0 (2026-03-13)
**初始发布版本**

#### 核心功能
1. **语音对话**
   - 语音识别（系统API）
   - 语音播报
   - 简单问答

2. **用药记录**
   - 添加/编辑/删除用药
   - 基本提醒功能
   - 技术: Room数据库

3. **紧急呼叫**
   - 一键呼叫紧急联系人
   - 语音播报求助信息

4. **健康监测**
   - 步数统计
   - 天气查询（和风天气API）

#### 技术栈
- Kotlin + Jetpack Compose
- Room数据库
- Retrofit网络请求
- TextToSpeech

---

## 优化待办清单

### 高优先级
1. [ ] **阿里云千问集成**
   - 集成通义千问大模型API
   - 实现智能对话功能
   - 支持OpenClaw代理配置
   - 技术: HTTPURLConnection + JSON解析

2. [ ] **后台语音防诈骗监听**
   - 后台持续监听语音
   - 关键词识别（彩票、抽奖、投资等）
   - 自动弹出提醒并记录
   - 技术: SpeechRecognizer + 关键词匹配

3. [ ] **用药记录数据同步**
   - 云端备份功能
   - 多设备同步
   - 技术: Firebase或自建服务器

### 中优先级
4. [ ] **UI界面美化**
   - 老年人友好的大字体设计
   - 高对比度配色
   - 简化操作流程

5. [ ] **健康数据分析**
   - 用药依从性统计
   - 健康趋势图表
   - 技术: MPAndroidChart

6. [ ] **语音唤醒功能**
   - "小助手"唤醒词
   - 免手操作
   - 技术: 讯飞语音唤醒

### 低优先级
7. [ ] **多语言支持**
   - 简体中文、繁体中文、英文
   - 技术: Android国际化

8. [ ] **无障碍功能**
   - TalkBack支持
   - 大字体模式
   - 高对比度模式

---

## 关键技术记录

### 闹钟提醒实现
```kotlin
// AlarmManager设置精确闹钟
alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    triggerTime,
    pendingIntent
)

// 锁屏显示
setShowWhenLocked(true)
setTurnScreenOn(true)
```

### 数据持久化
```kotlin
// SharedPreferences保存复杂对象
val json = Gson().toJson(medications)
sharedPreferences.edit().putString("medications", json).apply()
```

### 语音播报
```kotlin
// TextToSpeech初始化
textToSpeech = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) {
        textToSpeech?.language = Locale.CHINA
    }
}
```

---

## 版本回退说明

如需回退到历史版本：
1. 查看git提交记录: `git log --oneline`
2. 回退到指定版本: `git checkout <commit-hash>`
3. 重新编译: `./gradlew clean assembleDebug`

或在Android Studio中：
1. 打开Version Control工具窗口
2. 选择Log标签
3. 右键点击目标提交
4. 选择Checkout Revision
