# Smart Home (a partir de diagrama UML) — Kotlin Mini

Simulação simplificada de automação residencial em Kotlin usando coroutines e Flow para monitoramento de energia em tempo real, geração de sugestões e aplicação de ações na "Casa".

## Principais Recursos
- Monitoramento contínuo de consumo (total, por cômodo, por dispositivo)
- Sensores simulados com ruído/variação configurável
- Motor de sugestões baseado em preferências do usuário
- Ações aplicáveis: desligar dispositivos, ajustar nível (ex: iluminação)
- Arquitetura reativa (Flow) com composição de múltiplos sensores

## Estrutura de Código
```
src/main/kotlin/br/com/casainteligente/mini/
 ├── Casa.kt          # Estado e operações sobre dispositivos
 ├── Energia.kt       # Sensores, leituras, snapshot e monitor
 ├── Sugestoes.kt     # Preferências, ações, sugestões e motor
 └── Main.kt          # Orquestração / exemplo de uso
```

Resumo das entidades:
- Casa: controla luzes, janelas, portas, alarme; aplica Acao sugerida.
- SensorEnergia: interface Flow<LeituraEnergia>.
- SensorSimulado: gera leituras sintéticas.
- MonitorEnergia: agrega leituras e produz SnapshotEnergia.
- MotorSugestoes: avalia snapshot + Preferencias => lista de Sugestao.
- Acao / TipoAcao: modelam a execução (DESLIGAR, AJUSTAR_NIVEL etc.).

## Fluxo de Execução
1. Sensores simulados geram LeituraEnergia continuamente.
2. MonitorEnergia agrega e emite SnapshotEnergia via Flow.
3. MotorSugestoes analisa snapshot e emite Sugestao(s).
4. Casa.aplicar(acao) efetua mudança de estado.
5. (Opcional) Log/console exibe evolução e ações tomadas.

## Exemplo Simplificado (conceito)
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

## Requisitos
- JDK 17+
- Gradle Wrapper (já incluído)

## Como Executar
macOS / Linux / WSL:
```
./gradlew run
```
Windows:
```
gradlew.bat run
```
A saída exibirá leituras agregadas e uma sugestão aplicada após alguns ciclos.

## Personalização Rápida
- Ajuste limites em Preferencias (ex: watts totais).
- Adicione novos sensores modificando a lista em Main.kt.
- Crie novas regras de sugestão estendendo MotorSugestoes.

## Ideias de Extensão
- Persistência histórica (ex: banco em memória / arquivo)
- API HTTP (Ktor ou Spring) para expor snapshots
- UI (compose desktop ou web)
- Regras mais ricas (horário, tarifa dinâmica)
