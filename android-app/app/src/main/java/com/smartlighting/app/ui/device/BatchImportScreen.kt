package com.smartlighting.app.ui.device

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartlighting.app.data.model.BatchCreateResult
import com.smartlighting.app.data.model.ImportRow
import com.smartlighting.app.ui.theme.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

private data class ParseResult(
    val rows: List<ImportRow> = emptyList(),
    val total: Int = 0,
    val validCount: Int = 0,
    val errorCount: Int = 0,
    val fileName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchImportScreen(
    onBack: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(0) } // 0=upload, 1=preview, 2=result
    var parseResult by remember { mutableStateOf(ParseResult()) }
    var importResult by remember { mutableStateOf<BatchCreateResult?>(null) }
    var importSuccess by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        parsing = true
        parseError = null
        try {
            val fileName = getFileName(context, uri)
            val rows = parseFile(context, uri)
            parseResult = ParseResult(
                rows = rows,
                total = rows.size,
                validCount = rows.count { it.valid },
                errorCount = rows.count { !it.valid },
                fileName = fileName
            )
            step = 1
        } catch (e: Exception) {
            parseError = e.message ?: "文件解析失败"
        } finally {
            parsing = false
        }
    }

    Scaffold(
        containerColor = DarkBlue,
        topBar = {
            TopAppBar(
                title = { Text("批量新增设备", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue, titleContentColor = TextPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkBlue, Navy)))
        ) {
            StepIndicator(step)

            when (step) {
                0 -> UploadStep(
                    parsing = parsing,
                    parseError = parseError,
                    onPickFile = {
                        filePickerLauncher.launch(arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        ))
                    }
                )
                1 -> PreviewStep(
                    parseResult = parseResult,
                    importing = importing,
                    onBackToUpload = { step = 0 },
                    onImport = {
                        importing = true
                        viewModel.batchCreateDevices(parseResult.rows) { ok, result ->
                            importing = false
                            importSuccess = ok && result != null
                            importResult = result
                            step = 2
                        }
                    }
                )
                2 -> ResultStep(
                    success = importSuccess,
                    result = importResult,
                    onDone = onBack,
                    onImportMore = {
                        step = 0
                        parseResult = ParseResult()
                        importResult = null
                    }
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    val steps = listOf("上传文件", "预览校验", "导入结果")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { i, label ->
            val isActive = i == currentStep
            val isDone = i < currentStep
            val color = when {
                isDone -> Green
                isActive -> Cyan
                else -> TextMuted
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
                Text(label, fontSize = 10.sp, color = if (isActive) Cyan else TextMuted)
            }
            if (i < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    color = if (i < currentStep) Green else TextMuted.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun UploadStep(
    parsing: Boolean,
    parseError: String?,
    onPickFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Upload zone
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable(enabled = !parsing) { onPickFile() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (parsing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Cyan, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("正在解析文件...", fontSize = 13.sp, color = TextMuted)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.UploadFile,
                            null,
                            tint = Cyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("点击选择文件", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("支持 .xlsx 和 .csv 格式", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        // Parse error
        if (parseError != null) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, null, tint = Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(parseError, fontSize = 12.sp, color = Red)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Format hint
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("文件格式说明", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text("• 第一行为表头，自动匹配列名", fontSize = 11.sp, color = TextSecondary)
                Text("• 支持列名: 设备编号/deviceId, 名称/name, 区域/area", fontSize = 11.sp, color = TextSecondary)
                Text("•  经度/longitude, 纬度/latitude, 出厂编号/factorySerial", fontSize = 11.sp, color = TextSecondary)
                Text("• 出厂编号为必填，1-30字符", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun PreviewStep(
    parseResult: ParseResult,
    importing: Boolean,
    onBackToUpload: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // File name + summary
        if (parseResult.fileName.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(parseResult.fileName, fontSize = 12.sp, color = TextMuted)
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(8.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${parseResult.total}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("总计", fontSize = 11.sp, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${parseResult.validCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Green)
                Text("有效", fontSize = 11.sp, color = TextMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${parseResult.errorCount}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Red)
                Text("错误", fontSize = 11.sp, color = TextMuted)
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(parseResult.rows) { _, row ->
                ImportRowCard(row)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBackToUpload,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan)
            ) {
                Text("重新选择")
            }
            Button(
                onClick = onImport,
                enabled = parseResult.validCount > 0 && !importing,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (importing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("确认导入 (${parseResult.validCount})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ImportRowCard(row: ImportRow) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.valid) CardBg else CardBg.copy(alpha = 0.8f)
        ),
        border = if (row.valid) null
        else BorderStroke(1.dp, Red.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${row.rowNum}", fontSize = 10.sp, color = TextMuted, modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.deviceId, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (row.name.isNotBlank()) Text(row.name, fontSize = 10.sp, color = TextSecondary)
                        if (row.area.isNotBlank()) Text(row.area, fontSize = 10.sp, color = TextSecondary)
                        if (row.factorySerial.isNotBlank()) Text(row.factorySerial, fontSize = 10.sp, color = TextSecondary)
                    }
                }
                if (!row.valid) {
                    Icon(Icons.Default.Error, null, tint = Red, modifier = Modifier.size(16.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(16.dp))
                }
            }
            if (!row.valid) {
                Text(
                    row.errors.joinToString("; "),
                    fontSize = 10.sp,
                    color = Red,
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultStep(
    success: Boolean,
    result: BatchCreateResult?,
    onDone: () -> Unit,
    onImportMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Icon(
            if (success && result?.failed == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
            null,
            tint = if (success && result?.failed == 0) Green else Amber,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            when {
                !success -> "导入失败"
                result?.failed == 0 -> "全部导入成功"
                else -> "部分导入成功"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        if (result != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${result.total}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("总数", fontSize = 11.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${result.success}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Green)
                    Text("成功", fontSize = 11.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${result.failed}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red)
                    Text("失败", fontSize = 11.sp, color = TextMuted)
                }
            }

            if (result.failedDetails.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("失败详情", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    result.failedDetails.forEach { detail ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg)
                        ) {
                            Text(
                                "第${detail.row}行 [${detail.deviceId ?: "?"}]: ${detail.reason}",
                                fontSize = 11.sp,
                                color = Red,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text("网络请求失败，请重试", fontSize = 14.sp, color = Red)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onImportMore,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan)
            ) {
                Text("继续导入")
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("完成", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── File Parsing ──

private fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && it.moveToFirst()) it.getString(nameIndex) else "unknown"
    } ?: "unknown"
}

private fun parseFile(context: Context, uri: Uri): List<ImportRow> {
    val fileName = getFileName(context, uri).lowercase()

    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("无法读取文件")

    return if (fileName.endsWith(".csv")) {
        parseCsv(inputStream)
    } else if (fileName.endsWith(".xlsx")) {
        parseXlsx(inputStream)
    } else {
        throw Exception("不支持的文件格式，请使用 .xlsx 或 .csv 文件")
    }
}

private fun parseCsv(inputStream: InputStream): List<ImportRow> {
    val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
    val lines = reader.readLines().filter { it.isNotBlank() }
    if (lines.isEmpty()) throw Exception("文件为空")

    val headerLine = lines.first()
    val colMap = buildColumnMap(headerLine)
    val dataLines = lines.drop(1)

    return dataLines.mapIndexed { index, line ->
        val fields = parseCsvLine(line)
        validateRow(index + 1, colMap, fields)
    }
}

private fun parseXlsx(inputStream: InputStream): List<ImportRow> {
    val zip = ZipInputStream(inputStream)

    // Read shared strings
    val sharedStrings = mutableListOf<String>()
    var sheetXml: String? = null

    var entry = zip.nextEntry
    while (entry != null) {
        val name = entry.name
        when {
            name == "xl/sharedStrings.xml" -> {
                val text = zip.readBytes().toString(Charsets.UTF_8)
                sharedStrings.addAll(parseXlsxSharedStrings(text))
            }
            name == "xl/worksheets/sheet1.xml" -> {
                sheetXml = zip.readBytes().toString(Charsets.UTF_8)
            }
        }
        entry = zip.nextEntry
    }
    zip.close()

    if (sheetXml == null) throw Exception("无法找到工作表 sheet1")

    return parseXlsxSheet(sheetXml, sharedStrings)
}

private fun parseXlsxSharedStrings(xml: String): List<String> {
    val result = mutableListOf<String>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(xml.reader())

    var inText = false
    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.START_TAG -> if (parser.name == "t") inText = true
            XmlPullParser.TEXT -> if (inText) result.add(parser.text)
            XmlPullParser.END_TAG -> if (parser.name == "t") inText = false
        }
        parser.next()
    }
    return result
}

private fun parseXlsxSheet(xml: String, sharedStrings: List<String>): List<ImportRow> {
    // Parse sheet XML to extract rows and cells
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(xml.reader())

    val rows = mutableListOf<MutableMap<String, String>>()
    var currentRow = mutableMapOf<String, String>()
    var inRow = false
    var inCell = false
    var cellRef = ""
    var cellType = ""
    var cellText = ""

    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        when (parser.eventType) {
            XmlPullParser.START_TAG -> {
                when (parser.name) {
                    "row" -> {
                        currentRow = mutableMapOf()
                        inRow = true
                    }
                    "c" -> {
                        cellRef = parser.getAttributeValue(null, "r") ?: ""
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        inCell = true
                        cellText = ""
                    }
                    "v" -> if (inCell) cellText = ""
                }
            }
            XmlPullParser.TEXT -> {
                if (inCell) cellText = parser.text
            }
            XmlPullParser.END_TAG -> {
                when (parser.name) {
                    "row" -> {
                        if (currentRow.isNotEmpty()) rows.add(currentRow)
                        inRow = false
                    }
                    "c" -> {
                        if (inRow && cellRef.isNotBlank()) {
                            val col = cellRef.filter { it.isLetter() }
                            val value = if (cellType == "s") {
                                val idx = cellText.toIntOrNull()
                                if (idx != null && idx < sharedStrings.size) sharedStrings[idx] else ""
                            } else {
                                cellText
                            }
                            currentRow[col] = value
                        }
                        inCell = false
                    }
                }
            }
        }
        parser.next()
    }

    if (rows.isEmpty()) throw Exception("工作表中没有数据")

    // First row is header
    val headerRow = rows.first()
    val dataRows = rows.drop(1)

    // Build column index from header
    val colMap = mutableMapOf<String, Int>()
    dataRows.forEachIndexed { index, _ ->
        headerRow.forEach { (col, label) ->
            val normalized = label.trim().lowercase().replace(Regex("[\\s_\\-()（）]"), "")
            val key = mapColumnKey(normalized)
            if (key != null && !colMap.containsKey(key)) {
                colMap[key] = col[0].code - 'A'.code // Column A=0, B=1, etc.
            }
        }
    }

    // Convert column indices back to letters
    val colLetterMap = mutableMapOf<String, String>()
    val colKeys = listOf("deviceId", "name", "area", "longitude", "latitude", "ratedPower", "factorySerial")
    val colLetters = listOf("A", "B", "C", "D", "E", "F", "G")
    colKeys.forEachIndexed { i, key ->
        val letter = colLetters.getOrNull(i) ?: return@forEachIndexed
        // Check if this key was found in header, otherwise use default column
        val foundEntry = colMap.entries.find { it.key == key }
        if (foundEntry != null) {
            colLetterMap[key] = ('A'.code + foundEntry.value).toChar().toString()
        } else {
            colLetterMap[key] = letter
        }
    }

    return dataRows.mapIndexed { index, rowData ->
        val rowNum = index + 1
        val deviceId = rowData[colLetterMap["deviceId"]] ?: ""
        val name = rowData[colLetterMap["name"]] ?: ""
        val area = rowData[colLetterMap["area"]] ?: ""
        val longitude = rowData[colLetterMap["longitude"]] ?: ""
        val latitude = rowData[colLetterMap["latitude"]] ?: ""
        val factorySerial = rowData[colLetterMap["factorySerial"]] ?: ""
        val errors = validateFields(deviceId, longitude, latitude, factorySerial)
        ImportRow(rowNum, deviceId, name, area, longitude, latitude, factorySerial, errors)
    }
}

private fun buildColumnMap(headerLine: String): Map<String, Int> {
    val headers = parseCsvLine(headerLine).map { it.trim().lowercase().replace(Regex("[\\s_\\-()（）]"), "") }
    val map = mutableMapOf<String, Int>()
    for (i in headers.indices) {
        val key = mapColumnKey(headers[i])
        if (key != null) map[key] = i
    }
    return map
}

private fun mapColumnKey(normalized: String): String? {
    val deviceIdKeys = setOf("deviceid", "设备编号", "编号", "id", "设备id", "设备编码")
    val nameKeys = setOf("name", "名称", "设备名称", "设备名", "devicename")
    val areaKeys = setOf("area", "区域", "分区", "所属区域", "region")
    val lngKeys = setOf("longitude", "经度", "lng")
    val latKeys = setOf("latitude", "纬度", "lat")
    val factoryKeys = setOf("factoryserial", "出厂编号", "出厂编码", "序列号", "serial")
    val powerKeys = setOf("ratedpower", "功率", "额定功率", "power")

    return when {
        normalized in deviceIdKeys -> "deviceId"
        normalized in nameKeys -> "name"
        normalized in areaKeys -> "area"
        normalized in lngKeys -> "longitude"
        normalized in latKeys -> "latitude"
        normalized in factoryKeys -> "factorySerial"
        normalized in powerKeys -> "ratedPower"
        else -> null
    }
}

private fun validateRow(rowNum: Int, colMap: Map<String, Int>, fields: List<String>): ImportRow {
    val deviceId = colMap["deviceId"]?.let { fields.getOrElse(it) { "" }.trim() } ?: ""
    val name = colMap["name"]?.let { fields.getOrElse(it) { "" }.trim() } ?: ""
    val area = colMap["area"]?.let { fields.getOrElse(it) { "" }.trim() } ?: ""
    val longitude = colMap["longitude"]?.let { fields.getOrElse(it) { "" }.trim() } ?: ""
    val latitude = colMap["latitude"]?.let { fields.getOrElse(it) { "" }.trim() } ?: ""
    val factorySerial = colMap["factorySerial"]?.let { fields.getOrElse(it) { "" }.trim() } ?: ""

    val errors = validateFields(deviceId, longitude, latitude, factorySerial)
    return ImportRow(rowNum, deviceId, name, area, longitude, latitude, factorySerial, errors)
}

private fun validateFields(deviceId: String, longitude: String, latitude: String, factorySerial: String): List<String> {
    val errors = mutableListOf<String>()

    when {
        deviceId.isBlank() -> errors.add("设备编号不能为空")
        deviceId.length > 50 -> errors.add("设备编号不能超过50字符")
        !deviceId.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9_]*$")) ->
            errors.add("设备编号只能包含字母、数字和下划线")
    }

    if (longitude.isNotBlank()) {
        val lng = longitude.toDoubleOrNull()
        when {
            lng == null -> errors.add("经度格式无效")
            lng < 73 || lng > 135 -> errors.add("经度超出中国范围(73-135)")
        }
    }

    if (latitude.isNotBlank()) {
        val lat = latitude.toDoubleOrNull()
        when {
            lat == null -> errors.add("纬度格式无效")
            lat < 18 || lat > 54 -> errors.add("纬度超出中国范围(18-54)")
        }
    }

    when {
        factorySerial.isBlank() -> errors.add("出厂编号不能为空")
        factorySerial.length > 30 -> errors.add("出厂编号不能超过30字符")
    }

    return errors
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    for (char in line) {
        when {
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                result.add(current.toString())
                current.clear()
            }
            else -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}
