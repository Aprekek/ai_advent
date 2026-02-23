package com.aprekek.ai_advent.agentic_app.data.provider

import com.aprekek.ai_advent.agentic_app.data.provider.deepseek.DeepSeekGateway
import com.aprekek.ai_advent.agentic_app.data.provider.huggingface.HuggingFaceGateway
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.ModelId
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.port.ModelExecutionGateway

class UnifiedModelExecutionGateway(
    private val deepSeekGateway: DeepSeekGateway,
    private val huggingFaceGateway: HuggingFaceGateway
) : ModelExecutionGateway {
    override suspend fun generate(
        provider: ProviderType,
        modelId: ModelId,
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): String {
        return when (provider) {
            ProviderType.DeepSeek -> deepSeekGateway.generateWithModel(modelId.value, messages, options)
            ProviderType.HuggingFace -> huggingFaceGateway.generate(modelId, messages, options)
        }
    }
}
