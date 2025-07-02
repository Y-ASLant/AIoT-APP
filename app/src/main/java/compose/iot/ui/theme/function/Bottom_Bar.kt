package compose.iot.ui.theme.function

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.iot.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

//import android.widget.Toast
//import androidx.compose.ui.platform.LocalContext

@Composable
fun Bottom_button(selected: Int, onSelectedChanged: (Int) -> Unit) {
//    val context = LocalContext.current // 获取当前上下文

    Surface(
        //底部导航圆角
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp
    ) {
        NavigationBar(
            modifier = Modifier.padding(bottom = 0.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            NavigationBarItem(
                selected = selected == 0,
                onClick = {
                    onSelectedChanged(0)
//                    Toast.makeText(context, "切换至首页", Toast.LENGTH_SHORT).show()
                },
                icon = {
                    Icon(
                        painter = painterResource(if (selected == 0) R.drawable.bnbfill else R.drawable.bnbline),
                        contentDescription = "首页",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "首页",
                        fontSize = 12.sp
                    )
                },
                alwaysShowLabel = true
            )
            NavigationBarItem(
                selected = selected == 1,
                onClick = {
                    onSelectedChanged(1)
//                    Toast.makeText(context, "切换至面板", Toast.LENGTH_SHORT).show()
                },
                icon = {
                    Icon(
                        painter = painterResource(if (selected == 1) R.drawable.boardfill else R.drawable.boardline),
                        contentDescription = "面板",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "面板",
                        fontSize = 12.sp
                    )
                },
                alwaysShowLabel = true
            )
            NavigationBarItem(
                selected = selected == 2,
                onClick = {
                    onSelectedChanged(2)
//                    Toast.makeText(context, "切换至关于", Toast.LENGTH_SHORT).show()
                },
                icon = {
                    Icon(
                        painter = painterResource(if (selected == 2) R.drawable.terminalboxfill else R.drawable.terminalboxline),
                        contentDescription = "关于",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "关于",
                        fontSize = 12.sp
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

