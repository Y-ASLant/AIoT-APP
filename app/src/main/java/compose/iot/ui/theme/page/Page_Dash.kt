package compose.iot.ui.theme.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import compose.iot.R
//import compose.iot.ui.theme.function.ScaffoldDemo

@Composable
fun Page_Dash(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "连接设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.fluentiot24regular),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text("配置MQTT服务")
                }

                Button(
                    onClick = { navController.navigate("homeassistant") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.homeassistant),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text("配置Home Assistant服务")
                }
                
                Button(
                    onClick = { navController.navigate("video_stream") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.videocam),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text("配置视频流服务")
                }
            }
        }
    }
}

