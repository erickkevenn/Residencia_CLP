package br.com.casainteligente.mini

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    // Sensores simulados (poucos e simples)
    val s1 = SensorSimulado("Chuveiro", "banheiro", 4500.0, 150.0)
    val s2 = SensorSimulado("Geladeira", "cozinha", 120.0, 20.0)
    val s3 = SensorSimulado("TV_Sala", "sala", 6.0, 1.0)

    val monitor = MonitorEnergia(listOf(s1, s2, s3))
    val prefs = Preferencias(limiteWattsEmPonta = 1000.0)
    val motor = MotorSugestoes(monitor, prefs)
    val casa = Casa()

    // HUD simplificado
    val hud = launch {
        monitor.aoVivo().collect { s ->
            val top = s.porDispositivo.entries.sortedByDescending { it.value }.take(2)
            println("⚡ Total ${"%.0f".format(s.totalWatts)} W | Top: " +
                top.joinToString { it.key + "=" + "%.0f".format(it.value) + "W" })
        }
    }

    // Ações de demonstração
    casa.ligarLuz("sala")
    casa.nivelTeto("sala", 40)
    casa.fecharJanela("quarto")

    delay(3000)

    // Gerar e aplicar primeira sugestão (se houver)
    val sugestoes = motor.gerar()
    sugestoes.forEach { println("🔔 ${it.titulo} — ${it.descricao} (R$ ${"%.2f".format(it.economiaBRL)})") }
    sugestoes.firstOrNull { it.acao != null }?.acao?.let { acao ->
        println("Aplicando ação: ${acao.tipo} ${acao.params}")
        casa.aplicar(acao)
    }

    delay(10_000)
    hud.cancelAndJoin()
}
