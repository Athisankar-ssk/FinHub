package com.example.finhub.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finhub.ui.screens.home.DevBytesTheme
import com.example.finhub.ui.theme.*;


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationDialog(
    isVisible: Boolean,
    title: String,
    confirmButtonText: String,
    cancelButtonText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    containerColor: Color = BottomCard,
    titleColor: Color =  CardPurple,
    confirmButtonBorderColor: Color = CardPurple,
    confirmButtonTextColor: Color = CardPurple,
    cancelButtonContainerColor: Color = Follow,
    cancelButtonTextColor: Color = Color.White,
    cancelButtonBorderColor: Color = Follow
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onCancel,
            containerColor = containerColor,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = confirmButtonTextColor
                        ),
                        border = BorderStroke(1.dp, confirmButtonBorderColor),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmButtonText, color = confirmButtonTextColor)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cancelButtonContainerColor,
                            contentColor = cancelButtonTextColor
                        ),
                        border = BorderStroke(1.dp, cancelButtonBorderColor),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(cancelButtonText, color = cancelButtonTextColor)
                    }
                }
            }
        }
    }
}