package br.com.casainteligente.mini.app

import br.com.casainteligente.mini.energia.*
import br.com.casainteligente.mini.sugestoes.*
import br.com.casainteligente.mini.atuacao.Casa
import br.com.casainteligente.mini.orquestracao.SequenciaEnergiaService
import br.com.casainteligente.mini.notificacao.*
import br.com.casainteligente.mini.analise.*

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    // Sensores simulados
    val s1 = SensorSimulado("Chuveiro", "banheiro", 4500.0, 150.0)
    val s2 = SensorSimulado("Geladeira", "cozinha", 120.0, 20.0)
    val s3 = SensorSimulado("TV_Sala", "sala", 6.0, 1.0)

    val monitor = MonitorEnergia(listOf(s1, s2, s3))
    val prefs = Preferencias(limiteWattsEmPonta = 1000.0)
    val motor = MotorSugestoes(monitor, prefs)

    val casa = Casa(
        mapOf(
            "Chuveiro" to s1,
            "Geladeira" to s2,
            "TV_Sala"  to s3
        )
    )

    // Detector + Notificador
    val detector = AnomalyDetector(alpha = 0.5, desvioThresh = 1.5, offlineMs = 2000)
    val notify: Notificador = run {
        val tg = runCatching { NotificadorTelegram() }.getOrNull()
        if (tg != null) NotificadorComposite(NotificadorConsole(), tg)
        else NotificadorConsole()
    }

    // Mensagem inicial para confirmar funcionamento
    notify.enviar(Alerta("Sistema", "START", "Monitoramento iniciado ✅"))

    // HUD + alertas em tempo real
    val hud = launch {
        monitor.aoVivo().collect { s ->
            val now = java.time.Instant.now().toEpochMilli()

            // 1) Verificar anomalias
            s.porDispositivo.forEach { (id, w) ->
                detector.feed(id, w, now).forEach { notify.enviar(it) }
            }

            // 2) Verificar consumo acima do limite
            if (s.totalWatts > prefs.limiteWattsEmPonta) {
                notify.enviar(
                    Alerta(
                        "Sistema",
                        "LIMITE",
                        "Consumo total ${"%.0f".format(s.totalWatts)} W > limite ${prefs.limiteWattsEmPonta} W"
                    )
                )
            }

            // HUD local
            val top = s.porDispositivo.entries.sortedByDescending { it.value }.take(2)
            val topStr = top.joinToString { it.key + "=" + "%.0f".format(it.value) + "W" }
            println("⚡ Total ${"%.0f".format(s.totalWatts)} W | Top: $topStr")
        }
    }

    // Ações de demonstração
    casa.ligarLuz("sala")
    casa.nivelTeto("sala", 40)
    delay(3000)

    // 3) Sugestões + enviar pelo Telegram
    val sugestoes = motor.gerar()
    sugestoes.forEach { s ->
        val msg = "💡 Sugestão: ${s.titulo} — ${s.descricao} (R$ ${"%.2f".format(s.economiaBRL)})"
        println(msg)
        notify.enviar(Alerta("Sugestao", "INFO", msg))
    }
    sugestoes.firstOrNull { it.acao != null }?.acao?.let { acao ->
        casa.aplicar(acao)
        notify.enviar(Alerta("Ação", "APLICADA", "${acao.tipo} ${acao.params}"))
    }

    // 4) Rodar ciclo do DS e enviar resumo
    val ds = SequenciaEnergiaService(monitor, prefs, casa)
    val acoesDs = ds.rodarCiclo()
    if (acoesDs.isNotEmpty()) {
        val resumo = acoesDs.joinToString { it.tipo.name + it.params.toString() }
        println("✅ DS aplicou ações: $resumo")
        notify.enviar(Alerta("DS", "APLICOU", resumo))
    } else {
        println("✅ DS: nenhuma ação necessária (dentro do limite).")
        notify.enviar(Alerta("DS", "OK", "Nenhuma ação necessária"))
    }

    delay(10_000)
    hud.cancelAndJoin()
}
