@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.afrc.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.io.File

private val Green = Color(0xFF18D66B)
private val Dark = Color(0xFF07130E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AFRCApp() }
    }
}

data class Chat(val id: String, val name: String, val preview: String)

data class Message(val id: String, val text: String, val mine: Boolean)

@Composable
fun AFRCApp() {
    var loggedIn by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(true) }
    if (!loggedIn) LoginScreen(onLoggedIn = { loggedIn = true })
    else MainShell(dark = dark, onDarkChanged = { dark = it })
}

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Dark).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("AFRC CHAT", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("Messagerie privée • Sans appels", color = Green, modifier = Modifier.padding(bottom = 28.dp))
        if (register) OutlinedTextField(username, { username = it }, label = { Text("Nom d'utilisateur") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Button(onClick = onLoggedIn, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Dark)) {
            Text(if (register) "Créer mon compte" else "Connexion")
        }
        TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (register) "J'ai déjà un compte" else "Créer un compte", color = Color.White)
        }
        Text("Email + mot de passe • Aucun numéro de téléphone requis", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun MainShell(dark: Boolean, onDarkChanged: (Boolean) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val chats = remember { mutableStateListOf(Chat("1", "Bienvenue", "Ton espace AFRC CHAT est prêt"), Chat("2", "Groupe AFRC", "Discussions de groupe")) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Green, background = Dark) else lightColorScheme(primary = Color(0xFF087A3B))) {
        Scaffold(topBar = { TopAppBar(title = { Text(listOf("Discussions", "Stories", "Contacts", "Paramètres")[tab]) }) }, bottomBar = {
            NavigationBar {
                listOf("💬", "📖", "👥", "⚙️").forEachIndexed { i, icon -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(icon) }, label = { Text(listOf("Chats", "Stories", "Contacts", "Réglages")[i]) }) }
            }
        }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (tab) {
                    0 -> ChatList(chats)
                    1 -> Stories()
                    2 -> Contacts()
                    3 -> Settings(dark, onDarkChanged)
                }
            }
        }
    }
}

@Composable
fun ChatList(chats: List<Chat>) {
    var selected by remember { mutableStateOf<Chat?>(null) }
    if (selected != null) ChatScreen(selected!!) else LazyColumn(Modifier.fillMaxSize()) {
        item { Text("Rechercher une discussion", modifier = Modifier.padding(18.dp), color = Color.Gray) }
        items(chats) { chat -> ListItem(headlineContent = { Text(chat.name) }, supportingContent = { Text(chat.preview) }, leadingContent = { Box(Modifier.size(48.dp).background(Green, CircleShape), contentAlignment = Alignment.Center) { Text(chat.name.take(1), color = Dark, fontWeight = FontWeight.Bold) } }, modifier = Modifier.clickable { selected = chat }) }
    }
}

@Composable
fun ChatScreen(chat: Chat) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(Message("1", "Bienvenue dans ${chat.name} 👋", false)) }
    var recording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) recording = true }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(12.dp)) { items(messages) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(18.dp), color = if (m.mine) Green else Color.LightGray, modifier = Modifier.padding(4.dp)) { Text(m.text, Modifier.padding(12.dp), color = if (m.mine) Dark else Color.Black) } } } }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, placeholder = { Text("Message…") }, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                if (text.isNotBlank()) { messages.add(Message(System.currentTimeMillis().toString(), text, true)); text = "" }
            }) { Text("➤") }
            TextButton(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = true
                else audioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }) { Text(if (recording) "⏺" else "🎤") }
        }
        if (recording) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { recording = false }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Arrêter le vocal", color = Dark) }
            }
        }
    }
}

@Composable fun Stories() { LazyColumn(Modifier.fillMaxSize()) { item { Text("Stories 24 h", Modifier.padding(20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold) }; item { Text("Aucune story pour le moment. Ajoute une photo ou une vidéo.", Modifier.padding(20.dp), color = Color.Gray) } } }
@Composable fun Contacts() { LazyColumn(Modifier.fillMaxSize()) { item { Text("Contacts", Modifier.padding(20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold) }; item { Text("Recherche d'utilisateurs, création de groupes et invitations.", Modifier.padding(20.dp), color = Color.Gray) } } }
@Composable fun Settings(dark: Boolean, onDarkChanged: (Boolean) -> Unit) { LazyColumn(Modifier.fillMaxSize()) { item { Text("Mon profil", Modifier.padding(20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold) }; item { ListItem(headlineContent = { Text("@utilisateur") }, supportingContent = { Text("Modifier mon profil") }) }; item { ListItem(headlineContent = { Text("Mode sombre") }, trailingContent = { Switch(dark, onDarkChanged) }) }; item { ListItem(headlineContent = { Text("Confidentialité") }, supportingContent = { Text("En ligne, dernière connexion, blocages") }) }; item { ListItem(headlineContent = { Text("Sécurité") }, supportingContent = { Text("Mot de passe, sessions, signalements") }) }; item { ListItem(headlineContent = { Text("À propos") }, supportingContent = { Text("AFRC CHAT 2.0.0 • Sans appels") }) } } }

// Socket.IO client helper. No call events are defined or emitted anywhere in this project.
object Realtime {
    private var socket: Socket? = null
    fun connect(token: String) { socket = IO.socket(BuildConfig.API_BASE_URL, IO.Options().apply { query = "token=$token" }); socket?.connect() }
    fun sendMessage(conversationId: String, text: String) { socket?.emit("message:send", JSONObject().put("conversationId", conversationId).put("text", text)) }
    fun disconnect() { socket?.disconnect() }
}
