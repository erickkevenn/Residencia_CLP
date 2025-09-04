package br.com.casainteligente.mini

private fun lerLinha(prompt: String): String { print(prompt); return readLine() ?: "" }
private fun lerDouble(prompt: String): Double = lerLinha(prompt).toDoubleOrNull() ?: 0.0

private fun tipo(d: Dispositivo) = when (d) {
    is Lampada -> "LÂMPADA"
    is Geladeira -> "GELADEIRA"
    is Porta -> "PORTA"
    is Eletrodomestico -> "ELETRO"
    else -> "OUTRO"
}

private fun consumoAtualW(d: Dispositivo): Double =
    if (d is Lampada) d.potencia * (d.luminosidade / 100.0) else d.potencia

private fun imprimirLinha(d: Dispositivo) {
    val t = tipo(d)
    val atual = consumoAtualW(d)
    val extras = when (d) {
        is Lampada -> "brilho ${d.luminosidade}%"
        is Porta   -> if (d.trancada) "TRANCADA" else "DESTRANCADA"
        else       -> ""
    }
    println("%-10s | %-16s | %-12s | %7.1f W | %7.1f W | %s"
        .format(t, d.nome, d.local, d.potencia, atual, extras))
}

private fun listarBonito(repo: SensorRepository) {
    val itens = repo.listar()
    if (itens.isEmpty()) {
        println("Nenhum dispositivo cadastrado.")
        return
    }
    println("%-10s | %-16s | %-12s | %7s | %7s | %s"
        .format("Tipo", "Nome", "Cômodo", "Potência", "Atual", "Extras"))
    println("-".repeat(10 + 1 + 1 + 16 + 3 + 12 + 3 + 9 + 3 + 7 + 3 + 20))
    itens.forEach { imprimirLinha(it) }
}

private fun snapshotFromRepo(repo: SensorRepository): SnapshotEnergia {
    val lista = repo.listar()
    val porDispositivo = lista.associate { d ->
        val watts = when (d) {
            is Lampada -> d.potencia * (d.luminosidade / 100.0)
            else -> d.potencia
        }
        d.nome to watts
    }
    val porComodo = lista.groupBy { it.local }.mapValues { (_, ds) ->
        ds.sumOf { if (it is Lampada) it.potencia * (it.luminosidade / 100.0) else it.potencia }
    }
    val total = porDispositivo.values.sum()
    return SnapshotEnergia(total, porComodo, porDispositivo)
}

fun main() {
    val repo = SensorRepository()
    val prefs = Preferencias(limiteWattsEmPonta = 1000.0, precoKWhBRL = 0.95)

    // Dispositivos iniciais
    repo.adicionar(Eletrodomestico("Chuveiro", "banheiro", 4500.0, 150.0))
    repo.adicionar(Geladeira("Geladeira", "cozinha", 120.0, 20.0))
    repo.adicionar(Eletrodomestico("TV_Sala", "sala", 120.0, 30.0))
    repo.adicionar(Lampada("luz_sala", "sala", 10.0, 2.0, 100))
    repo.adicionar(Porta("porta_entrada", "sala", trancada = true))

    loop@ while (true) {
        println("\n--- MENU DISPOSITIVOS ---")
        println("1. Listar")
        println("2. Adicionar")
        println("3. Atualizar (recria pelo nome)")
        println("4. Remover")
        println("5. Ajustar luminosidade (somente Dimmable)")
        println("6. Trancar/Destrancar (somente Lockable)")
        println("7. Mostrar consumo e custo (total/por cômodo)")
        println("8. Sugerir economias")
        println("0. Sair")

        when (lerLinha("Escolha: ").toIntOrNull()) {
            1 -> listarBonito(repo)
            2 -> {
                println("Tipos: 1-Lampada, 2-Geladeira, 3-Porta, 4-Eletrodomestico")
                when (lerLinha("Tipo: ")) {
                    "1" -> {
                        val nome = lerLinha("Nome: ")
                        val local = lerLinha("Local: ")
                        val pot = lerDouble("Potência: ")
                        val cons = lerDouble("Consumo médio: ")
                        val lum = lerLinha("Luminosidade (0..100) [opcional]: ").toIntOrNull() ?: 100
                        repo.adicionar(Lampada(nome, local, pot, cons, lum))
                    }
                    "2" -> {
                        val nome = lerLinha("Nome: ")
                        val local = lerLinha("Local: ")
                        val pot = lerDouble("Potência: ")
                        val cons = lerDouble("Consumo médio: ")
                        repo.adicionar(Geladeira(nome, local, pot, cons))
                    }
                    "3" -> {
                        val nome = lerLinha("Nome: ")
                        val local = lerLinha("Local: ")
                        val tranc = lerLinha("Trancada? (s/n): ").lowercase() == "s"
                        repo.adicionar(Porta(nome, local, trancada = tranc))
                    }
                    "4" -> {
                        val nome = lerLinha("Nome: ")
                        val local = lerLinha("Local: ")
                        val pot = lerDouble("Potência: ")
                        val cons = lerDouble("Consumo médio: ")
                        repo.adicionar(Eletrodomestico(nome, local, pot, cons))
                    }
                    else -> println("Tipo inválido.")
                }
            }
            3 -> {
                val nome = lerLinha("Nome do dispositivo a atualizar (será recriado): ")
                val antigo = repo.encontrar(nome)
                if (antigo == null) {
                    println("Não encontrado.")
                } else {
                    repo.remover(nome)
                    println("Tipos: 1-Lampada, 2-Geladeira, 3-Porta, 4-Eletrodomestico")
                    when (lerLinha("Novo tipo: ")) {
                        "1" -> {
                            val local = lerLinha("Local: ")
                            val pot = lerDouble("Potência: ")
                            val cons = lerDouble("Consumo médio: ")
                            val lum = lerLinha("Luminosidade (0..100) [opcional]: ").toIntOrNull() ?: 100
                            repo.adicionar(Lampada(nome, local, pot, cons, lum))
                        }
                        "2" -> {
                            val local = lerLinha("Local: ")
                            val pot = lerDouble("Potência: ")
                            val cons = lerDouble("Consumo médio: ")
                            repo.adicionar(Geladeira(nome, local, pot, cons))
                        }
                        "3" -> {
                            val local = lerLinha("Local: ")
                            val tranc = lerLinha("Trancada? (s/n): ").lowercase() == "s"
                            repo.adicionar(Porta(nome, local, trancada = tranc))
                        }
                        "4" -> {
                            val local = lerLinha("Local: ")
                            val pot = lerDouble("Potência: ")
                            val cons = lerDouble("Consumo médio: ")
                            repo.adicionar(Eletrodomestico(nome, local, pot, cons))
                        }
                        else -> println("Tipo inválido.")
                    }
                }
            }
            4 -> {
                val nome = lerLinha("Nome do dispositivo a remover: ")
                if (!repo.remover(nome)) println("Não encontrado.")
            }
            5 -> {
                val nome = lerLinha("Nome do dispositivo (Dimmable): ")
                val d = repo.encontrar(nome)
                if (d is Dimmable) {
                    val lvl = lerLinha("Nível (0..100): ").toIntOrNull() ?: 100
                    d.ajustarLuminosidade(lvl)
                } else println("Esse dispositivo não tem luminosidade.")
            }
            6 -> {
                val nome = lerLinha("Nome do dispositivo (Lockable): ")
                val d = repo.encontrar(nome)
                if (d is Lockable) {
                    when (lerLinha("1-Trancar, 2-Destrancar: ")) {
                        "1" -> d.trancar()
                        "2" -> d.destrancar()
                        else -> println("Opção inválida.")
                    }
                } else println("Esse dispositivo não é trancável.")
            }
            7 -> {
                val snapshot = snapshotFromRepo(repo)
                val custos = snapshot.custos(prefs.precoKWhBRL)
                println("\n--- Consumo Atual ---")
                println("Total: %.0f W".format(snapshot.totalWatts))
                println("Por cômodo:")
                snapshot.porComodo.forEach { (c, w) -> println(" - $c: %.0f W".format(w)) }
                println("\n--- Custo Estimado ---")
                println("Hora: R$ %.2f | Dia: R$ %.2f | Mês: R$ %.2f"
                    .format(custos.custoHoraBRL, custos.custoDiaBRL, custos.custoMesBRL))
            }
            8 -> {
                val snapshot = snapshotFromRepo(repo)
                val motor = MotorSugestoes(prefs)
                val sugs = motor.gerar(snapshot)
                if (sugs.isEmpty()) {
                    println("Nenhuma sugestão agora. Tudo otimizado! ✅")
                } else {
                    println("\n--- Sugestões de Economia ---")
                    sugs.forEachIndexed { i, s ->
                        println("${i + 1}. ${s.titulo}")
                        println("   ${s.descricao}")
                        println("   Impacto: %.3f kWh | Economia: R$ %.2f"
                            .format(s.impactoKWh, s.economiaBRL))
                        s.acao?.let { println("   Ação: ${it.tipo} ${it.params}") }
                    }
                }
            }
            0 -> break@loop
            else -> println("Opção inválida.")
        }
    }
}
