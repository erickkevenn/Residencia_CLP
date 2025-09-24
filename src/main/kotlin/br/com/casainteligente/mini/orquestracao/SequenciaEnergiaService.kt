package br.com.casainteligente.mini.orquestracao
import br.com.casainteligente.mini.energia.MonitorEnergia
import br.com.casainteligente.mini.sugestoes.Preferencias
import br.com.casainteligente.mini.atuacao.Casa
import br.com.casainteligente.mini.sugestoes.Acao
import br.com.casainteligente.mini.sugestoes.TipoAcao
import kotlinx.coroutines.coroutineScope

import kotlinx.coroutines.*

/**
 * Orquestrador que implementa o DS:
 * - coleta snapshot + limite
 * - estima demanda (heurística simples)
 * - decide e aplica ações na Casa
 * - **preserva pelo menos 1 dispositivo ligado** (ex.: Geladeira)
 */
class SequenciaEnergiaService(
    private val monitor: MonitorEnergia,
    private val prefs: Preferencias,
    private val casa: Casa,
    /**
     * IDs que nunca devem ser desligados (essenciais).
     * Ex.: "Geladeira". Ajuste conforme seus sensores/atuadores.
     */
    private val preservarIds: Set<String> = setOf("Geladeira")
) {
    suspend fun rodarCiclo(): List<Acao> = coroutineScope {
        val snap = monitor.snapshot()
        val limite = prefs.limiteWattsEmPonta
        val demandaPrevista = snap.totalWatts * 1.15 // heurística: +15%

        val acoes = mutableListOf<Acao>()
        if (demandaPrevista <= limite) {
            println("📝 DS — dentro do limite, nada a fazer.")
            return@coroutineScope acoes
        }
        val ordenados = snap.porDispositivo.entries.sortedByDescending { it.value }
        val todosIds = ordenados.map { it.key }
        val candidatosSemEssenciais = todosIds.filterNot { it in preservarIds }

        val desligaveis = if (candidatosSemEssenciais.size > 1) {
            candidatosSemEssenciais.dropLast(1)
        } else {
            emptyList()
        }

        val preservadoAutomatico = (todosIds - desligaveis.toSet()).firstOrNull()
        val preservadosInfo = (preservarIds + listOfNotNull(preservadoAutomatico)).distinct()

        var alvo = snap.totalWatts * 0.8
        var atual = snap.totalWatts

        for (id in desligaveis) {
            if (atual <= alvo) break

            val acao = if (id.startsWith("luz_teto_")) {
                Acao(TipoAcao.AJUSTAR_NIVEL, mapOf("id" to id, "nivel" to "0"))
            } else {
                Acao(TipoAcao.DESLIGAR, mapOf("id" to id))
            }

            acoes += acao
            casa.aplicar(acao)
            atual *= 0.9 // aproximação: cada ação reduz ~10% do total
        }

        println(
            "📝 DS — total=${"%.1f".format(snap.totalWatts)}W, " +
                    "limite=${"%.1f".format(limite)}W, prev=${"%.1f".format(demandaPrevista)}W, " +
                    "preservados=$preservadosInfo, ações=${acoes.size}"
        )
        acoes
    }
}
