package cl.duoc.syncpass.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Construye un JWT minimo (solo el payload importa) para alimentar
 * [AuthSession.signedIn] en los tests, sin depender de un login real contra
 * Cognito.
 */
fun fakeJwt(sub: String, roles: List<String> = emptyList()): String {
    val payload = JSONObject().apply {
        put("sub", sub)
        put("cognito:groups", JSONArray(roles))
    }.toString()
    val encoded = Base64.encodeToString(
        payload.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
    return "header.$encoded.signature"
}
