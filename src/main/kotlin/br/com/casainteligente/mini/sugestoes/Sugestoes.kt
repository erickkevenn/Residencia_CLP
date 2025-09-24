package br.com.casainteligente.mini.sugestoes
import br.com.casainteligente.mini.energia.MonitorEnergia
import br.com.casainteligente.mini.energia.SnapshotEnergia
import br.com.casainteligente.mini.energia.LeituraEnergia

data class Preferencias(
    val limiteWattsEmPonta: Double = 1000.0
)

enum class TipoAcao { DESLIGAR, AGENDAR, AJUSTAR_NIVEL }

data class Acao(
    val tipo: TipoAcao,
    val params: Map<String, String> = emptyMap()
)

data class Sugestao(
    val titulo: String,
    val descricao: String,
    val impactoKWh: Double,
    val economiaBRL: Double,
    val acao: Acao? = null
)

class MotorSugestoes(
    private val monitor: MonitorEnergia,
    private val prefs: Preferencias
) {
    private val PRECO_KWH = 1.0 // R$1/kWh (simples para demonstração)

    fun gerar(): List<Sugestao> {
        val snap = monitor.snapshot()
        val out = mutableListOf<Sugestao>()

        // Regra: total acima do limite -> sugerir adiar/redistribuir
        if (snap.totalWatts > prefs.limiteWattsEmPonta) {
            val excedente = snap.totalWatts - prefs.limiteWattsEmPonta
            val top = snap.porDispositivo.entries.sortedByDescending { it.value }.firstOrNull()?.key
            val kWh = excedente / 1000.0 * 1.0 // referência de 1h
            out += Sugestao(
                titulo = "Reduzir consumo agora (acima do limite)",
                descricao = "Adiar/distribuir uso. Excedente ~${"%.0f".format(excedente)} W.",
                impactoKWh = kWh,
                economiaBRL = kWh * PRECO_KWH,
                acao = Acao(TipoAcao.AGENDAR, mapOf("quando" to "fora_de_pico", "alvo" to (top ?: "")))
            )
        }

        // Regra: standby (3..12 W) -> sugerir desligar
        snap.porDispositivo.filter { it.value in 3.0..12.0 }.forEach { (id, w) ->
            val kWhMes = (w / 1000.0) * 24.0 * 30.0
            out += Sugestao(
                titulo = "Desligar '$id' da tomada",
                descricao = "Consumo em standby (~${"%.1f".format(w)} W).",
                impactoKWh = kWhMes,
                economiaBRL = kWhMes * PRECO_KWH,
                acao = Acao(TipoAcao.DESLIGAR, mapOf("id" to id))
            )
        }

        return out
    }
}
