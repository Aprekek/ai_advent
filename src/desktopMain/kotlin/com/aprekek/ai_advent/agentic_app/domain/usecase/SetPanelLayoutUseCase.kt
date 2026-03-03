package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class SetPanelLayoutUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(layoutState: PanelLayoutState) {
        preferencesRepository.setPanelLayout(layoutState)
    }
}
