package br.com.casainteligente.mini

class SensorRepository {
    private val dispositivos = mutableListOf<Dispositivo>()

    fun adicionar(d: Dispositivo) { dispositivos.add(d) }
    fun listar(): List<Dispositivo> = dispositivos.toList()

    fun atualizar(nome: String, novo: Dispositivo): Boolean {
        val idx = dispositivos.indexOfFirst { it.nome == nome }
        return if (idx >= 0) { dispositivos[idx] = novo; true } else false
    }

    fun remover(nome: String): Boolean = dispositivos.removeIf { it.nome == nome }

    fun encontrar(nome: String): Dispositivo? = dispositivos.find { it.nome == nome }
}
