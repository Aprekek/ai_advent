# Refactor Map (Clean Architecture + SOLID)

## Goals
- Enforce dependency rule: `presentation -> domain <- data`.
- Keep domain provider-agnostic (no API keys, base URLs, HTTP details).
- Reduce coupling by moving wiring to composition root.
- Increase cohesion by splitting current `Main.kt` into focused units.

## Target package map
```text
src/main/kotlin/com/aprekek/ai_advent/agentic_app/
  presentation/
    cli/
      CliRunner.kt
      ModeMenu.kt
      ConsoleView.kt
      LoadingIndicator.kt
      CommandParser.kt
      mode/
        StandardChatController.kt
        ComparisonController.kt
        TemperatureDiffController.kt
        ModelMetricsComparisonController.kt

  domain/
    model/
      ChatMessage.kt
      ChatRole.kt
      GenerationOptions.kt
      ModelId.kt
      Metrics.kt
      CostEstimate.kt
    port/
      ChatGateway.kt
      ConversationState.kt
      MetricsProvider.kt
      ConfigProvider.kt
    usecase/
      SendMessageUseCase.kt
      ComparePromptStrategiesUseCase.kt
      CompareTemperatureUseCase.kt
      CompareModelsWithMetricsUseCase.kt
      BuildTemperatureComparisonPromptUseCase.kt
      BuildModelComparisonPromptUseCase.kt

  data/
    provider/
      deepseek/
        DeepSeekApiClient.kt
        DeepSeekGateway.kt
        DeepSeekDtos.kt
        DeepSeekMapper.kt
      huggingface/
        HuggingFaceApiClient.kt
        HuggingFaceGateway.kt
        HuggingFaceDtos.kt
        HuggingFaceMapper.kt
    state/
      InMemoryConversationState.kt
    config/
      EnvConfigProvider.kt
      AppConfig.kt
    http/
      HttpClientFactory.kt

  di/
    AppModule.kt
  Main.kt
```

## Domain contracts (first stable version)
```kotlin
// domain/model/GenerationOptions.kt
data class GenerationOptions(
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val extraInstruction: String? = null,
    val temperature: Double? = null
)
```

```kotlin
// domain/port/ChatGateway.kt
interface ChatGateway {
    suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String
}
```

```kotlin
// domain/port/ConversationState.kt
interface ConversationState {
    fun history(sessionId: String): List<ChatMessage>
    fun append(sessionId: String, message: ChatMessage)
    fun trimToLast(sessionId: String, maxMessages: Int)
    fun clear(sessionId: String)
}
```

```kotlin
// domain/model/Metrics.kt
data class Metrics(
    val responseTimeMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
```

```kotlin
// domain/port/MetricsProvider.kt
interface MetricsProvider {
    fun lastMetrics(): Metrics?
}
```

```kotlin
// domain/usecase/SendMessageUseCase.kt
class SendMessageUseCase(
    private val chatGateway: ChatGateway,
    private val conversationState: ConversationState,
    private val maxHistoryMessages: Int = 20
) {
    suspend operator fun invoke(
        sessionId: String,
        rawInput: String,
        options: GenerationOptions = GenerationOptions()
    ): Result<String>
}
```

## Data contracts (provider-specific)
- Keep `apiKey/baseUrl/model` only in `data`.
- Add provider request object, for example:

```kotlin
data class ProviderRequestContext(
    val model: String,
    val apiKey: String,
    val baseUrl: String
)
```

- Controllers/use-cases choose a model strategy; data layer resolves credentials/endpoints.

## Composition root
- `di/AppModule.kt` creates:
  - `HttpClient`
  - config provider
  - gateways
  - use cases
  - CLI controllers
- `Main.kt` only does:
  - `val app = AppModule.fromEnvironment()`
  - `app.cliRunner.run()`

## PR plan (recommended)
1. PR-1: Extract presentation from `Main.kt`.
   - Create `ConsoleView`, `LoadingIndicator`, `ModeMenu`, CLI controllers.
   - Keep old domain/data types for now.
   - No behavior changes.

2. PR-2: Stabilize domain.
   - Introduce `GenerationOptions`, `ChatGateway`, `ConversationState`.
   - Remove `apiKey/baseUrl/model` from domain options.
   - Make `SendMessageUseCase` stateless with explicit `sessionId`.

3. PR-3: Rework data adapters.
   - Add `DeepSeekGateway` implementing `ChatGateway`.
   - Move provider context and mapping fully to `data`.
   - Keep `DeepSeekApiClient` HTTP-only.

4. PR-4: Multi-provider + model comparison cleanup.
   - Add dedicated HuggingFace adapter.
   - Replace hardcoded provider overrides in presentation by strategy/use case level selection.
   - Add `MetricsProvider` adapter and cost calculator service.

5. PR-5: Composition root + cleanup.
   - Add `di/AppModule.kt`.
   - Shrink `Main.kt` to bootstrap only.
   - Remove legacy helpers from old monolith file.

## Test map
```text
src/test/kotlin/com/aprekek/ai_advent/agentic_app/
  domain/usecase/
    SendMessageUseCaseTest.kt
    CompareTemperatureUseCaseTest.kt
    CompareModelsWithMetricsUseCaseTest.kt
  data/provider/deepseek/
    DeepSeekGatewayContractTest.kt
    DeepSeekApiClientTest.kt
  presentation/cli/
    StandardChatControllerTest.kt
    ComparisonControllerTest.kt
```

## Acceptance checklist
- Domain module compiles without importing `ktor`, `AppConfig`, or provider DTOs.
- All controllers depend on use cases/interfaces, not concrete repositories/clients.
- `Main.kt` has no business logic (bootstrap only).
- Current modes still work: standard, short, comparison, temperature diff, model metrics comparison.
- Existing tests pass; new tests cover the extracted logic.
