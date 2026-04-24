package com.caltrack.app.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NetworkBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isOffline by produceState(initialValue = false) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        value = cm.activeNetwork?.let {
            cm.getNetworkCapabilities(it)?.hasCapability(NET_CAPABILITY_INTERNET)
        } != true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { value = false }
            override fun onLost(network: Network) { value = true }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitDispose { cm.unregisterNetworkCallback(callback) }
    }

    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFF5252))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No internet connection",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
