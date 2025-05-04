package com.example.finhub.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))  // Dark background like DevBytes
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 5-line Welcome Message
        Text(
            text = "Welcome to FinHub",
            style = TextStyle(
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 18.dp)
        )

        Text(
            text = "Get the latest business headlines",
            style = TextStyle(
                color = Color.LightGray,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "Summarized in just 50 words",
            style = TextStyle(
                color = Color.LightGray,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "Stay ahead with market trends",
            style = TextStyle(
                color = Color.LightGray,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "Personalized for your region",
            style = TextStyle(
                color = Color.LightGray,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Sign In Button with less rounded corners
        Button(
            onClick = { navController.navigate("signin") },
            shape = RoundedCornerShape(12.dp),  // Reduced corner radius
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2CDCBB),
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp)
        ) {
            Text(
                text = "Sign In",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button (Outlined) with less rounded corners
        OutlinedButton(
            onClick = { navController.navigate("signup") },
            shape = RoundedCornerShape(12.dp),  // Reduced corner radius
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF2CDCBB)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp)
                .border(1.dp, Color(0xFF2CDCBB), RoundedCornerShape(12.dp))  // Matching border radius
        ) {
            Text(
                text = "Sign Up",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
