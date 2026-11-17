package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.AcaraTwoToneIcon
import com.example.ui.components.AnggaranTwoToneIcon
import com.example.ui.components.AsetTwoToneIcon
import com.example.ui.components.CariTwoToneIcon
import com.example.ui.components.DaftarTungguTwoToneIcon
import com.example.ui.components.JalanUangTwoToneIcon
import com.example.ui.components.KategoriTwoToneIcon
import com.example.ui.components.LabelTwoToneIcon
import com.example.ui.components.NilaiBersihTwoToneNavIcon
import com.example.ui.components.PerencanaTwoToneIcon
import com.example.ui.components.TabunganTwoToneIcon
import com.example.ui.components.TagihanTwoToneIcon
import com.example.ui.components.UtangTwoToneNavIcon
import com.example.ui.components.resolveUniformQuickIconTone
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentBlueBright
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun MainMenuOrderSheet(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    val currentOrder = remember {
        mutableStateListOf<String>().apply {
            addAll(settings.mainMenuOrder)
        }
    }

    val hiddenItems = remember {
        mutableStateListOf<String>().apply {
            addAll(settings.hiddenMenuItems)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Top Bar matching video: "Atur menu utama" and "Selesai"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Atur menu utama",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )

                    TextButton(
                        onClick = {
                            viewModel.updateMainMenuOrder(currentOrder.toList(), hiddenItems.toList())
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "Selesai",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlueBright
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(currentOrder, key = { _, item -> item }) { index, itemName ->
                        val isHidden = hiddenItems.contains(itemName)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHidden) DarkSurfaceElevated.copy(alpha = 0.5f) else DarkSurface
                            ),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom item icon
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceElevated,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            MenuItemIcon(itemName = itemName)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = itemName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isHidden) TextMuted else TextWhite
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Eye Toggle Icon
                                    IconButton(
                                        onClick = {
                                            if (isHidden) {
                                                hiddenItems.remove(itemName)
                                            } else {
                                                hiddenItems.add(itemName)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isHidden) "Tampilkan" else "Sembunyikan",
                                            tint = if (isHidden) TextMuted else TextWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Up Reorder
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val item = currentOrder.removeAt(index)
                                                currentOrder.add(index - 1, item)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Naik",
                                            tint = if (index > 0) TextWhite else Color(0x33FFFFFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Down Reorder
                                    IconButton(
                                        onClick = {
                                            if (index < currentOrder.size - 1) {
                                                val item = currentOrder.removeAt(index)
                                                currentOrder.add(index + 1, item)
                                            }
                                        },
                                        enabled = index < currentOrder.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Turun",
                                            tint = if (index < currentOrder.size - 1) TextWhite else Color(0x33FFFFFF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItemIcon(itemName: String, modifier: Modifier = Modifier.size(20.dp)) {
    val uniformTone = resolveUniformQuickIconTone(AccentBlue)
    when (itemName) {
        "Anggaran" -> AnggaranTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Perencana" -> PerencanaTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Acara" -> AcaraTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Aset" -> AsetTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Tabungan" -> TabunganTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Nilai Bersih" -> NilaiBersihTwoToneNavIcon(themeColor = uniformTone, isSelected = true, modifier = modifier)
        "Jalan Uang" -> JalanUangTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Utang" -> UtangTwoToneNavIcon(themeColor = uniformTone, isSelected = true, modifier = modifier)
        "Tagihan" -> TagihanTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Cari" -> CariTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Kategori" -> KategoriTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Label" -> LabelTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        "Daftar Tunggu" -> DaftarTungguTwoToneIcon(themeColor = uniformTone, modifier = modifier)
        else -> Icon(Icons.Default.ViewAgenda, contentDescription = null, tint = uniformTone, modifier = modifier)
    }
}
