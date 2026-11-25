package com.amkumirab.solostudying.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amkumirab.solostudying.ui.viewmodel.ShopViewModel

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.amkumirab.solostudying.data.entity.*
import com.amkumirab.solostudying.ui.theme.*
import kotlinx.coroutines.delay

@Composable
internal fun RewardShopDestination(
    viewModel: ShopViewModel,
    goldBalance: Int,
    onCreateReward: () -> Unit,
) {
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    val balances by viewModel.balances.collectAsStateWithLifecycle()
    ShopTab(
        rewards = rewards,
        balances = balances,
        goldBalance = goldBalance,
        onCreateRewardClicked = onCreateReward,
        onPurchaseReward = viewModel::purchaseReward,
        onUseReward = viewModel::useRewardTime,
        onDeleteReward = viewModel::deleteReward,
    )
}

@Composable
fun ShopTab(
    rewards: List<RewardItemEntity>,
    balances: List<RewardBalanceEntity>,
    goldBalance: Int,
    onCreateRewardClicked: () -> Unit,
    onPurchaseReward: (RewardItemEntity, (Boolean, String) -> Unit) -> Unit,
    onUseReward: (String, Float, (Boolean) -> Unit) -> Unit,
    onDeleteReward: (RewardItemEntity) -> Unit
) {
    var shopToastMessage by remember { mutableStateOf<String?>(null) }
    var shopToastSuccess by remember { mutableStateOf(true) }

    LaunchedEffect(shopToastMessage) {
        if (shopToastMessage != null) {
            delay(3500)
            shopToastMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your rewards",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                )
                Text(
                    text = "Spend your study Gold.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onCreateRewardClicked,
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp).testTag("create_reward_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add reward", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Persistent custom styled Toast message
        AnimatedVisibility(
            visible = shopToastMessage != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            shopToastMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("shop_toast"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (shopToastSuccess) Color(0xFF142E25) else Color(0xFF34171E)
                    ),
                    border = BorderStroke(1.dp, if (shopToastSuccess) RpgEmerald else RpgRuby)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (shopToastSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = "Success check indicator",
                            tint = if (shopToastSuccess) RpgEmerald else RpgRuby
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Unlocked inventory balances (Time-Based)
            item {
                Text(
                    text = "Ready to use",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (balances.isEmpty() || balances.none { it.availableHours > 0f }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "No stored reward assets inside inventory. Complete study duels against bosses, buy rewards with gold below, and they will accumulate here safely!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        balances.filter { it.availableHours > 0f }.forEach { balance ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C30)),
                                border = BorderStroke(1.dp, NeonBlueAccent.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = balance.rewardName.uppercase(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite
                                            )
                                        )
                                        Text(
                                            text = "Available Amount: ${String.format(LocalLocale.current.platformLocale, "%.1f", balance.availableHours)} Hours",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = RpgGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onUseReward(balance.rewardName, 1f) { success ->
                                                if (success) {
                                                    shopToastSuccess = true
                                                    shopToastMessage = "Spent 1 Unit of ${balance.rewardName}! Bask in your reward time guilt-free!"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RpgEmerald),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Spend 1 Unit", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Purchasing shop items
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Reward shop",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(rewards) { reward ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_item_${reward.name.replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Graphic Box representation of Reward
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (reward.rewardType == "Time-Based")
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
                                            radius = 120f
                                        )
                                    else
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF581C87), Color(0xFF0F172A)),
                                            radius = 120f
                                        )
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (reward.rewardType == "Time-Based") NeonBlueAccent else Color(0xFFFF5CE2),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (reward.rewardType == "Time-Based") Icons.Default.HourglassEmpty else Icons.Default.EmojiEvents,
                                contentDescription = "Reward representation icon",
                                tint = if (reward.rewardType == "Time-Based") NeonBlueAccent else Color(0xFFFF5CE2),
                                modifier = Modifier.size(32.dp)
                            )

                            // RPG item quantity/duration badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 12.dp)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (reward.rewardType == "Time-Based") NeonBlueAccent.copy(alpha = 0.5f) else Color(0xFFFF5CE2).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 12.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (reward.rewardType == "Time-Based") "${reward.rewardValue}H" else "1X",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (reward.rewardType == "Time-Based") NeonBlueAccent else Color(0xFFFF5CE2)
                                    )
                                )
                            }
                        }

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reward.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (reward.isCustom) {
                                    IconButton(
                                        onClick = { onDeleteReward(reward) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete reward item",
                                            tint = RpgRuby,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = reward.description,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Price
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Gold coin icon",
                                        tint = RpgGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${reward.cost} GOLD",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = RpgGold
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        onPurchaseReward(reward) { success, msg ->
                                            shopToastSuccess = success
                                            shopToastMessage = msg
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (goldBalance >= reward.cost) RpgGold else Color(0xFF24201A),
                                        contentColor = if (goldBalance >= reward.cost) Color.Black else TextMuted
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("buy_button_${reward.name.replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = "ACQUIRE",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CreateRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("150") }
    var rewardType by remember { mutableStateOf("Time-Based") } // "Time-Based", "One-Time"
    var hoursValue by remember { mutableStateOf("1") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkFantasySurface),
            border = BorderStroke(1.5.dp, DarkCardBorder),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "FORGE GUILD REWARD",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = NeonBlueAccent,
                        letterSpacing = 1.sp
                    )
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reward Name (e.g. Gaming Episode)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reward_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (e.g. Treat is 1 Episode of Anime)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reward_desc_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = costInput,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            costInput = it
                        }
                    },
                    label = { Text("Guild Cost (Gold Coins)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        focusedLabelColor = NeonBlueAccent,
                        focusedIndicatorColor = NeonBlueAccent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("reward_cost_input"),
                    singleLine = true
                )

                Column {
                    Text(text = "REWARD STRUCTURE TYPE", style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Time-Based", "One-Time").forEach { type ->
                            val isSel = rewardType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonBlueAccent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) NeonBlueAccent else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { rewardType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) NeonBlueAccent else TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                if (rewardType == "Time-Based") {
                    OutlinedTextField(
                        value = hoursValue,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                hoursValue = it
                            }
                        },
                        label = { Text("Accumulated Hours Granted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            focusedLabelColor = NeonBlueAccent,
                            focusedIndicatorColor = NeonBlueAccent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("reward_value_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL")
                    }

                    val finalCost = costInput.toIntOrNull() ?: 150
                    val finalVal = hoursValue.toIntOrNull() ?: 1
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && finalCost > 0) {
                                onConfirm(name, desc, finalCost, rewardType, finalVal)
                            }
                        },
                        enabled = name.isNotEmpty() && finalCost > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlueSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("conclude_reward_forge_button")
                    ) {
                        Text("FORGE REWARD", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
