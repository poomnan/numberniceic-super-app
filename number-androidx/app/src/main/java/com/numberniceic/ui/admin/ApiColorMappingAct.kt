package com.numberniceic.ui.admin

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.utils.OutfitApiColorEntry
import com.numberniceic.utils.OutfitApiColorMappingManager

class ApiColorMappingAct : AppCompatActivity() {
    private lateinit var adapter: ApiColorMappingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_color_mapping)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_api_color_mapping))
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "ตั้งค่าสีเสื้อผ้าที่ใส่"

        adapter = ApiColorMappingAdapter(
            items = emptyList(),
            onEdit = { entry -> showEditDialog(entry) },
            onReset = { entry ->
                OutfitApiColorMappingManager.clearOverride(this, entry.sourceColorName)
                refreshList()
            },
            onEditDayShade = { dayNumber, dayName, shadeIndex, currentHex ->
                showColorSelectDialog(
                    title = "เลือกสีเฉด ${shadeIndex + 1} - วัน$dayName",
                    initialHex = currentHex
                ) { picked ->
                    val shades = OutfitApiColorMappingManager.getDayPalette(this, dayNumber).toMutableList()
                    while (shades.size < 5) {
                        shades.add(shades.lastOrNull() ?: "#FFFFFF")
                    }
                    shades[shadeIndex] = picked
                    OutfitApiColorMappingManager.saveDayPalette(this, dayNumber, shades)
                    refreshList()
                    Thread {
                        OutfitApiColorMappingManager.pushDayPalettesToServer(this)
                        OutfitApiColorMappingManager.syncDayPalettesFromServer(this)
                        runOnUiThread { refreshList() }
                    }.start()
                }
            }
        )

        val toolbar = findViewById<Toolbar>(R.id.toolbar_api_color_mapping)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_api_color_mapping)

        // Handle Window Insets for Edge-to-Edge (Android 15+)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Push toolbar down by status bar height
            toolbar.updatePadding(top = systemBars.top)
            // Adjust toolbar height to include the status bar height
            toolbar.layoutParams.height = dp(56) + systemBars.top
            
            // Add bottom padding to RecyclerView to avoid overlapping with navigation bar
            recyclerView.updatePadding(bottom = dp(12) + systemBars.bottom)
            
            insets
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ApiColorMappingAct)
            adapter = this@ApiColorMappingAct.adapter
        }

        refreshList()
        Thread {
            OutfitApiColorMappingManager.syncDayPalettesFromServer(this)
            runOnUiThread { refreshList() }
        }.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshList() {
        val entries = OutfitApiColorMappingManager.getEntries(this)
            .sortedWith(
                compareBy<OutfitApiColorEntry> { it.dayNumber ?: Int.MAX_VALUE }
                    .thenBy { it.dayName ?: "" }
                    .thenBy { it.colorName }
            )
        val rows = mutableListOf<ApiColorMappingRow>()
        var lastDayNumber: Int? = null
        var lastDayName: String? = null
        entries.forEach { entry ->
            if (entry.dayNumber != lastDayNumber || entry.dayName != lastDayName) {
                val dayNumber = entry.dayNumber
                rows.add(
                    ApiColorMappingRow.DayHeader(
                        dayName = entry.dayName ?: "-",
                        dayNumber = dayNumber,
                        palette = if (dayNumber != null) OutfitApiColorMappingManager.getDayPalette(this, dayNumber) else emptyList()
                    )
                )
                lastDayNumber = entry.dayNumber
                lastDayName = entry.dayName
            }
        }
        adapter.submitList(rows)
    }

    private fun showEditDialog(entry: OutfitApiColorEntry) {
        showColorSelectDialog(
            title = "แก้ไขสี: ${entry.colorName}",
            initialHex = entry.activeHex
        ) { selectedHex ->
            OutfitApiColorMappingManager.saveOverride(this, entry.sourceColorName, selectedHex)
            refreshList()
        }
    }

    private val recentColors = mutableListOf<String>()

    private fun showColorSelectDialog(title: String, initialHex: String, onSelected: (String) -> Unit) {
        var selectedHex = OutfitApiColorMappingManager.normalizeHex(initialHex)
        var selectedColor = runCatching { Color.parseColor(selectedHex) }.getOrDefault(Color.WHITE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(10))
            background = roundedBg(Color.WHITE, dp(16).toFloat())
        }

        // 1. Spectrum Grid (เอา Tab ออก เน้นพื้นที่ตรงนี้)
        val specView = SpectrumColorGridView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(280)
            )
            setColor(selectedColor)
        }

        // 2. Control Row (Preview + Hex)
        val ctrlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20); bottomMargin = dp(10) }
        }

        val previewCircle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50)).apply { marginEnd = dp(15) }
            background = roundedOval(selectedColor)
        }

        val hexContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            
            addView(TextView(this@ApiColorMappingAct).apply {
                text = "HEX CODE"; textSize = 11f; setTextColor(Color.GRAY)
            })
            addView(TextView(this@ApiColorMappingAct).apply {
                text = selectedHex; textSize = 18f; setTextColor(Color.BLACK)
                setPadding(0, dp(2), 0, 0)
            })
        }

        val txtHexDisplay = hexContainer.getChildAt(1) as TextView

        val btnEdit = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            background = null
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                val input = android.widget.EditText(this@ApiColorMappingAct).apply {
                    setText(selectedHex); setSelection(text.length)
                }
                AlertDialog.Builder(this@ApiColorMappingAct)
                    .setTitle("ป้อนรหัสสี")
                    .setView(input)
                    .setPositiveButton("ตกลง") { _, _ ->
                        val h = OutfitApiColorMappingManager.normalizeHex(input.text.toString().trim())
                        if (runCatching { Color.parseColor(h) }.isSuccess) {
                            selectedHex = h
                            selectedColor = Color.parseColor(h)
                            txtHexDisplay.text = h
                            previewCircle.background = roundedOval(selectedColor)
                            specView.setColor(selectedColor)
                        }
                    }.show()
            }
        }

        specView.onColorChanged = { color ->
            selectedColor = color
            selectedHex = color.toHexColor()
            txtHexDisplay.text = selectedHex
            previewCircle.background = roundedOval(color)
        }

        ctrlRow.addView(previewCircle)
        ctrlRow.addView(hexContainer)
        ctrlRow.addView(btnEdit)

        // 3. Recent Colors
        val recentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40)
            ).apply { topMargin = dp(10) }
        }
        fun refreshRecent() {
            recentRow.removeAllViews()
            recentColors.forEach { hex ->
                val dot = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(8) }
                    background = roundedOval(Color.parseColor(hex))
                    setOnClickListener {
                        selectedHex = hex
                        selectedColor = Color.parseColor(hex)
                        txtHexDisplay.text = hex
                        previewCircle.background = roundedOval(selectedColor)
                        specView.setColor(selectedColor)
                    }
                }
                recentRow.addView(dot)
            }
        }
        refreshRecent()

        root.addView(specView)
        root.addView(ctrlRow)
        root.addView(recentRow)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(root)
            .setPositiveButton("เลือกสีนี้") { _, _ ->
                if (!recentColors.contains(selectedHex)) {
                    recentColors.add(0, selectedHex)
                    if (recentColors.size > 8) recentColors.removeAt(8)
                }
                onSelected(selectedHex)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    // Keep legacy showGradientPicker for any other callers (no longer called from showColorSelectDialog)
    private fun showGradientPicker(initialHex: String, onSelected: (String) -> Unit) {
        showColorSelectDialog("เลือกสีแบบไล่เฉด", initialHex, onSelected)
    }

    private fun buildPaletteColors(activeHex: String): List<String> {
        val values = linkedSetOf<String>()
        OutfitApiColorMappingManager.defaultMap.values.forEach { raw ->
            val normalized = OutfitApiColorMappingManager.normalizeHex(raw)
            if (runCatching { Color.parseColor(normalized) }.isSuccess) {
                values.add(normalized)
            }
        }
        listOf(
            "#FF1744", "#D50000", "#FF6D00", "#FFEA00", "#76FF03", "#00E676",
            "#00B0FF", "#2979FF", "#651FFF", "#D500F9", "#F50057", "#795548",
            "#607D8B", "#9E9E9E", "#212121", "#FFFFFF"
        ).forEach { values.add(it) }
        val active = OutfitApiColorMappingManager.normalizeHex(activeHex)
        if (runCatching { Color.parseColor(active) }.isSuccess) {
            values.add(active)
        }
        return values.toList()
    }

    private fun swatchDrawable(hex: String, selected: Boolean): GradientDrawable {
        val borderColor = if (selected) Color.parseColor("#1B5E20") else Color.parseColor("#B0BEC5")
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(hex))
            setStroke(if (selected) dp(3) else dp(1), borderColor)
        }
    }

    /** Filled circle drawable */
    private fun roundedOval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    /** Rounded rectangle drawable */
    private fun roundedBg(color: Int, radius: Float) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(value: Int): Int {
        return android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun Int.toHexColor(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }
}
