package com.example.elderlyguardian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.elderlyguardian.data.EmergencyContact
import com.example.elderlyguardian.data.Medication
import com.example.elderlyguardian.data.MedicationRecord
import com.example.elderlyguardian.repository.WeatherRepository
import com.example.elderlyguardian.services.MedicationReminderService
import com.example.elderlyguardian.services.VoiceMonitoringService
import com.example.elderlyguardian.utils.ASRCallback
import com.example.elderlyguardian.utils.IFlytekASRHelper
import com.example.elderlyguardian.utils.MedicationReminderManager
import com.example.elderlyguardian.utils.MedicationStorage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
enum class Screen {
    Main, EmergencyCall, VoiceChat, MedicationRecord, Settings, Help, EmergencySOS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启动语音监测服务（防诈骗）- 核心功能
        startVoiceMonitoringService()
        
        setContent {
            MaterialTheme {
                ElderlyGuardianApp()
            }
        }
    }
    
    private fun startVoiceMonitoringService() {
        val serviceIntent = Intent(this, VoiceMonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
fun ElderlyGuardianApp() {
    var currentScreen by remember { mutableStateOf(Screen.Main) }
    
    when (currentScreen) {
        Screen.Main -> MainScreen(
            onEmergencyCall = { currentScreen = Screen.EmergencyCall },
            onVoiceChat = { currentScreen = Screen.VoiceChat },
            onMedicationRecord = { currentScreen = Screen.MedicationRecord },
            onSettings = { currentScreen = Screen.Settings },
            onEmergencySOS = { currentScreen = Screen.EmergencySOS }
        )
        Screen.EmergencyCall -> EmergencyCallScreen(
            onBack = { currentScreen = Screen.Main }
        )
        Screen.VoiceChat -> VoiceChatScreen(
            onBack = { currentScreen = Screen.Main }
        )
        Screen.MedicationRecord -> MedicationRecordScreen(
            onBack = { currentScreen = Screen.Main }
        )
        Screen.Settings -> SettingsScreen(
            onBack = { currentScreen = Screen.Main }
        )
        Screen.Help -> HelpScreen(
            onBack = { currentScreen = Screen.Main }
        )
        Screen.EmergencySOS -> EmergencySOSScreen(
            onBack = { currentScreen = Screen.Main }
        )
    }
}

@Composable
fun MainScreen(
    onEmergencyCall: () -> Unit,
    onVoiceChat: () -> Unit,
    onMedicationRecord: () -> Unit,
    onSettings: () -> Unit,
    onEmergencySOS: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Text(
            text = "银发管家",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = "您的智能健康助手",
            fontSize = 20.sp,
            color = Color(0xFF666666)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MainMenuButton(
            icon = Icons.Default.Emergency,
            text = "紧急呼叫",
            subText = "一键拨打 120 或联系家人",
            color = Color(0xFFD32F2F),
            onClick = onEmergencyCall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MainMenuButton(
            icon = Icons.Default.Mic,
            text = "语音聊天",
            subText = "陪我聊聊天，问问天气吧",
            color = Color(0xFF2196F3),
            onClick = onVoiceChat
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MainMenuButton(
            icon = Icons.Default.Medication,
            text = "用药记录",
            subText = "记录和管理您的用药",
            color = Color(0xFF4CAF50),
            onClick = onMedicationRecord
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MainMenuButton(
            icon = Icons.Default.Warning,
            text = "紧急呼救",
            subText = "摔倒或突发疾病时求救",
            color = Color(0xFFFF5722),
            onClick = onEmergencySOS
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MainMenuButton(
            icon = Icons.Default.Settings,
            text = "设置",
            subText = "个性化应用设置",
            color = Color(0xFF9E9E9E),
            onClick = onSettings
        )
    }
}

@Composable
fun MainMenuButton(
    icon: ImageVector,
    text: String,
    subText: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subText,
                    fontSize = 18.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun EmergencyCallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var showCallPermissionDialog by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }
    
    val emergencyContacts = remember {
        mutableStateListOf(
            EmergencyContact("0", "120 急救", "120", "急救"),
            EmergencyContact("1", "儿子", "13800138001", "子女"),
            EmergencyContact("2", "女儿", "13800138002", "子女"),
            EmergencyContact("3", "社区医院", "01012345678", "医疗")
        )
    }
    
    // 方式 1: ACTION_CALL + CALL_PHONE 权限（需要权限，直接拨打）
    fun makeCall1(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "方式 1 失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // 方式 2: ACTION_CALL + 延迟执行（需要权限）
    fun makeCall2(phoneNumber: String) {
        textToSpeech?.speak("正在拨打$phoneNumber", TextToSpeech.QUEUE_FLUSH, null, null)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "方式 2 失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 1000)
    }
    
    // 方式 3: ACTION_DIAL（不需要权限，但只打开拨号盘）
    fun makeCall3(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "方式 3 失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // 方式 4: ACTION_DIAL + 延迟
    fun makeCall4(phoneNumber: String) {
        textToSpeech?.speak("正在拨打$phoneNumber", TextToSpeech.QUEUE_FLUSH, null, null)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "方式 4 失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 1500)
    }
    
    // 方式 5: 使用 CALL_PRIVILEGED（系统权限，通常不可用）
    fun makeCall5(phoneNumber: String) {
        try {
            val intent = Intent("android.intent.action.CALL_PRIVILEGED").apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "方式 5 失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // 方式 6: 使用 TelephonyManager（需要权限）
    fun makeCall6(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "方式 6 失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // 方式 7: 直接启动 + 语音提示
    fun makeCall7(phoneNumber: String) {
        textToSpeech?.speak("正在拨打$phoneNumber，请稍后", TextToSpeech.QUEUE_FLUSH, null, null)
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: SecurityException) {
                // 如果没有权限，尝试 ACTION_DIAL
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "方式 7 失败：${e2.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "方式 7 失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 800)
    }
    
    // 默认使用方式 7（最可靠）
    val currentCallMethod: (String) -> Unit = { makeCall7(it) }
    
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
    }
    
    // 辅助函数：紧急号码拨打 - 使用引导式策略
    fun dialEmergencyNumber(phoneNumber: String) {
        try {
            // 使用 ACTION_DIAL 打开拨号盘，预填号码
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            
            // 显示引导提示
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(context, "请在拨号盘上点击绿色拨打按钮", Toast.LENGTH_LONG).show()
            }, 1000)
            
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开拨号盘，请手动拨打 $phoneNumber", Toast.LENGTH_LONG).show()
        }
    }
    
    // 辅助函数：普通号码拨打 - 尝试直接拨打
    fun tryDirectDial(phoneNumber: String) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            try {
                // 有权限，尝试直接拨打
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
            } catch (e: SecurityException) {
                // 即使申请了权限，系统可能仍限制，回退到拨号盘
                dialEmergencyNumber(phoneNumber)
            } catch (e: Exception) {
                dialEmergencyNumber(phoneNumber)
            }
        } else {
            // 无权限，使用拨号盘
            dialEmergencyNumber(phoneNumber)
        }
    }
    
    // 拨打电话的核心方法 - 采用务实的策略
    fun makePhoneCallInternal(phoneNumber: String, isEmergency: Boolean = false) {
        try {
            textToSpeech?.speak("正在为您拨打$phoneNumber", TextToSpeech.QUEUE_FLUSH, null, null)
            
            Handler(Looper.getMainLooper()).postDelayed({
                // 策略：根据号码类型选择拨打方式
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
            }, 800)
        } catch (e: Exception) {
            Toast.makeText(context, "拨打电话失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    fun makePhoneCall(phoneNumber: String) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            makePhoneCallInternal(phoneNumber)
        } else {
            pendingCallNumber = phoneNumber
            showCallPermissionDialog = true
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (pendingCallNumber.isNotEmpty()) {
                    makePhoneCallInternal(pendingCallNumber)
                    pendingCallNumber = ""
                }
            }, 500)
        } else {
            Toast.makeText(context, "需要电话权限才能拨打紧急电话", Toast.LENGTH_LONG).show()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = { Text("紧急呼叫", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(32.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFD32F2F),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 120 急救按钮 - 使用与联系人相同的拨打方式
            Button(
                onClick = { makePhoneCall("120") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("拨打 120 急救", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "紧急联系人",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    editingContact = null
                    showAddContactDialog = true
                }) {
                    Icon(Icons.Default.Add, "添加联系人", tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(emergencyContacts) { contact ->
                    EmergencyContactCard(
                        contact = contact,
                        onCall = { makePhoneCall(contact.phone) },
                        onEdit = {
                            editingContact = contact
                            showAddContactDialog = true
                        },
                        onDelete = {
                            showDeleteConfirmDialog = true
                            contactToDelete = contact
                        }
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirmDialog && contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false
                contactToDelete = null
            },
            title = { Text("确认删除", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除联系人 \"${contactToDelete?.name}\" 吗？", fontSize = 22.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactToDelete != null) {
                            emergencyContacts.removeIf { it.id == contactToDelete?.id }
                        }
                        showDeleteConfirmDialog = false
                        contactToDelete = null
                    }
                ) {
                    Text("删除", fontSize = 24.sp, color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                Button(onClick = { 
                    showDeleteConfirmDialog = false
                    contactToDelete = null
                }) {
                    Text("取消", fontSize = 24.sp)
                }
            }
        )
    }
    
    if (showAddContactDialog) {
        AddEditContactDialog(
            contact = editingContact,
            onDismiss = {
                showAddContactDialog = false
                editingContact = null
            },
            onSave = { newContact ->
                if (editingContact == null) {
                    emergencyContacts.add(newContact)
                } else {
                    val index = emergencyContacts.indexOfFirst { it.id == editingContact?.id }
                    if (index != -1) {
                        emergencyContacts[index] = newContact
                    }
                }
                showAddContactDialog = false
                editingContact = null
            }
        )
    }
    
    if (showCallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCallPermissionDialog = false },
            title = { Text("需要电话权限", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("拨打紧急电话需要电话权限。", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("请点击\"授予权限\"以继续。", fontSize = 22.sp, color = Color(0xFF666666))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCallPermissionDialog = false
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                ) {
                    Text("授予权限", fontSize = 24.sp)
                }
            },
            dismissButton = {
                Button(onClick = { showCallPermissionDialog = false }) {
                    Text("取消", fontSize = 24.sp)
                }
            }
        )
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = contact.relationship,
                        fontSize = 18.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "电话：${contact.phone}",
                        fontSize = 20.sp,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Medium
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "编辑", tint = Color(0xFF2196F3))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "删除", tint = Color(0xFFF44336))
                    }
                    Button(
                        onClick = onCall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("拨打", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    
    val relationshipOptions = listOf("子女", "配偶", "父母", "兄弟姐妹", "朋友", "邻居", "其他")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (contact == null) "添加联系人" else "编辑联系人",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("电话号码", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                    placeholder = { Text("例如：13800138000", fontSize = 18.sp) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("关系:", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                relationshipOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { relationship = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = relationship == option,
                            onClick = { relationship = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option, fontSize = 20.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank() && relationship.isNotBlank()) {
                        val newContact = EmergencyContact(
                            id = contact?.id ?: System.currentTimeMillis().toString(),
                            name = name,
                            phone = phone,
                            relationship = relationship
                        )
                        onSave(newContact)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && relationship.isNotBlank()
            ) {
                Text("保存", fontSize = 24.sp)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消", fontSize = 24.sp)
            }
        }
    )
}

data class ChatMessage(val isUser: Boolean, val message: String)

@Composable
fun VoiceChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("点击麦克风按钮开始对话") }
    var hasRecordPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    
    // 天气API默认开启（和风天气）
    var useRealWeatherApi by remember { mutableStateOf(true) }
    var useRealVoiceApi by remember { mutableStateOf(true) }  // 默认使用讯飞语音
    
    // 讯飞ASR相关
    var iFlytekInitialized by remember { mutableStateOf(false) }
    var iFlytekStatus by remember { mutableStateOf("未初始化") }
    val iFlytekASR = remember { IFlytekASRHelper.getInstance() }
    
    // 调试日志（显示在界面上）- 增加到20条
    var debugLogs by remember { mutableStateOf(listOf<String>()) }
    fun addDebugLog(log: String) {
        debugLogs = (listOf("${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())} $log") + debugLogs).take(20)
    }
    
    val chatHistory = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val weatherRepository = remember { WeatherRepository() }
    
    // 初始化讯飞ASR
    LaunchedEffect(Unit) {
        addDebugLog("开始初始化讯飞ASR...")
        val initResult = iFlytekASR.initialize(context)
        iFlytekInitialized = initResult
        iFlytekStatus = if (initResult) "讯飞ASR已就绪" else "讯飞ASR初始化失败"
        addDebugLog("初始化结果: $initResult")
    }
    
    // 处理识别结果
    fun handleRecognitionResult(userText: String) {
        recognizedText = userText
        chatHistory.add(ChatMessage(true, userText))
        statusText = "思考中..."
        
        // 检查是否是天气查询，如果是则调用天气API
        if (userText.contains("天气")) {
            scope.launch {
                val weatherResult = weatherRepository.fetchRealWeatherFromApi("北京")
                val response = weatherRepository.formatWeatherResult(weatherResult)
                chatHistory.add(ChatMessage(false, response))
                statusText = ""
                textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } else {
            val response = generateAIResponse(userText)
            chatHistory.add(ChatMessage(false, response))
            statusText = ""
            textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    val quickQuestions = listOf(
        "今天天气怎么样？",
        "适合出门散步吗？",
        "我想去公园运动",
        "我有点不舒服",
        "今天吃什么好？",
        "提醒我吃降压药",
        "帮我联系家人",
        "附近的医院在哪里？",
        "现在几点了？"
    )
    
    // 方案 2: 使用 RecognizerIntent（最简单可靠）- 使用 ActivityResultLauncher 获取结果
    fun startListening2() {
        if (!hasRecordPermission) {
            statusText = "请先授予麦克风权限"
            showPermissionDialog = true
            return
        }
        
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                context.startActivity(intent)
                statusText = "等待语音识别..."
            } catch (e: Exception) {
                statusText = "启动失败：${e.message}"
            }
        } catch (e: Exception) {
            statusText = "启动失败：${e.message}"
        }
    }
    
    // 注意：已删除方案3-7，只保留方案2（RecognizerIntent），最稳定可靠
    
    // 启动讯飞ASR语音识别
    fun startIFlytekASR() {
        addDebugLog("点击麦克风，开始识别")
        
        if (!hasRecordPermission) {
            addDebugLog("错误：没有麦克风权限")
            statusText = "请先授予麦克风权限"
            showPermissionDialog = true
            return
        }
        
        addDebugLog("检查状态: isReady=${iFlytekASR.isReady()}, initialized=$iFlytekInitialized")
        
        if (!iFlytekASR.isReady()) {
            addDebugLog("讯飞ASR未就绪，切换到系统识别")
            statusText = "讯飞ASR未就绪，使用系统识别"
            startListening2()
            return
        }
        
        isListening = true
        statusText = "请说话..."
        addDebugLog("开始讯飞ASR识别...")
        
        iFlytekASR.startListening(object : ASRCallback {
            override fun onStart() {
                addDebugLog("ASR已启动，请说话")
                statusText = "正在聆听..."
            }
            
            override fun onResult(result: String) {
                addDebugLog("识别结果: [$result]")
                isListening = false
                statusText = "识别完成"
                if (result.isNotEmpty()) {
                    handleRecognitionResult(result)
                } else {
                    statusText = "识别结果为空"
                }
            }
            
            override fun onError(code: Int, message: String) {
                addDebugLog("识别错误: code=$code, msg=$message")
                isListening = false
                
                // 网络错误处理
                if (code == 18804 || message.contains("network") || message.contains("connection")) {
                    statusText = "网络连接失败，切换到系统识别"
                    addDebugLog("网络错误，使用系统识别")
                } else {
                    statusText = "识别错误($code)：$message"
                }
                
                // 讯飞失败，回退到系统识别
                startListening2()
            }
            
            override fun onBeginOfSpeech() {
                addDebugLog("检测到语音开始")
                statusText = "开始说话..."
            }
            
            override fun onEndOfSpeech() {
                addDebugLog("检测到语音结束，识别中...")
                statusText = "识别中..."
            }
            
            override fun onVolumeChanged(volume: Double) {
                // 可以在这里更新音量UI
            }
            
            override fun onDebug(message: String) {
                addDebugLog(message)
            }
        })
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (!isGranted) {
            showPermissionDialog = true
        }
    }
    
    LaunchedEffect(Unit) {
        hasRecordPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasRecordPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
    }
    
    fun stopListening() {
        if (useRealVoiceApi && iFlytekASR.isReady()) {
            iFlytekASR.stopListening()
        } else {
            speechRecognizer?.stopListening()
        }
        isListening = false
    }
    
    fun sendQuickQuestion(question: String) {
        chatHistory.add(ChatMessage(true, question))
        statusText = "思考中..."
        
        // 检查是否是天气相关问题
        if (question.contains("天气") && useRealWeatherApi) {
            // 使用真实API获取天气
            scope.launch {
                val weatherResult = weatherRepository.fetchRealWeatherFromApi("北京")
                val response = weatherRepository.formatWeatherResult(weatherResult) +
                        if (weatherResult is com.example.elderlyguardian.data.WeatherResult.Error) {
                            "\n【已切换到实时API模式】"
                        } else {
                            "\n【实时天气数据】"
                        }
                
                chatHistory.add(ChatMessage(false, response))
                statusText = ""
                textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        } else {
            // 检查是否使用讯飞大模型
            // 使用模拟数据回复
            val response = generateAIResponse(question, useRealWeatherApi)
            chatHistory.add(ChatMessage(false, response))
            statusText = ""
            textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.size > 0) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
            textToSpeech?.shutdown()
            iFlytekASR.release()
        }
    }
    
    var showAllQuestions by remember { mutableStateOf(false) }
    
    // 根据选择的语音模式调用对应的识别方法
    fun getCurrentVoiceMethod() {
        if (useRealVoiceApi && iFlytekInitialized) {
            // 使用讯飞ASR语音识别
            startIFlytekASR()
        } else {
            // 使用系统语音识别
            startListening2()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = { Text("语音聊天", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(32.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2196F3),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory.size) { index ->
                val message = chatHistory[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.isUser) Color(0xFF2196F3) else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Text(
                            text = message.message,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 20.sp,
                            color = if (message.isUser) Color.White else Color(0xFF333333)
                        )
                    }
                }
            }
        }
        
        // 快捷问题区域 - 使用 Row 布局确保均匀排列
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                quickQuestions.take(3).forEach { question ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = { sendQuickQuestion(question) },
                                onLongClick = { showAllQuestions = true }
                            ),
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = question,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = Color(0xFF1976D2),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
            Text(
                text = "长按显示更多问题",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    if (isListening) {
                        stopListening()
                    } else {
                        getCurrentVoiceMethod()
                    }
                },
                modifier = Modifier.size(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
        
        Text(
            text = statusText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = Color(0xFF666666)
        )
    }
    
    // 显示所有问题的对话框
    if (showAllQuestions) {
        AlertDialog(
            onDismissRequest = { showAllQuestions = false },
            title = { Text("快捷问题", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickQuestions) { question ->
                        Button(
                            onClick = {
                                sendQuickQuestion(question)
                                showAllQuestions = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(question, fontSize = 18.sp, color = Color(0xFF1976D2))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAllQuestions = false }) {
                    Text("关闭", fontSize = 24.sp)
                }
            }
        )
    }
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要麦克风权限", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            text = { Text("语音聊天需要麦克风权限。请点击\"授予权限\"以继续。", fontSize = 22.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        hasRecordPermission = true
                        // 权限已授予，不需要额外初始化
                    }
                ) {
                    Text("授予权限", fontSize = 24.sp)
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text("取消", fontSize = 24.sp)
                }
            }
        )
    }
}

// 天气数据类
data class WeatherData(
    val city: String = "北京",
    val temperature: Int = 25,
    val weather: String = "晴朗",
    val humidity: Int = 45,
    val wind: String = "微风",
    val airQuality: String = "良",
    val advice: String = "适合出门散步或运动"
)

// 模拟获取天气数据（实际应用中应该调用真实天气 API）
fun fetchWeatherData(): WeatherData {
    // 这里应该调用真实的天气 API，如和风天气、OpenWeatherMap 等
    // 由于需要 API key 和网络权限，这里使用模拟数据
    // 实际实现示例：
    // val response = URL("https://api.weather.com/v1/current?city=北京&appkey=YOUR_KEY").readText()
    // return parseWeatherJson(response)
    
    return WeatherData(
        city = "北京",
        temperature = (18..28).random(),
        weather = listOf("晴朗", "多云", "阴天", "小雨").random(),
        humidity = (30..70).random(),
        wind = listOf("微风", "和风", "清风").random(),
        airQuality = listOf("优", "良", "轻度污染").random(),
        advice = "适合出门散步或运动，记得多喝水"
    )
}

fun generateAIResponse(input: String, useRealApi: Boolean = false): String {
    val lowerInput = input.lowercase()
    
    return when {
        lowerInput.contains("天气") -> {
            if (useRealApi) {
                "正在获取实时天气数据，请稍候...\n【已切换到实时API模式】"
            } else {
                val weather = fetchWeatherData()
                "今天${weather.city}${weather.weather}，气温${weather.temperature}度，湿度${weather.humidity}%，${weather.wind}，空气质量${weather.airQuality}。${weather.advice}。\n【模拟数据模式】"
            }
        }
        lowerInput.contains("运动") || lowerInput.contains("锻炼") -> {
            "适度运动对身体很好。建议您选择散步、太极拳或广场舞等温和的运动方式。每天坚持 30 分钟，有助于保持身体健康。"
        }
        lowerInput.contains("吃药") || lowerInput.contains("药") -> {
            "请按时服药，遵医嘱用药。如果您有用药记录，我可以帮您查看今天的用药安排。"
        }
        lowerInput.contains("不舒服") || lowerInput.contains("难受") -> {
            "身体不舒服要及时就医，不要拖延。如果症状严重，我可以帮您拨打 120 或联系家人。"
        }
        lowerInput.contains("吃") || lowerInput.contains("饭") -> {
            "饮食要清淡、均衡。多吃蔬菜水果，少吃油腻和辛辣食物。三餐定时定量，不要暴饮暴食。"
        }
        lowerInput.contains("你好") || lowerInput.contains("您好") -> {
            "您好！我是您的智能健康助手。有什么我可以帮您的吗？您可以问我天气、健康建议或者用药提醒。"
        }
        lowerInput.contains("谢谢") -> {
            "不客气！这是我应该做的。如果您有任何问题，随时可以问我。祝您身体健康！"
        }
        lowerInput.contains("再见") -> {
            "再见！祝您生活愉快，身体健康！有需要随时找我。"
        }
        else -> {
            "我听到了您说的话。作为健康助手，我可以帮您查询天气、提供健康建议、提醒用药等。您还有什么其他问题吗？"
        }
    }
}

@Composable
fun MedicationRecordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    
    // 使用持久化存储
    val medicationStorage = remember { MedicationStorage(context) }
    val medications = remember { mutableStateListOf<Medication>() }
    
    // 加载保存的用药记录
    LaunchedEffect(Unit) {
        val savedMedications = medicationStorage.loadMedications()
        if (savedMedications.isNotEmpty()) {
            medications.clear()
            medications.addAll(savedMedications)
        } else {
            // 首次使用，添加默认数据
            medications.addAll(
                listOf(
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
                    ),
                    Medication(
                        id = "2",
                        name = "降糖药",
                        dosage = "2 片",
                        frequency = "每日三次",
                        times = listOf("08:00", "12:00", "18:00"),
                        startDate = "2024-01-01",
                        endDate = null,
                        notes = "饭前服用",
                        reminderEnabled = true,
                        reminderMinutesBefore = 10,
                        effect = "控制血糖，预防糖尿病并发症",
                        duration = "8 小时"
                    ),
                    Medication(
                        id = "3",
                        name = "钙片",
                        dosage = "1 片",
                        frequency = "每日一次",
                        times = listOf("12:00"),
                        startDate = "2024-01-01",
                        endDate = null,
                        notes = "随餐服用",
                        reminderEnabled = true,
                        reminderMinutesBefore = 10,
                        effect = "补充钙质，预防骨质疏松",
                        duration = "24 小时"
                    )
                )
            )
            medicationStorage.saveMedications(medications)
        }
    }
    
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
    }
    
    fun speakMedicationInfo(medication: Medication) {
        val effectText = if (!medication.effect.isNullOrBlank()) "，作用是${medication.effect}" else ""
        val durationText = if (!medication.duration.isNullOrBlank()) "，药效持续${medication.duration}" else ""
        val notesText = if (!medication.notes.isNullOrBlank()) "，${medication.notes}" else ""
        val message = "该吃${medication.name}了，剂量是${medication.dosage}，服用频率是${medication.frequency}${effectText}${durationText}${notesText}"
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    fun speakTakenConfirmation(medicationName: String) {
        val message = "已记录服用$medicationName，记得按时服药，保持健康！"
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = { Text("用药记录", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(32.dp))
                }
            },
            actions = {
                IconButton(onClick = { showAddMedicationDialog = true }) {
                    Icon(Icons.Default.Add, "添加", modifier = Modifier.size(32.dp), tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF4CAF50),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(medications) { medication ->
                MedicationCard(
                    medication = medication,
                    onSpeak = { speakMedicationInfo(medication) },
                    onTaken = { timeIndex, isTaken ->
                        // 更新服用状态
                        val updatedMedication = medication.copy(
                            todayTaken = medication.todayTaken.toMutableMap().apply {
                                put(timeIndex, isTaken)
                            }
                        )
                        val index = medications.indexOfFirst { it.id == medication.id }
                        if (index != -1) {
                            medications[index] = updatedMedication
                        }
                        if (isTaken) {
                            speakTakenConfirmation(medication.name)
                        }
                    },
                    onEdit = {
                        editingMedication = medication
                        showAddMedicationDialog = true
                    },
                    onDelete = {
                        medications.removeIf { it.id == medication.id }
                        // 保存到持久化存储
                        medicationStorage.saveMedications(medications)
                    }
                )
            }
        }
    }
    
    if (showAddMedicationDialog) {
        AddEditMedicationDialog(
            medication = editingMedication,
            onDismiss = {
                showAddMedicationDialog = false
                editingMedication = null
            },
            onSave = { newMedication ->
                // 先取消旧的提醒
                if (editingMedication != null) {
                    MedicationReminderManager(context).cancelMedicationReminder(editingMedication!!.id)
                }

                // 保存用药记录
                if (editingMedication == null) {
                    medications.add(newMedication)
                } else {
                    val index = medications.indexOfFirst { it.id == editingMedication?.id }
                    if (index != -1) {
                        medications[index] = newMedication
                    }
                }

                // 保存到持久化存储
                medicationStorage.saveMedications(medications)

                // 如果开启了提醒，设置闹钟
                if (newMedication.reminderEnabled) {
                    val reminderManager = MedicationReminderManager(context)

                    // 检查闹钟权限（Android 12+）
                    if (!reminderManager.canScheduleExactAlarms()) {
                        // 没有权限，引导用户去设置
                        reminderManager.openAlarmSettings()
                        Toast.makeText(context, "请允许应用设置闹钟提醒", Toast.LENGTH_LONG).show()
                    } else {
                        newMedication.times.forEachIndexed { index, timeStr ->
                            val timeParts = timeStr.split(":")
                            if (timeParts.size == 2) {
                                val calendar = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                                    set(Calendar.MINUTE, timeParts[1].toInt())
                                    set(Calendar.SECOND, 0)
                                    // 如果设置的时间已过，设置为明天
                                    if (timeInMillis <= System.currentTimeMillis()) {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                }

                                // 根据频率类型设置重复天数
                                val repeatDays = when (newMedication.frequencyType) {
                                    "daily" -> listOf(1, 2, 3, 4, 5, 6, 7) // 每天
                                    "weekly", "custom" -> newMedication.frequencyDays // 指定天数
                                    else -> listOf(1, 2, 3, 4, 5, 6, 7)
                                }

                                val reminderMedication = MedicationReminderManager.Medication(
                                    id = "${newMedication.id}_$index",
                                    name = newMedication.name,
                                    dosage = newMedication.dosage,
                                    time = calendar,
                                    repeatDays = repeatDays
                                )
                                reminderManager.setMedicationReminder(reminderMedication)
                            }
                        }
                        Toast.makeText(context, "已设置用药提醒", Toast.LENGTH_SHORT).show()
                    }
                }

                showAddMedicationDialog = false
                editingMedication = null
            }
        )
    }
}

// 用药记录卡片 - 带服用状态勾选框
@Composable
fun MedicationCard(
    medication: Medication,
    onSpeak: () -> Unit,
    onTaken: (Int, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 使用 Box 包裹，确保背景色正确
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部：药品名称和删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 药品名称
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = medication.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }

                // 删除按钮（小图标）
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 作用
            Text(
                text = "💊 作用：${medication.effect ?: "暂无说明"}",
                fontSize = 18.sp,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 剂量和频率
            Text(
                text = "📋 剂量：${medication.dosage}",
                fontSize = 18.sp,
                color = Color(0xFF424242)
            )
            Text(
                text = "🔄 频率：${medication.frequency}",
                fontSize = 18.sp,
                color = Color(0xFF424242)
            )

            // 闹钟提醒状态
            if (medication.reminderEnabled) {
                Text(
                    text = "⏰ 提醒：已开启",
                    fontSize = 16.sp,
                    color = Color(0xFF4CAF50)
                )
            }

            // 可选信息
            if (!medication.duration.isNullOrBlank()) {
                Text(
                    text = "📏 药效持续：${medication.duration}",
                    fontSize = 16.sp,
                    color = Color(0xFF616161)
                )
            }
            if (!medication.notes.isNullOrBlank()) {
                Text(
                    text = "📝 备注：${medication.notes}",
                    fontSize = 16.sp,
                    color = Color(0xFF616161)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 服用时间和勾选框
            Text(
                text = "今日服用状态：",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(8.dp))

            medication.times.forEachIndexed { index, time ->
                val isTaken = medication.todayTaken[index] ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (isTaken) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 时间
                    Text(
                        text = "🕐 $time",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isTaken) Color(0xFF2E7D32) else Color(0xFF424242)
                    )

                    // 大勾选框
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isTaken) Color(0xFF4CAF50) else Color.White,
                                RoundedCornerShape(8.dp)
                            )
                            .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .clickable { onTaken(index, !isTaken) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isTaken) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "已服用",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 播报按钮
            Button(
                onClick = onSpeak,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("播报用药信息", fontSize = 18.sp)
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除 ${medication.name} 的用药记录吗？", fontSize = 18.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("删除", fontSize = 18.sp)
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false }) {
                    Text("取消", fontSize = 18.sp)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationDialog(
    medication: Medication?,
    onDismiss: () -> Unit,
    onSave: (Medication) -> Unit
) {
    var name by remember { mutableStateOf(medication?.name ?: "") }
    var dosage by remember { mutableStateOf(medication?.dosage ?: "") }
    var frequencyType by remember { mutableStateOf(medication?.frequencyType ?: "daily") } // daily, weekly, custom
    // frequencyValue 应该从 times.size 计算，而不是从保存的值读取
    var times by remember { mutableStateOf(medication?.times ?: listOf("08:00")) }
    var frequencyValue by remember { mutableStateOf(times.size) } // 根据实际时间数量
    var frequencyDays by remember { mutableStateOf(medication?.frequencyDays ?: listOf(1, 2, 3, 4, 5, 6, 7)) } // 每周哪几天
    var notes by remember { mutableStateOf(medication?.notes ?: "") }
    var effect by remember { mutableStateOf(medication?.effect ?: "") }
    var duration by remember { mutableStateOf(medication?.duration ?: "") }
    var reminderEnabled by remember { mutableStateOf(medication?.reminderEnabled ?: true) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableStateOf(-1) }

    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (medication == null) "添加用药" else "编辑用药",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // 药品名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("药品名称", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 剂量
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("剂量", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                    placeholder = { Text("例如：1 片", fontSize = 18.sp) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 服用频率
                Text("服用频率:", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // 频率类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("daily" to "每日", "weekly" to "每周", "custom" to "自定义").forEach { (type, label) ->
                        Button(
                            onClick = { frequencyType = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (frequencyType == type) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                contentColor = if (frequencyType == type) Color.White else Color(0xFF333333)
                            ),
                            modifier = Modifier.weight(1f).padding(4.dp)
                        ) {
                            Text(label, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 每日/每周几次
                if (frequencyType != "custom") {
                    Text("${if (frequencyType == "daily") "每日" else "每周"}服用次数:", fontSize = 18.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..4).forEach { count ->
                            Button(
                                onClick = {
                                    frequencyValue = count
                                    // 调整时间数量
                                    times = if (times.size > count) times.take(count)
                                    else times + List(count - times.size) { "08:00" }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (frequencyValue == count) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                    contentColor = if (frequencyValue == count) Color.White else Color(0xFF333333)
                                ),
                                modifier = Modifier.weight(1f).padding(4.dp)
                            ) {
                                Text("$count 次", fontSize = 16.sp)
                            }
                        }
                    }
                }

                // 每周自定义选择星期
                if (frequencyType == "weekly" || frequencyType == "custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("选择星期:", fontSize = 18.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        weekDays.forEachIndexed { index, day ->
                            val dayNum = index + 1
                            val isSelected = frequencyDays.contains(dayNum)
                            Button(
                                onClick = {
                                    frequencyDays = if (isSelected) {
                                        if (frequencyDays.size > 1) frequencyDays - dayNum else frequencyDays
                                    } else {
                                        frequencyDays + dayNum
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                    contentColor = if (isSelected) Color.White else Color(0xFF333333)
                                ),
                                modifier = Modifier.weight(1f).padding(2.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(day, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 服用时间
                Text("服用时间:", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // 显示所有时间
                times.forEachIndexed { index, time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                editingTimeIndex = index
                                showTimePickerDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("第${index + 1}次: $time", fontSize = 18.sp)
                        }
                        if (times.size > 1) {
                            IconButton(
                                onClick = {
                                    times = times.toMutableList().apply { removeAt(index) }
                                    frequencyValue = times.size
                                }
                            ) {
                                Icon(Icons.Default.Delete, "删除", tint = Color.Red)
                            }
                        }
                    }
                }

                // 添加时间按钮
                if (times.size < 6) {
                    Button(
                        onClick = {
                            times = times + "08:00"
                            frequencyValue = times.size
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("+ 添加服用时间", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 闹钟提醒开关
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { reminderEnabled = !reminderEnabled }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("到点提醒（闹钟）", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 药效/作用
                OutlinedTextField(
                    value = effect,
                    onValueChange = { effect = it },
                    label = { Text("药效/作用（可选）", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                    placeholder = { Text("例如：降低血压，预防心血管疾病", fontSize = 18.sp) },
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 药效持续时间
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("药效持续时间（可选）", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                    placeholder = { Text("例如：24 小时", fontSize = 18.sp) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 备注
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）", fontSize = 20.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                    placeholder = { Text("例如：饭后服用", fontSize = 18.sp) },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank()) {
                        // 根据实际时间数量更新频率值
                        val actualFrequencyValue = times.size
                        val frequencyStr = when (frequencyType) {
                            "daily" -> "每日${actualFrequencyValue}次"
                            "weekly" -> "每周${frequencyDays.size}天，每天${actualFrequencyValue}次"
                            "custom" -> "自定义：${frequencyDays.map { weekDays[it - 1] }.joinToString(",")}，每天${actualFrequencyValue}次"
                            else -> "每日${actualFrequencyValue}次"
                        }
                        val newMedication = Medication(
                            id = medication?.id ?: System.currentTimeMillis().toString(),
                            name = name,
                            dosage = dosage,
                            frequency = frequencyStr,
                            frequencyType = frequencyType,
                            frequencyValue = actualFrequencyValue,
                            frequencyDays = frequencyDays,
                            times = times,
                            startDate = medication?.startDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date()),
                            endDate = medication?.endDate,
                            notes = notes.takeIf { it.isNotBlank() },
                            reminderEnabled = reminderEnabled,
                            reminderMinutesBefore = medication?.reminderMinutesBefore ?: 10,
                            effect = effect.takeIf { it.isNotBlank() },
                            duration = duration.takeIf { it.isNotBlank() }
                        )
                        onSave(newMedication)
                    }
                },
                enabled = name.isNotBlank() && dosage.isNotBlank()
            ) {
                Text("保存", fontSize = 24.sp)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消", fontSize = 24.sp)
            }
        }
    )

    // 时间选择器对话框
    if (showTimePickerDialog) {
        var selectedHour by remember { mutableStateOf(times.getOrElse(editingTimeIndex) { "08:00" }.split(":")[0].toInt()) }
        var selectedMinute by remember { mutableStateOf(times.getOrElse(editingTimeIndex) { "08:00" }.split(":")[1].toInt()) }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("选择时间", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("选择服用时间", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 小时选择
                    Text("小时: $selectedHour", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 23,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 分钟选择
                    Text("分钟: $selectedMinute", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = it.toInt() },
                        valueRange = 0f..59f,
                        steps = 59,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "选定时间: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        times = times.toMutableList().apply {
                            if (editingTimeIndex >= 0 && editingTimeIndex < size) {
                                set(editingTimeIndex, newTime)
                            }
                        }
                        showTimePickerDialog = false
                    }
                ) {
                    Text("确定", fontSize = 20.sp)
                }
            },
            dismissButton = {
                Button(onClick = { showTimePickerDialog = false }) {
                    Text("取消", fontSize = 20.sp)
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val version = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        sdf.format(Date())
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = { Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(32.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF9E9E9E),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("应用设置", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("字体大小：大（已优化）", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("语音播报：已启用", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("防诈骗监测：运行中", fontSize = 20.sp, color = Color(0xFF4CAF50))
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("关于应用", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("应用名称：银发管家", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("版本号：$version", fontSize = 20.sp, color = Color(0xFF2196F3))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("专为老年人设计的智能健康管理应用", fontSize = 18.sp, color = Color(0xFF666666))
                    }
                }
            }
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = { Text("帮助", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(32.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2196F3),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HelpCard(
                    title = "紧急呼叫",
                    content = "点击'紧急呼叫'按钮，可以直接拨打 120 急救电话或联系预设的紧急联系人。建议提前设置好子女、社区医院等联系方式。"
                )
            }
            item {
                HelpCard(
                    title = "语音聊天",
                    content = "点击'语音聊天'按钮，按住麦克风图标说话，松开后系统会识别您的语音并给出智能回复。您可以询问天气、健康建议、饮食推荐等问题。"
                )
            }
            item {
                HelpCard(
                    title = "用药记录",
                    content = "在'用药记录'中，您可以添加需要服用的药物，设置用药时间和剂量。系统会提醒您按时服药，并记录您的用药情况。"
                )
            }
            item {
                HelpCard(
                    title = "紧急呼救",
                    content = "当您摔倒或突发疾病时，可以使用'紧急呼救'功能。系统会向周围发出警报，并提供您的急救信息。"
                )
            }
            item {
                HelpCard(
                    title = "防诈骗监测",
                    content = "应用会在后台运行语音监测，当检测到'转账'、'汇款'、'验证码'等诈骗关键词时，会立即发出警告提醒您。"
                )
            }
            item {
                HelpCard(
                    title = "权限说明",
                    content = "本应用需要以下权限：\n1. 麦克风权限 - 用于语音识别和防诈骗监测\n2. 电话权限 - 用于紧急呼叫功能"
                )
            }
        }
    }
}

@Composable
fun HelpCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 18.sp,
                color = Color(0xFF666666),
                lineHeight = 28.sp
            )
        }
    }
}

@Composable
fun EmergencySOSScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isSOSActive by remember { mutableStateOf(false) }
    var userCondition by remember { mutableStateOf("心脏疾病") }
    
    val conditions = listOf("心脏疾病", "高血压", "糖尿病", "癫痫", "其他")
    
    var sosHandler by remember { mutableStateOf<Handler?>(null) }
    var sosRunnable by remember { mutableStateOf<Runnable?>(null) }
    
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
            }
        }
        sosHandler = Handler(Looper.getMainLooper())
    }
    
    fun startSOS() {
        isSOSActive = true
        val message = "紧急呼救！我有$userCondition，需要帮助！我的急救药在左手边。"
        
        // 取消之前的回调
        sosRunnable?.let { sosHandler?.removeCallbacks(it) }
        
        // 循环播报
        sosRunnable = object : Runnable {
            override fun run() {
                if (isSOSActive) {
                    textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                    sosHandler?.postDelayed(this, 5000)
                }
            }
        }
        sosRunnable?.let { sosHandler?.postDelayed(it, 1000) }
    }
    
    fun stopSOS() {
        isSOSActive = false
        // 取消所有待处理的回调
        sosRunnable?.let { sosHandler?.removeCallbacks(it) }
        textToSpeech?.speak("已停止紧急呼救", TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // 清理资源
            sosRunnable?.let { sosHandler?.removeCallbacks(it) }
            textToSpeech?.shutdown()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        TopAppBar(
            title = { Text("紧急呼救", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(32.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFFF5722),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "我的身体状况",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    conditions.forEach { condition ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { userCondition = condition }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = userCondition == condition,
                                onClick = { userCondition = condition }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(condition, fontSize = 20.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (isSOSActive) {
                        stopSOS()
                    } else {
                        startSOS()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSOSActive) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSOSActive) "停止呼救" else "开始呼救",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isSOSActive) "正在循环播报呼救信息..." else "点击按钮开始紧急呼救",
                fontSize = 20.sp,
                color = Color(0xFF666666)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 选择您的身体状况\n2. 点击'开始呼救'按钮\n3. 系统会循环播报您的急救信息\n4. 将手机放在显眼位置或握在手中",
                        fontSize = 18.sp,
                        color = Color(0xFF666666),
                        lineHeight = 28.sp
                    )
                }
            }
        }
    }
}
