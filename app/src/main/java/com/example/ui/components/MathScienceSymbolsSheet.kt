package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PHYSICS_SYMBOLS = listOf(
    "Δ", "θ", "λ", "α", "β", "π", "ω", "Ω", "μ", "±", "√", "∫", "∑", "∞", "≈", "≠",
    "F⃗", "v⃗", "a⃗", "p⃗", "E⃗", "B⃗", "10⁻⁶", "10⁻³", "10³", "10⁶", "½", "²/₃",
    "g = 9.8 m/s²", "c = 3×10⁸ m/s", "h = 6.63×10⁻³⁴ J·s", "e = 1.6×10⁻¹⁹ C"
)

private val CHEMISTRY_SYMBOLS = listOf(
    "→", "⇌", "↑", "↓", "Δ", "°C", "K", "pH", "[H⁺]", "[OH⁻]", "Kₐ", "K_b", "K_sp",
    "α", "β", "γ", "σ", "π", "H⁺", "OH⁻", "e⁻", "mol/L",
    "H₂O", "CO₂", "H₂SO₄", "HCl", "HNO₃", "NaOH", "NaCl", "C₆H₁₂O₆",
    "Resonance ⟷", "Reversible ⇌"
)

private val BIOLOGY_SYMBOLS = listOf(
    "♀", "♂", "DNA", "RNA", "mRNA", "tRNA", "ATP", "ADP", "NADP+", "NADPH",
    "5'", "3'", "µm", "nm", "mm", "F₁ Generation", "F₂ Generation",
    "Dominant (AA)", "Recessive (aa)", "Heterozygous (Aa)", "Mitosis", "Meiosis"
)

private val NEET_STEP_TEMPLATES = listOf(
    "Given Data:",
    "Formula Applied:",
    "Step-by-Step Calculation:",
    "Final Answer with Units:",
    "Reasoning / Concept:",
    "Reaction Mechanism & Conditions:",
    "Diagram Label:"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MathScienceSymbolsSheet(
    onDismiss: () -> Unit,
    onSymbolSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Physics", "Chemistry", "Biology", "NEET Steps")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "NEET Formulas & Symbols Palette",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any symbol or step header to insert into your answer",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Tab bar
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            val symbols = when (selectedTab) {
                0 -> PHYSICS_SYMBOLS
                1 -> CHEMISTRY_SYMBOLS
                2 -> BIOLOGY_SYMBOLS
                else -> NEET_STEP_TEMPLATES
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                symbols.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .clickable {
                                onSymbolSelected(item)
                                onDismiss()
                            }
                            .testTag("symbol_chip_$item")
                    ) {
                        Text(
                            text = item,
                            fontSize = if (selectedTab == 3) 13.sp else 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
