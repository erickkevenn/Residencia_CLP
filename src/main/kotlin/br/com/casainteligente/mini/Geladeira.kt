package br.com.casainteligente.mini

data class Geladeira(
    override val nome: String,
    override val local: String,
    override var potencia: Double,
    override var consumoMedio: Double
) : Dispositivo
