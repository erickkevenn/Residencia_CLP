# Smart Home — Kotlin

Simulação simplificada de automação residencial em Kotlin usando coroutines e Flow para:

- Monitoramento de energia em tempo real
- Geração de sugestões inteligentes
- Aplicação de ações na "Casa"
- Detecção de anomalias
- Notificações automáticas via Telegram

## Principais Recursos

- Monitoramento contínuo de consumo (total, por cômodo, por dispositivo)
- Sensores simulados com ruído/variação configurável
- Motor de Sugestões baseado em preferências do usuário
- Sequência de Energia (DS): orquestra desligamentos preservando essenciais
- AnomalyDetector: identifica desvios de padrão ou sensores offline
- Notificações via Telegram (alertas de consumo, anomalias, ações aplicadas)
- Ações aplicáveis: desligar dispositivos, ajustar nível (ex: iluminação)
- Arquitetura reativa (Flow) com composição de múltiplos sensores

## Estrutura de Código

```
src/main/kotlin/br/com/casainteligente/mini/
 ├── app/
 │   └── Main.kt                # Orquestração / exemplo de uso
 ├── energia/
 │   └── Energia.kt             # Sensores, leituras, snapshot e monitor
 ├── sugestoes/
 │   └── Sugestoes.kt           # Preferências, ações, sugestões e motor
 ├── atuacao/
 │   └── Casa.kt                # Estado e operações sobre dispositivos
 ├── orquestracao/
 │   └── SequenciaEnergiaService.kt # Fluxo do DS
 ├── analise/
 │   └── Anomalias.kt           # Detector de anomalias (EWMA + thresholds)
 └── notificacao/
    └── Notificador.kt         # Console / Telegram / Composite
```

## Resumo das entidades

- **Casa**: controla luzes, janelas, portas, alarme; aplica Acao.
- **SensorEnergia**: interface Flow<LeituraEnergia>.
- **SensorSimulado**: gera leituras sintéticas.
- **MonitorEnergia**: agrega leituras e produz SnapshotEnergia.
- **MotorSugestoes**: avalia snapshot + Preferencias → lista de Sugestao.
- **SequenciaEnergiaService (DS)**: decide desligamentos preservando essenciais.
- **AnomalyDetector**: detecta consumo fora do padrão ou sensores offline.
- **NotificadorTelegram/Console**: envia mensagens no console e/ou Telegram.

## Fluxo de Execução

1. Sensores simulados geram LeituraEnergia continuamente.
2. MonitorEnergia agrega e emite SnapshotEnergia via Flow.
3. MotorSugestoes analisa snapshot e gera Sugestao(s).
4. Casa.aplicar(acao) efetua mudança de estado.
5. SequenciaEnergiaService orquestra desligamentos quando limite é ultrapassado.
6. AnomalyDetector verifica desvios (alto/baixo) e sensores offline.
7. Notificador envia alertas ao console e ao Telegram.

## Exemplo Simplificado

```kotlin
val sensores = listOf(
    SensorSimulado("sala.luz", base = 40.0, variacao = 5.0),
    SensorSimulado("cozinha.geladeira", base = 120.0, variacao = 8.0),
    SensorSimulado("quarto.tv", base = 60.0, variacao = 10.0)
)

val monitor = MonitorEnergia(sensores)
val prefs = Preferencias(limiteWattsTotal = 300.0)
val motor = MotorSugestoes(monitor, prefs)
val casa = Casa(mapOf("sala.luz" to sensores[0]))

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

- JDK 21 (configurado no Gradle Toolchain)
- Gradle Wrapper (já incluído)

## Como Executar

**macOS / Linux / WSL:**

```
./gradlew run
```

**Windows:**

```
gradlew.bat run
```

## Configuração do Telegram

1. Crie um bot via @BotFather e copie o token.
2. Descubra seu chat_id acessando:
   https://api.telegram.org/bot<SEU_TOKEN>/getUpdates

Configure no IntelliJ (Run Configurations → Environment variables) ou no `build.gradle.kts`:

```
TELEGRAM_BOT_TOKEN=xxxxx
TELEGRAM_CHAT_ID=123456789
```