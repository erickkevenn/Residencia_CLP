# Como rodar o Diagrama de Sequência (DS)

Os artefatos foram adicionados:

- `docs/DS_Residencia_CLP_compat.puml` — PlantUML compatível
- `docs/DS_Residencia_CLP_full.puml` — PlantUML completo
- `src/main/kotlin/br/com/casainteligente/mini/SequenciaEnergiaService.kt` — implementação do DS

## Rodando o projeto

Requisitos: Java 21+ e Gradle Wrapper incluso.

```bash
./gradlew run
```

O `main` atual (`Main.kt`) agora chama o `SequenciaEnergiaService.rodarCiclo()` após gerar as sugestões,
aplicando ações conforme o DS (desligar/ajustar nível) e registrando no console.
