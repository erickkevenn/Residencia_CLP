package br.com.casainteligente.mini

data class Preferencias(
    val limiteWattsEmPonta: Double = 1000.0,
    val precoKWhBRL: Double = 0.95 // ajuste conforme a sua tarifa
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
    val acao: Acao?
)

class MotorSugestoes(
    private val preferencias: Preferencias
) {
    private val PRECO = preferencias.precoKWhBRL

    fun gerar(snapshot: SnapshotEnergia): List<Sugestao> {
        val out = mutableListOf<Sugestao>()

        // 1) Se total na ponta exceder limite, sugere desligar maiores consumidores
        if (snapshot.totalWatts > preferencias.limiteWattsEmPonta) {
            val excedenteW = snapshot.totalWatts - preferencias.limiteWattsEmPonta
            val excedenteStr = "%.0f".format(excedenteW)
            val maiores = snapshot.porDispositivo.entries.sortedByDescending { it.value }
            var somado = 0.0
            for ((id, w) in maiores) {
                if (somado >= excedenteW) break
                val kWhHora = w / 1000.0
                val wStr = "%.0f".format(w)

                out += Sugestao(
                    titulo = "Reduzir pico: desligar '$id'",
                    descricao = "Seu consumo está acima do limite em ponta (excedente ~${excedenteStr} W). " +
                            "Desligar '$id' reduz ~${wStr} W.",
                    impactoKWh = kWhHora,
                    economiaBRL = kWhHora * PRECO,
                    acao = Acao(TipoAcao.DESLIGAR, mapOf("id" to id))
                )
                somado += w
            }
        }

        // 2) Standby: entre 3 e 12 W -> sugerir tirar da tomada
        snapshot.porDispositivo
            .filter { it.value in 3.0..12.0 }
            .forEach { (id, w) ->
                val kWhMes = (w / 1000.0) * 24.0 * 30.0
                val wStr = "%.1f".format(w)

                out += Sugestao(
                    titulo = "Standby: retirar '$id' da tomada",
                    descricao = "Consumo em standby (~${wStr} W).",
                    impactoKWh = kWhMes,
                    economiaBRL = kWhMes * PRECO,
                    acao = Acao(TipoAcao.DESLIGAR, mapOf("id" to id))
                )
            }

        // 3) Ajuste de nível de luz: se existir id que comece com 'luz_', sugerir 100% -> 60%
        snapshot.porDispositivo
            .filter { (id, w) -> id.startsWith("luz_") && w > 0.0 }
            .forEach { (id, w) ->
                val w60 = w * 0.6
                val saving = w - w60
                val savingStr = "%.0f".format(saving)
                val kWhHora = saving / 1000.0

                out += Sugestao(
                    titulo = "Ajustar nível de iluminação",
                    descricao = "Diminuir '$id' de 100% para 60% reduz ~${savingStr} W.",
                    impactoKWh = kWhHora,
                    economiaBRL = kWhHora * PRECO,
                    acao = Acao(TipoAcao.AJUSTAR_NIVEL, mapOf("id" to id, "nivel" to "60"))
                )
            }

        return out
    }
}
