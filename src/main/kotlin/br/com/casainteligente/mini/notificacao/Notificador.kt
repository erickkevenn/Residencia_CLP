package br.com.casainteligente.mini.notificacao

import br.com.casainteligente.mini.analise.Alerta
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

interface Notificador { fun enviar(alerta: Alerta) }

class NotificadorConsole : Notificador {
    override fun enviar(alerta: Alerta) {
        println("🚨 [${alerta.tipo}] ${alerta.deviceId} — ${alerta.detalhe}")
    }
}

class NotificadorTelegram(
    private val token: String = System.getenv("TELEGRAM_BOT_TOKEN")
        ?: System.getProperty("telegram.bot.token")
        ?: error("TELEGRAM_BOT_TOKEN ou -Dtelegram.bot.token não configurado"),
    private val chatId: String = System.getenv("TELEGRAM_CHAT_ID")
        ?: System.getProperty("telegram.chat.id")
        ?: error("TELEGRAM_CHAT_ID ou -Dtelegram.chat.id não configurado"),
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
) : Notificador {
    override fun enviar(alerta: Alerta) {
        val text = buildString {
            append("🚨 *${alerta.tipo}*\n")
            append("*Dispositivo:* `${alerta.deviceId}`\n")
            append("*Detalhe:* ${alerta.detalhe}")
        }.take(4096)
        val url = "https://api.telegram.org/bot$token/sendMessage" +
                  "?chat_id=$chatId&parse_mode=Markdown&text=" +
                  URLEncoder.encode(text, StandardCharsets.UTF_8)
        val req = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build()
        try {
            val res = client.send(req, HttpResponse.BodyHandlers.ofString())
            if (res.statusCode() !in 200..299) {
                System.err.println("Telegram erro ${res.statusCode()}: ${res.body()}")
            }
        } catch (e: Exception) {
            System.err.println("Falha ao enviar Telegram: ${e.message}")
        }
    }
}

class NotificadorComposite(private vararg val alvos: Notificador) : Notificador {
    override fun enviar(alerta: Alerta) = alvos.forEach { it.enviar(alerta) }
}
