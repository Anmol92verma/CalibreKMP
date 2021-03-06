import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.baseio.kmm.welcome.Language
import com.baseio.kmm.welcome.LanguageDropdownModel
import com.baseio.kmm.welcome.LanguageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import navigation.Navigator
import navigation.WelcomeWizard
import navigation.screen
import theme.CalibreColorProvider
import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser

@Composable
fun WelcomeWizard(window: ComposeWindow) {
    var navigator by remember { mutableStateOf<Navigator?>(null) }

    Column(Modifier.background(CalibreColorProvider.colors.uiBackground)) {
        WWHeadline()
        Navigator(initialScreen = WelcomeWizard.Screen1) {
            navigator = this
            screen(WelcomeWizard.Screen1) {
                WZDLanguageFolderPathSelectionScreen(window)
            }
            screen(WelcomeWizard.Screen2) {
                WZDDeviceModelSelectionScreen(window)
            }
        }
        NavigationButtons(navigator)
    }
}

@Composable
fun ColumnScope.WZDDeviceModelSelectionScreen(window: ComposeWindow) {
    val manufactures by remember { mutableStateOf(ManufacturesDevicesMap.manufacturersMap) }
    var devices by remember { mutableStateOf(manufactures.first().devices) }
    Column(Modifier.weight(1f)) {
        Text(
            "Choose your e-book device, If your device is not in the list, choose a \"Generic\" device.",
            Modifier.padding(12.dp), style = TextStyle(color = CalibreColorProvider.colors.textPrimary)
        )

        Row {
            CommonDeviceChooser(Modifier.weight(1f), "Manufacturers", manufactures.map { it.name }) { nameSelected ->
                devices = manufactures.find { it.name == nameSelected }!!.devices
            }
            CommonDeviceChooser(Modifier.weight(1f), "Devices", devices)
        }

    }
}

@Composable
private fun CommonDeviceChooser(
    modifier: Modifier,
    title: String,
    items: List<String>,
    selectedManufacturer: (String) -> Unit = {}
) {
    Column(modifier.padding(16.dp)) {
        Text(
            title,
            Modifier.padding(vertical = 12.dp), style = TextStyle(color = CalibreColorProvider.colors.textPrimary)
        )
        Surface(
            elevation = 4.dp,
            modifier = Modifier.fillMaxSize(),
            color = CalibreColorProvider.colors.uiBackground, border = BorderStroke(1.dp, color = Color.White)
        ) {
            LazyColumn(Modifier.padding(4.dp)) {
                items(items) { item ->
                    Box(Modifier.clickable {
                        selectedManufacturer(item)
                    }.fillMaxWidth()) {
                        Text(
                            item,
                            style = TextStyle(color = CalibreColorProvider.colors.textPrimary),
                            modifier = Modifier.padding(4.dp)
                        )
                    }

                }
            }
        }
    }
}


@Composable
private fun ColumnScope.WZDLanguageFolderPathSelectionScreen(
    window: ComposeWindow
) {
    Column(Modifier.weight(1f)) {
        Divider(color = CalibreColorProvider.colors.lineColor)
        LanguageDropDown()
        Spacer(Modifier.height(48.dp))
        FileLocationChooser(window)
        Spacer(Modifier.weight(1f))
        Text(
            "If you are moving calibre from an old computer to a new one, please read the instructions.",
            Modifier.padding(24.dp), style = TextStyle(color = CalibreColorProvider.colors.textPrimary)
        )
        Divider(color = CalibreColorProvider.colors.lineColor)

    }
}

@Composable
fun NavigationButtons(navigator: Navigator?) {
    var isBackButtonEnabled by remember { mutableStateOf((navigator?.screenCount ?: 0) > 1) }
    var isNextButtonEnabled by remember { mutableStateOf((navigator?.totalScreens ?: 0) > 1) }

    LaunchedEffect(Unit) {
        launch {
            navigator?.changePublisher?.receiveAsFlow()?.collect {
                isBackButtonEnabled = navigator.screenCount > 1
                isNextButtonEnabled = navigator.totalScreens > 1
            }
        }
    }

    Surface(elevation = 4.dp, color = CalibreColorProvider.colors.uiBackground) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    navigator?.goBack()
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = CalibreColorProvider.colors.buttonColor,
                    disabledBackgroundColor = CalibreColorProvider.colors.buttonColor.copy(alpha = 0.5f)
                ), enabled = isBackButtonEnabled
            ) {
                Text("Back", style = TextStyle(color = CalibreColorProvider.colors.buttonTextColor))
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = {
                    navigator?.navigate(WelcomeWizard.Screen2)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = CalibreColorProvider.colors.buttonColor,
                    disabledBackgroundColor = CalibreColorProvider.colors.buttonColor.copy(alpha = 0.5f)
                ), enabled = isNextButtonEnabled
            ) {
                Text("Next", style = TextStyle(color = CalibreColorProvider.colors.buttonTextColor))
            }
            Spacer(Modifier.width(12.dp))

            Button(
                onClick = {

                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = CalibreColorProvider.colors.buttonColor
                ),
            ) {
                Text("Cancel", style = TextStyle(color = CalibreColorProvider.colors.buttonTextColor))
            }
            Spacer(Modifier.width(12.dp))

        }
    }
}

@Composable
fun FileLocationChooser(window: ComposeWindow) {

    Column(Modifier.padding(24.dp)) {
        Text(
            "Choose a location for your books. When you add books to calibre, they will be copied here. Use an empty folder for a new calibre library:",
            style = TextStyle(color = CalibreColorProvider.colors.textPrimary),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FolderChooserButtons(window)
        Text(
            "If a calibre library already exists at the newly selected location, calibre will use it automatically.",
            style = TextStyle(color = CalibreColorProvider.colors.textPrimary), modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun FolderChooserButtons(
    window: ComposeWindow
) {
    val currentUsersHomeDir = System.getProperty("user.home")
    var otherFolder by remember { mutableStateOf(currentUsersHomeDir + File.separator.toString() + "Calibre Library") }

    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(
            otherFolder,
            style = TextStyle(color = CalibreColorProvider.colors.textPrimary),
            modifier = Modifier.fillMaxWidth(0.6f).border(
                1.dp, CalibreColorProvider.colors.lineColor, shape = RoundedCornerShape(15)
            ).background(
                CalibreColorProvider.colors.uiBackground,
            ).padding(12.dp)
        )

        Spacer(Modifier.width(24.dp))

        Button(
            onClick = {
                val dir = folderPicker(window)
                otherFolder = dir?.absolutePath ?: ""
            }, colors = ButtonDefaults.buttonColors(
                backgroundColor = CalibreColorProvider.colors.buttonColor
            ),
            modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            Text("Change", style = TextStyle(color = CalibreColorProvider.colors.buttonTextColor))
        }
    }
}

private fun folderPicker(window: ComposeWindow) =
    if (System.getProperty("os.name").contains("Mac")) {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        val dialog = FileDialog(window)
        dialog.isVisible = true
        val name = dialog.file
        val dir = dialog.directory
        if (name == null || dir == null) null else File(dialog.directory, dialog.file)
    } else {
        val chooser = JFileChooser()
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "Choose destination"
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val dir = chooser.selectedFile
            dir?.let {
                if (dir.absolutePath.trim { it <= ' ' }.isEmpty()) {
                    null
                } else {
                    dir
                }
            } ?: run {
                null
            }
        } else {
            null
        }
    }

@Composable
fun LanguageDropDown() {
    var isOpen by remember { mutableStateOf(false) }
    var languages by remember { mutableStateOf<LanguageDropdownModel?>(null) }
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(true) {
        withContext(Dispatchers.IO) {
            languages = LanguageLoader.loadLanguagePack()
            selectedIndex = languages?.languages?.indexOfFirst { it.alpha_2 == "en" } ?: 0
        }
    }

    Box(
        modifier = Modifier.padding(24.dp).background(CalibreColorProvider.colors.uiBackground)
    ) {
        Row() {
            Text(
                "Choose your language: ",
                Modifier.align(Alignment.CenterVertically),
                style = TextStyle(color = CalibreColorProvider.colors.textPrimary)
            )
            if (isOpen) {
                LazyColumn {
                    itemsIndexed(languages?.languages ?: emptyList()) { index: Int, it: Language ->
                        DropdownMenuItem(onClick = {
                            selectedIndex = index
                            isOpen = false
                        }) {
                            Text(it.name ?: "", style = TextStyle(color = CalibreColorProvider.colors.textPrimary))
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        isOpen = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = CalibreColorProvider.colors.buttonColor
                    ),
                ) {
                    Text(
                        languages?.languages?.get(selectedIndex)?.name ?: "Not Selected",
                        style = TextStyle(color = CalibreColorProvider.colors.buttonTextColor)
                    )
                }
            }

        }


    }

}

@Composable
fun WWHeadline() {
    TopAppBar(title = {
        Column {
            Text(
                "Welcome to CalibreKMM",
                style = TextStyle(color = CalibreColorProvider.colors.appBarTextTitleColor)
            )
            Text(
                "The one stop solution to all your e-book needs.",
                style = TextStyle(color = CalibreColorProvider.colors.appBarTextSubTitleColor)
            )
        }
    }, actions = {
        Image(
            painter = painterResource("images/library.png"),
            contentDescription = null,
            Modifier.size(64.dp).padding(4.dp)
        )
    }, backgroundColor = CalibreColorProvider.colors.appBarColor)


}
