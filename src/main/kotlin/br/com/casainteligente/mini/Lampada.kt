package br.com.casainteligente.mini

data class Lampada(
    override val nome: String,
    override val local: String,
    override var potencia: Double,
    override var consumoMedio: Double,
    override var luminosidade: Int = 100
) : Dispositivo, Dimmable
