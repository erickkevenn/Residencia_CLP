package br.com.casainteligente.mini

// Modelo base e capacidades (traits)
sealed interface Dispositivo {
    val nome: String
    val local: String
    var potencia: Double
    var consumoMedio: Double
}

interface Dimmable {
    var luminosidade: Int
    fun ajustarLuminosidade(nivel: Int) {
        luminosidade = nivel.coerceIn(0, 100)
        println("Luminosidade ajustada para ${'$'}luminosidade%")
    }
}

interface Lockable {
    var trancada: Boolean
    fun trancar() { trancada = true; println("Dispositivo trancado") }
    fun destrancar() { trancada = false; println("Dispositivo destrancado") }
}
