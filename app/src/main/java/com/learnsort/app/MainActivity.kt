package com.learnsort.app

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class ItemKind { QUESTION, NOTE }

data class StudyItem(
    val id: Long,
    val kind: ItemKind,
    val title: String,
    val body: String,
    val answer: String = "",
    val explanation: String = "",
    val subject: String = "数学二",
    val chapter: String = "待整理",
    val questionType: String = "综合题",
    val tags: String = "",
    val cause: String = "",
    val mastery: String = "待复习",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AiConfig(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini"
)

private class StudyRepository(context: Context) {
    private val prefs = context.getSharedPreferences("learnsort_store", Context.MODE_PRIVATE)

    fun loadItems(): List<StudyItem> {
        val raw = prefs.getString("items", null)
        if (raw.isNullOrBlank()) return seedItems()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(array.getJSONObject(i).toItem())
            }
        }.getOrElse { seedItems() }
    }

    fun saveItems(items: List<StudyItem>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun loadConfig() = AiConfig(
        baseUrl = prefs.getString("ai_base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1",
        apiKey = prefs.getString("ai_key", "") ?: "",
        model = prefs.getString("ai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
    )

    fun saveConfig(config: AiConfig) {
        prefs.edit()
            .putString("ai_base_url", config.baseUrl)
            .putString("ai_key", config.apiKey)
            .putString("ai_model", config.model)
            .apply()
    }

    private fun seedItems() = listOf(
        StudyItem(
            id = 1L,
            kind = ItemKind.QUESTION,
            title = "二重积分交换积分次序",
            body = "设 D 是由 x=0、y=x 和 y=1 围成的区域，写出交换积分次序后的积分。",
            answer = "先画出区域，再按 x 分段：0≤x≤1 时，x≤y≤1。",
            explanation = "交换次序的关键是先确定区域的水平投影，再判断每个 x 对应的 y 范围。",
            subject = "数学二",
            chapter = "二重积分",
            questionType = "计算题",
            tags = "积分区域,交换次序,分段积分",
            cause = "不会画区域",
            mastery = "仍不会"
        ),
        StudyItem(
            id = 2L,
            kind = ItemKind.QUESTION,
            title = "英语长难句拆分",
            body = "识别主干、从句和修饰成分，整理成自己的翻译步骤。",
            answer = "先找谓语，再找主句主语，最后处理定语从句和插入语。",
            explanation = "长难句不要求一次读懂，先拆骨架再补充细节。",
            subject = "英语二",
            chapter = "长难句",
            questionType = "阅读理解",
            tags = "主干,从句,翻译",
            cause = "看不出主干",
            mastery = "待复习"
        ),
        StudyItem(
            id = 3L,
            kind = ItemKind.NOTE,
            title = "二重积分区域判断清单",
            body = "1. 先画边界；2. 找水平或竖直投影；3. 出现交点时分段；4. 检查上下限顺序。",
            subject = "数学二",
            chapter = "二重积分",
            tags = "积分区域,复习清单"
        )
    )

    private fun StudyItem.toJson() = JSONObject().apply {
        put("id", id)
        put("kind", kind.name)
        put("title", title)
        put("body", body)
        put("answer", answer)
        put("explanation", explanation)
        put("subject", subject)
        put("chapter", chapter)
        put("questionType", questionType)
        put("tags", tags)
        put("cause", cause)
        put("mastery", mastery)
        put("imageUri", imageUri ?: JSONObject.NULL)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toItem() = StudyItem(
        id = optLong("id"),
        kind = runCatching { ItemKind.valueOf(optString("kind")) }.getOrDefault(ItemKind.QUESTION),
        title = optString("title"),
        body = optString("body"),
        answer = optString("answer"),
        explanation = optString("explanation"),
        subject = optString("subject", "数学二"),
        chapter = optString("chapter", "待整理"),
        questionType = optString("questionType", "综合题"),
        tags = optString("tags"),
        cause = optString("cause"),
        mastery = optString("mastery", "待复习"),
        imageUri = optString("imageUri").takeUnless { it == "" || it == "null" },
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudyRepository(application)
    var items by mutableStateOf(repository.loadItems())
        private set
    var aiConfig by mutableStateOf(repository.loadConfig())
        private set
    var aiBusy by mutableStateOf(false)
        private set
    var aiResult by mutableStateOf<String?>(null)
        private set

    fun save(item: StudyItem) {
        val next = if (item.id == 0L) {
            items + item.copy(id = (items.maxOfOrNull { it.id } ?: 0L) + 1L)
        } else {
            items.map { if (it.id == item.id) item else it }
        }
        items = next
        repository.saveItems(next)
    }

    fun delete(item: StudyItem) {
        items = items.filterNot { it.id == item.id }
        repository.saveItems(items)
    }

    fun importImages(uris: List<Uri>) {
        uris.forEachIndexed { index, uri ->
            save(
                StudyItem(
                    id = 0L,
                    kind = ItemKind.QUESTION,
                    title = if (uris.size == 1) "待整理图片题目" else "待整理图片题目 ${index + 1}",
                    body = "已导入图片，请补充题干或使用 AI 识别。",
                    imageUri = uri.toString(),
                    chapter = "待整理",
                    mastery = "待整理"
                )
            )
        }
    }

    fun createNoteFromQuestion(item: StudyItem) {
        save(
            StudyItem(
                id = 0L,
                kind = ItemKind.NOTE,
                title = "${item.title} · 知识笔记",
                body = listOfNotNull(
                    "知识点：${item.tags.ifBlank { "待补充" }}",
                    "错因：${item.cause.ifBlank { "待补充" }}",
                    "解析：${item.explanation.ifBlank { "待补充" }}"
                ).joinToString("\n"),
                subject = item.subject,
                chapter = item.chapter,
                tags = item.tags
            )
        )
    }

    fun saveConfig(config: AiConfig) {
        aiConfig = config
        repository.saveConfig(config)
    }

    fun generateGuide(item: StudyItem) {
        aiBusy = true
        aiResult = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val base = aiConfig.baseUrl.trim().trimEnd('/').let {
                    if (it.endsWith("/chat/completions")) it else "${it}/chat/completions"
                }
                val prompt = "请根据以下错题生成简洁的复习提纲，包含：核心知识点、解题步骤、常见错因、一道自测问题。\n\n题目：${item.body}\n答案：${item.answer}\n解析：${item.explanation}"
                val request = JSONObject().apply {
                    put("model", aiConfig.model)
                    put("temperature", 0.2)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }))
                }
                val connection = (URL(base).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    if (aiConfig.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${aiConfig.apiKey}")
                }
                connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (connection.responseCode !in 200..299) error("接口返回 ${connection.responseCode}: ${response}")
                JSONObject(response).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content")
                    ?.takeIf { it.isNotBlank() }
                    ?: error("接口响应中没有找到 choices[0].message.content")
            }.getOrElse { "生成失败：${it.message ?: "未知错误"}" }
            withContext(Dispatchers.Main) {
                aiResult = result
                aiBusy = false
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LearnSortApp(viewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnSortApp(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<StudyItem?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.importImages(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tab == 0) "错题库" else "知识库") },
                actions = {
                    IconButton(onClick = { showGuide = true }) { Icon(Icons.Default.AutoAwesome, "AI助手") }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "设置") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF7F7FB))
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("错题库") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.MenuBook, null) }, label = { Text("知识库") })
            }
        },
        containerColor = Color(0xFFF7F7FB)
    ) { padding ->
        if (tab == 0) {
            QuestionScreen(
                padding = padding,
                items = vm.items.filter { it.kind == ItemKind.QUESTION },
                onEdit = { editor = it },
                onNew = { editor = StudyItem(0L, ItemKind.QUESTION, "", "") },
                onImport = { picker.launch(arrayOf("image/*")) },
                onMakeNote = vm::createNoteFromQuestion
            )
        } else {
            NoteScreen(
                padding = padding,
                items = vm.items.filter { it.kind == ItemKind.NOTE },
                onEdit = { editor = it },
                onNew = { editor = StudyItem(0L, ItemKind.NOTE, "", "") }
            )
        }
    }

    editor?.let { item ->
        EditorDialog(
            item = item,
            aiBusy = vm.aiBusy,
            aiResult = vm.aiResult,
            onDismiss = { editor = null },
            onSave = { vm.save(it); editor = null },
            onDelete = { vm.delete(item); editor = null },
            onGenerate = vm::generateGuide,
            onMakeNote = { vm.createNoteFromQuestion(item) }
        )
    }
    if (showSettings) {
        SettingsDialog(vm.aiConfig, onDismiss = { showSettings = false }, onSave = { vm.saveConfig(it); showSettings = false })
    }
    if (showGuide) {
        AssistantDialog(vm.items.filter { it.kind == ItemKind.QUESTION }, onDismiss = { showGuide = false }, onOpen = { editor = it; showGuide = false })
    }
}

@Composable
private fun QuestionScreen(
    padding: PaddingValues,
    items: List<StudyItem>,
    onEdit: (StudyItem) -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onMakeNote: (StudyItem) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedMastery by rememberSaveable { mutableStateOf("全部") }
    val filtered = items.filter {
        (query.isBlank() || "${it.title}${it.body}${it.tags}${it.subject}".contains(query, true)) &&
            (selectedMastery == "全部" || it.mastery == selectedMastery)
    }
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("搜索题目、知识点或备注") },
                shape = RoundedCornerShape(16.dp)
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("全部", "待复习", "仍不会", "已掌握", "待整理").forEach { status ->
                    FilterChip(selected = selectedMastery == status, onClick = { selectedMastery = status }, label = { Text(status) })
                }
            }
            Text("${filtered.size} 道题目", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { item ->
                    QuestionCard(item, onClick = { onEdit(item) }, onMakeNote = { onMakeNote(item) })
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onImport) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp)); Text("导入图片") }
            ExtendedFloatingActionButton(onClick = onNew, icon = { Icon(Icons.Default.Add, null) }, text = { Text("新建题目") })
        }
    }
}

@Composable
private fun NoteScreen(padding: PaddingValues, items: List<StudyItem>, onEdit: (StudyItem) -> Unit, onNew: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = items.filter { query.isBlank() || "${it.title}${it.body}${it.tags}".contains(query, true) }
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 10.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("搜索知识点或主题笔记") },
                shape = RoundedCornerShape(16.dp)
            )
            Text("${filtered.size} 篇笔记", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { item -> NoteCard(item, onClick = { onEdit(item) }) }
            }
        }
        ExtendedFloatingActionButton(onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), icon = { Icon(Icons.Default.Add, null) }, text = { Text("新建笔记") })
    }
}

@Composable
private fun QuestionCard(item: StudyItem, onClick: () -> Unit, onMakeNote: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF5865F2), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.title.ifBlank { "未命名题目" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                MasteryBadge(item.mastery)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AssistChip(onClick = {}, label = { Text(item.subject) })
                AssistChip(onClick = {}, label = { Text(item.chapter) })
                item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
            }
            TextButton(onClick = onMakeNote, contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("整理成知识笔记") }
        }
    }
}

@Composable
private fun NoteCard(item: StudyItem, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, null, tint = Color(0xFF00897B))
                Spacer(Modifier.width(8.dp))
                Text(item.title.ifBlank { "未命名笔记" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text("${item.subject} · ${item.chapter}", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MasteryBadge(value: String) {
    val color = when (value) {
        "已掌握" -> Color(0xFF2E7D32)
        "仍不会" -> Color(0xFFC62828)
        "待整理" -> Color(0xFFEF6C00)
        else -> Color(0xFF5E35B1)
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) { Text(value, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) }
}

@Composable
private fun EditorDialog(
    item: StudyItem,
    aiBusy: Boolean,
    aiResult: String?,
    onDismiss: () -> Unit,
    onSave: (StudyItem) -> Unit,
    onDelete: () -> Unit,
    onGenerate: (StudyItem) -> Unit,
    onMakeNote: () -> Unit
) {
    var draft by remember(item.id) { mutableStateOf(item) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSave(draft) }) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(6.dp)); Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (draft.kind == ItemKind.QUESTION) "编辑错题" else "编辑知识笔记") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditField("标题", draft.title) { draft = draft.copy(title = it) }
                EditField(if (draft.kind == ItemKind.QUESTION) "题干 / 内容" else "笔记内容", draft.body, minLines = 5) { draft = draft.copy(body = it) }
                EditField("学科", draft.subject) { draft = draft.copy(subject = it) }
                EditField("章节 / 主题", draft.chapter) { draft = draft.copy(chapter = it) }
                EditField("标签（用逗号分隔）", draft.tags) { draft = draft.copy(tags = it) }
                if (draft.kind == ItemKind.QUESTION) {
                    EditField("参考答案", draft.answer, minLines = 3) { draft = draft.copy(answer = it) }
                    EditField("解析", draft.explanation, minLines = 3) { draft = draft.copy(explanation = it) }
                    EditField("错因", draft.cause) { draft = draft.copy(cause = it) }
                    EditField("掌握情况", draft.mastery) { draft = draft.copy(mastery = it) }
                    if (!draft.imageUri.isNullOrBlank()) Text("已关联图片：${draft.imageUri}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onGenerate(draft) }, enabled = !aiBusy) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(5.dp)); Text(if (aiBusy) "生成中…" else "AI复习提纲") }
                        OutlinedButton(onClick = onMakeNote) { Icon(Icons.Default.Book, null); Spacer(Modifier.width(5.dp)); Text("生成笔记") }
                    }
                    aiResult?.let { result ->
                        Surface(color = Color(0xFFF0F4FF), shape = RoundedCornerShape(12.dp)) { Text(result, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (draft.id != 0L) TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(5.dp)); Text("删除") }
            }
        }
    )
}

@Composable
private fun EditField(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = minLines)
}

@Composable
private fun SettingsDialog(config: AiConfig, onDismiss: () -> Unit, onSave: (AiConfig) -> Unit) {
    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var model by remember { mutableStateOf(config.model) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSave(AiConfig(baseUrl.trim(), apiKey.trim(), model.trim())) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("AI 接口设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("支持 OpenAI 兼容的 /chat/completions 接口。配置保存在本机。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                EditField("接口地址", baseUrl) { baseUrl = it }
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key（可选）") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                EditField("模型名称", model) { model = it }
            }
        }
    )
}

@Composable
private fun AssistantDialog(items: List<StudyItem>, onDismiss: () -> Unit, onOpen: (StudyItem) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF6A1B9A)); Spacer(Modifier.width(8.dp)); Text("AI 学习助手") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择一道错题，在详情中生成复习提纲。", style = MaterialTheme.typography.bodyMedium)
                items.take(5).forEach { item ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(item) }) { Text(item.title, modifier = Modifier.padding(12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
                if (items.isEmpty()) Text("还没有错题，先导入一张图片或新建题目。", color = Color.Gray)
            }
        }
    )
}
