package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.Metrics
import com.aprekek.ai_advent.agentic_app.domain.model.ModelId
import com.aprekek.ai_advent.agentic_app.domain.model.ModelVariant
import com.aprekek.ai_advent.agentic_app.domain.model.PricingMode
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.port.MetricsProvider
import com.aprekek.ai_advent.agentic_app.domain.port.ModelExecutionGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CompareModelsWithMetricsUseCaseTest {
    @Test
    fun `returns stage results with metrics`() = runTest {
        val gateway = CapturingExecutionGateway(mutableListOf("a1", "a2"))
        val metricsProvider = FixedMetricsProvider(Metrics(100, 10, 20, 30))
        val useCase = CompareModelsWithMetricsUseCase(gateway, metricsProvider)

        val stages = listOf(
            ModelVariant("s1", ProviderType.DeepSeek, ModelId("m1"), PricingMode.DeepSeekReasonerPricing),
            ModelVariant("s2", ProviderType.HuggingFace, ModelId("m2"), PricingMode.NotAvailable)
        )

        val result = useCase("q", stages)

        assertTrue(result.isSuccess)
        val outputs = result.getOrThrow()
        assertEquals(2, outputs.size)
        assertEquals("a1", outputs[0].response)
        assertEquals(30, outputs[0].metrics?.totalTokens)
        assertEquals("m1", gateway.calledModelIds[0])
    }

    private class CapturingExecutionGateway(
        private val responses: MutableList<String>
    ) : ModelExecutionGateway {
        val calledModelIds = mutableListOf<String>()

        override suspend fun generate(
            provider: ProviderType,
            modelId: ModelId,
            messages: List<ChatMessage>,
            options: GenerationOptions
        ): String {
            calledModelIds += modelId.value
            return responses.removeFirst()
        }
    }

    private class FixedMetricsProvider(
        private val metrics: Metrics
    ) : MetricsProvider {
        override fun lastMetrics(provider: ProviderType): Metrics = metrics
    }
}
