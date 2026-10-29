package cl.duoc.syncpass.data

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

object AuthSession {
    var accessToken by mutableStateOf<String?>(null)
        private set
    var participantId by mutableStateOf<String?>(null)
        private set
    var roles by mutableStateOf<List<String>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var syncVersion by mutableStateOf(0)
        private set

    fun signedIn(access: String, idToken: String?) {
        fun claims(token: String?): JSONObject? = token?.split('.')?.getOrNull(1)?.let {
            JSONObject(String(Base64.decode(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)))
        }
        val identity = claims(idToken)
        val authorization = claims(access)
        accessToken = access
        participantId = identity?.optString("sub")?.takeIf { it.isNotBlank() }
            ?: authorization?.optString("sub")?.takeIf { it.isNotBlank() }
        val groups = authorization?.optJSONArray("cognito:groups")
            ?: identity?.optJSONArray("cognito:groups")
        roles = if (groups == null) emptyList() else (0 until groups.length()).mapNotNull { groups.optString(it).takeIf(String::isNotBlank) }
        error = null
    }

    fun fail(message: String) { error = message }
    fun refreshed() { syncVersion++ }
    fun signOut() { accessToken = null; participantId = null; roles = emptyList(); error = null }
}
