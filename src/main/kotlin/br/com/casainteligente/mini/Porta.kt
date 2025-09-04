package br.com.casainteligente.mini

data class Porta(
    override val nome: String,
    override val local: String,
    override var potencia: Double = 0.0,
    override var consumoMedio: Double = 0.0,
    override var trancada: Boolean = true
) : Dispositivo, Lockable
