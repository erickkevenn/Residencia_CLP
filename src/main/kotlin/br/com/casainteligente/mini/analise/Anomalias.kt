package br.com.casainteligente.mini.analise

import kotlin.math.abs
import kotlin.math.sqrt

data class Alerta(val deviceId: String, val tipo: String, val detalhe: String)

class AnomalyDetector(
    private val alpha: Double = 0.2,
    private val desvioThresh: Double = 3.0,
    private val offlineMs: Long = 5000
) {
    private data class State(var ewma: Double = 0.0, var ewvar: Double = 0.0, var lastTs: Long = 0)
    private val st = mutableMapOf<String, State>()

    fun feed(deviceId: String, watts: Double, tsMillis: Long): List<Alerta> {
        val s = st.getOrPut(deviceId) { State(watts, 0.0, tsMillis) }
        val alerts = mutableListOf<Alerta>()

        if (s.lastTs > 0 && tsMillis - s.lastTs > offlineMs) {
            alerts += Alerta(deviceId, "OFFLINE", "Sem leitura há ${(tsMillis - s.lastTs)} ms")
        }
        s.lastTs = tsMillis

        val ePrev = s.ewma
        val e = (1 - alpha) * s.ewma + alpha * watts
        val v = (1 - alpha) * s.ewvar + alpha * (watts - ePrev) * (watts - ePrev)
        s.ewma = e
        s.ewvar = v

        val sigma = sqrt(v.coerceAtLeast(1e-6))
        val z = if (sigma > 0) abs(watts - e) / sigma else 0.0
        if (z >= desvioThresh) {
            val dir = if (watts > e) "ALTO" else "BAIXO"
            alerts += Alerta(deviceId, "DESVIO_$dir", "z=${"%.1f".format(z)} (w=${"%.0f".format(watts)}, μ=${"%.0f".format(e)})")
        }
        return alerts
    }
}
