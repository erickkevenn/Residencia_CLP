package br.com.casainteligente.mini.energia

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.Instant
import kotlin.random.Random

data class LeituraEnergia(
    val timestamp: Instant,
    val deviceId: String,
    val comodo: String?,
    val watts: Double
)

data class SnapshotEnergia(
    val totalWatts: Double,
    val porComodo: Map<String, Double>,
    val porDispositivo: Map<String, Double>
)

interface SensorEnergia {
    val deviceId: String
    fun aoVivo(): Flow<LeituraEnergia>
}

// imports no topo já têm: kotlinx.coroutines.flow.* e delay, etc.
class SensorSimulado(
    override val deviceId: String,
    private val comodo: String,
    private val baseW: Double,
    private val variacao: Double = baseW * 0.1,
    private val intervaloMs: Long = 1000L
) : SensorEnergia {

    // ? estado de ligado/desligado
    private val ligado = MutableStateFlow(true)
    fun ligar() { ligado.value = true }
    fun desligar() { ligado.value = false }

    override fun aoVivo(): Flow<LeituraEnergia> = ligado.flatMapLatest { on ->
        flow {
            while (true) {
                val wBase = (baseW + Random.nextDouble(-variacao, variacao)).coerceAtLeast(0.0)
                val w = if (on) wBase else 0.0
                emit(LeituraEnergia(Instant.now(), deviceId, comodo, w))
                delay(intervaloMs)
            }
        }
    }
}


class MonitorEnergia(private val sensores: List<SensorEnergia>) {
    private val atuais = MutableStateFlow<Map<String, LeituraEnergia>>(emptyMap())

    fun aoVivo(): Flow<SnapshotEnergia> {
        val fluxos = sensores.map { it.aoVivo() }
        return merge(*fluxos.toTypedArray()).map { l ->
            atuais.update { it + (l.deviceId to l) }
            snapshot()
        }
    }

    fun snapshot(): SnapshotEnergia {
        val m = atuais.value
        val porDisp = m.mapValues { it.value.watts }
        val porComodo = m.values.groupBy { it.comodo ?: "desconhecido" }
            .mapValues { (_, vs) -> vs.sumOf { it.watts } }
        return SnapshotEnergia(porDisp.values.sum(), porComodo, porDisp)
    }
}