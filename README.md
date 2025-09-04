# Smart Home / Residência CLP (Mini) — Kotlin

Simulação e console app didático de automação residencial e monitoramento de energia em Kotlin, combinando:
- CRUD de dispositivos (CLI)
- Consumo e custo (total / por cômodo / por dispositivo)
- Motor de sugestões de economia (pico, standby, dimerização)
- (Opcional / extensível) Fluxos reativos com sensores simulados (coroutines + Flow)

## Sumário
1. Visão Geral
2. Principais Recursos
3. Estrutura de Código
4. Modelagem 
5. Cálculo de Consumo e Custos
6. Motor de Sugestões
7. Execução
8. Exemplo 
9. Personalização Rápida
10. Roadmap

## 1. Visão Geral
O projeto demonstra conceitos de modelagem com sealed interfaces, composição de capacidades (Dimmable, Lockable), cálculo de consumo dinâmico, estimativa de custo e geração de sugestões de economia. Há também uma mini versão reativa (sensores simulados) mostrada em trecho conceitual.

## 2. Principais Recursos
- CRUD de dispositivos em memória (menu interativo)
- Capacidades especializadas:
    - Dimmable (lâmpadas: ajuste de luminosidade)
    - Lockable (portas: trancar / destrancar)
- Cálculo:
    - Consumo instantâneo total
    - Consumo por cômodo
    - Consumo por dispositivo
    - Estimativa de custo hora / dia / mês
- Sugestões de economia:
    - Pico (redução de excedente)
    - Standby (3 a 12 W)
    - Dimerização (redução de brilho)
- Toolchain Java 24 gerando bytecode 21
- Base pronta para expansão com Flow (sensores simulados)

## 3. Estrutura de Código
```
src/main/kotlin/br/com/casainteligente/mini/
├── Casa.kt
├── Dispositivo.kt        # sealed + capacidades (Dimmable, Lockable)
├── Eletrodomestico.kt
├── Energia.kt            # SnapshotEnergia + custos(...)
├── Geladeira.kt
├── Lampada.kt
├── Porta.kt
├── SensorRepository.kt   # CRUD em memória
├── Sugestoes.kt          # MotorSugestoes
└── Main.kt               # CLI (menu)
```

Versão mini reativa (conceito):
```
Casa.kt          # Estado e ações
Energia.kt       # Sensores / Leitura / MonitorEnergia
Sugestoes.kt     # Preferencias / MotorSugestoes
Main.kt          # Orquestração
```

## 4. Modelagem 
- Dispositivo (sealed interface base)
- Lampada : Dispositivo, Dimmable
- Porta : Dispositivo, Lockable
- Geladeira : Dispositivo
- Eletrodomestico : Dispositivo
- Dimmable → luminosidade (%) afeta consumo: potencia * (luminosidade / 100)
- Lockable → estado trancada / destrancada
- SensorRepository → lista mutável (adicionar, atualizar, remover, listar)

## 5. Cálculo de Consumo e Custos
Fórmulas:
- Lâmpada (W) = potencia * (luminosidade / 100.0)
- Demais (W) = potencia nominal
- Total (W) = soma dos watts
- Por cômodo (W) = group by local
- Custo (R$):
    - hora = (totalWatts / 1000) * precoKWh
    - dia = hora * 24
    - mês = dia * 30
Preço padrão: Preferencias.precoKWhBRL (edite conforme tarifa local).

## 6. Motor de Sugestões
Regras (MotorSugestoes):
1. Pico: se totalWatts > limiteWattsEmPonta → sugerir desligar maiores cargas até remover excedente.
2. Standby: dispositivos entre 3 e 12 W → sugerir retirar da tomada (impacto mensal estimado).
3. Dimerização: ids iniciando com luz_ → reduzir brilho (ex.: 100% → 60%).
Cada sugestão inclui impacto (kWh) e economia estimada (R$).

## 7. Execução
Pré-requisitos:
- JDK 24 instalado (toolchain configurada)
- Gradle Wrapper (já incluído)

Compilar e executar (Unix / macOS / WSL):
```
./gradlew clean build
./gradlew run
```
Windows:
```
gradlew.bat clean build
gradlew.bat run
```

Trecho conceitual (sensores simulados com Flow):
```kotlin
val sensores = listOf(
    SensorSimulado("sala.luz", base = 40.0, variacao = 5.0),
    SensorSimulado("cozinha.geladeira", base = 120.0, variacao = 8.0),
    SensorSimulado("quarto.tv", base = 60.0, variacao = 10.0)
)
val monitor = MonitorEnergia(sensores)
val preferencias = Preferencias(limiteWattsTotal = 300.0)
val motor = MotorSugestoes(preferencias)
val casa = Casa()

runBlocking {
    launch {
        monitor.aoVivo().collect { snapshot ->
            val sugestoes = motor.gerar(snapshot)
            sugestoes.firstOrNull()?.let { casa.aplicar(it.acao) }
        }
    }
    delay(8000)
}
```

## 8. Exemplo
Menu:
1. Listar
2. Adicionar
3. Atualizar
4. Remover
5. Ajustar luminosidade (Dimmable)
6. Trancar / Destrancar (Lockable)
7. Mostrar consumo e custo
8. Sugerir economias
0. Sair
(Exibição inclui tabela alinhada, custos estimados e sugestões.)

## 9. Personalização Rápida
- Ajuste Preferencias (limites de watts, preço kWh)
- Adicione novos dispositivos em SensorRepository
- Crie novas regras no MotorSugestoes
- Integre sensores simulados ao CRUD (fusão das abordagens)

## 10. Roadmap
- Persistência (arquivo / SQLite)
- API REST (Ktor ou Spring)
- UI (Compose Desktop/Web)
- Tarifação dinâmica (ponta / fora-ponta)
- Perfis de automação (horários / regras por cômodo)
- Integração contínua com Flow de sensores
- Histórico e gráficos (consumo e custo)

