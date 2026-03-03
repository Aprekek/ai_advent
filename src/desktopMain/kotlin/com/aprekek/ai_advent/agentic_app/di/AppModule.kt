package com.aprekek.ai_advent.agentic_app.di

import com.aprekek.ai_advent.agentic_app.data.local.AppDirectories
import com.aprekek.ai_advent.agentic_app.data.local.AppStateStore
import com.aprekek.ai_advent.agentic_app.data.local.FileChatRepository
import com.aprekek.ai_advent.agentic_app.data.local.FilePreferencesRepository
import com.aprekek.ai_advent.agentic_app.data.local.FileUserRepository
import com.aprekek.ai_advent.agentic_app.data.local.ProfileStateStore
import com.aprekek.ai_advent.agentic_app.data.remote.deepseek.DeepSeekStreamingGateway
import com.aprekek.ai_advent.agentic_app.data.security.SecureApiKeyRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatStreamingGateway
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository
import com.aprekek.ai_advent.agentic_app.domain.usecase.BootstrapAppUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CreateChatUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CreateProfileUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.LoadWorkspaceUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SaveApiKeyUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SelectChatUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SetPanelLayoutUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SetThemeUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SwitchProfileUseCase
import com.aprekek.ai_advent.agentic_app.presentation.state.AppViewModel
import com.aprekek.ai_advent.agentic_app.util.DefaultIdGenerator
import com.aprekek.ai_advent.agentic_app.util.SystemTimeProvider
import com.aprekek.ai_advent.agentic_app.util.defaultJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(): KoinApplication {
    return startKoin {
        modules(appModule)
    }
}

private val appModule = module {
    single<Json> { defaultJson() }
    single { AppDirectories() }
    single { AppStateStore(get(), get()) }
    single { ProfileStateStore(get(), get()) }

    single<IdGenerator> { DefaultIdGenerator() }
    single<TimeProvider> { SystemTimeProvider() }

    single<HttpClient> {
        HttpClient(CIO) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(get())
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 90_000
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = Logger.DEFAULT
            }
        }
    }

    single<UserRepository> { FileUserRepository(get(), get(), get()) }
    single<PreferencesRepository> { FilePreferencesRepository(get()) }
    single<ChatRepository> { FileChatRepository(get(), get(), get()) }
    single<ApiKeyRepository> { SecureApiKeyRepository(get()) }
    single<ChatStreamingGateway> { DeepSeekStreamingGateway(get(), get()) }

    single { LoadWorkspaceUseCase(get(), get(), get()) }
    single { BootstrapAppUseCase(get(), get(), get()) }
    single { CreateProfileUseCase(get(), get()) }
    single { SwitchProfileUseCase(get(), get()) }
    single { CreateChatUseCase(get(), get()) }
    single { SelectChatUseCase(get()) }
    single { SaveApiKeyUseCase(get()) }
    single { SetThemeUseCase(get()) }
    single { SetPanelLayoutUseCase(get()) }
    single { SendMessageUseCase(get(), get(), get(), get(), get()) }

    single {
        AppViewModel(
            bootstrapAppUseCase = get(),
            createProfileUseCase = get(),
            switchProfileUseCase = get(),
            loadWorkspaceUseCase = get(),
            createChatUseCase = get(),
            selectChatUseCase = get(),
            saveApiKeyUseCase = get(),
            setThemeUseCase = get(),
            setPanelLayoutUseCase = get(),
            sendMessageUseCase = get()
        )
    }
}
