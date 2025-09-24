package br.com.casainteligente.mini.atuacao
import br.com.casainteligente.mini.energia.SensorSimulado
import br.com.casainteligente.mini.sugestoes.Acao
import br.com.casainteligente.mini.sugestoes.TipoAcao

class Casa(
    private val controlaveis: Map<String, SensorSimulado> = emptyMap()
) {
    private val luzes = mutableMapOf<String, Int>()       // id -> nivel (0..100)
    private val janelas = mutableMapOf<String, Int>()     // id -> abertura% (0..100)
    private val portas = mutableMapOf<String, Boolean>()  // id -> trancada
    private var alarmeArmado = false

    fun ligarLuz(comodo: String) = setNivel("luz_teto_$comodo", 100)
    fun desligarLuz(comodo: String) = setNivel("luz_teto_$comodo", 0)
    fun nivelTeto(comodo: String, nivel: Int) = setNivel("luz_teto_$comodo", nivel.coerceIn(0, 100))

    private fun setNivel(id: String, nivel: Int) {
        luzes[id] = nivel
        println("💡 $id = $nivel%")
    }
    private fun setAbertura(id: String, pct: Int) {
        janelas[id] = pct.coerceIn(0, 100)
        println("🪟 $id = ${janelas[id]}%")
    }

    fun armarAlarme() { alarmeArmado = true;  println("🔒 Alarme ARMADO") }
    fun desarmarAlarme() { alarmeArmado = false; println("🔓 Alarme DESARMADO") }

    fun travarPorta(comodo: String) { portas["porta_$comodo"] = true; println("🚪 porta_$comodo = trancada") }
    fun destravarPorta(comodo: String) { portas["porta_$comodo"] = false; println("🚪 porta_$comodo = destrancada") }

    fun aplicar(acao: Acao) {
        when (acao.tipo) {
            TipoAcao.DESLIGAR -> {
                val id = acao.params["id"]
                when {
                    id == null -> Unit
                    id.startsWith("luz_teto_") -> setNivel(id, 0)
                    id.startsWith("janela_") -> setAbertura(id, 0)
                    controlaveis.containsKey(id) -> {
                        controlaveis[id]!!.desligar()
                        println("🔌 $id DESLIGADO (sensor controlável)")
                    }
                    else -> println("⚠️ DESLIGAR não suportado para $id")
                }
            }
            TipoAcao.AJUSTAR_NIVEL -> {
                val id = acao.params["id"]
                val n = acao.params["nivel"]?.toIntOrNull()
                if (id != null && n != null) setNivel(id, n)
            }
            TipoAcao.AGENDAR -> println("🗓️ Agendar: ${acao.params}")
        }
    }
}