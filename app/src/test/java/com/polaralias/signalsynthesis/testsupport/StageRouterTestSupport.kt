package com.polaralias.signalsynthesis.testsupport

import com.polaralias.signalsynthesis.domain.ai.LlmProvider
import com.polaralias.signalsynthesis.domain.ai.LlmStageRequest
import com.polaralias.signalsynthesis.domain.ai.LlmStageResponse
import com.polaralias.signalsynthesis.domain.ai.StageLlmRunner
import com.polaralias.signalsynthesis.domain.ai.StageModelRouter
import com.polaralias.signalsynthesis.domain.ai.UserModelRoutingConfig
import com.polaralias.signalsynthesis.domain.model.AnalysisStage

data class RecordedStageCall(
    val stage: AnalysisStage,
    val request: LlmStageRequest,
    val provider: LlmProvider,
    val model: String
)

data class StageRouterFixture(
    val router: StageModelRouter,
    val calls: MutableList<RecordedStageCall>
)

fun stageRouterFixture(
    responses: Map<AnalysisStage, LlmStageResponse>,
    routingConfig: UserModelRoutingConfig = UserModelRoutingConfig(),
    apiKeys: Map<LlmProvider, String> = mapOf(LlmProvider.OPENAI to "test-key")
): StageRouterFixture {
    val calls = mutableListOf<RecordedStageCall>()

    val router = StageModelRouter(
        runnerFactory = { provider, model, _ ->
            object : StageLlmRunner {
                override suspend fun run(request: LlmStageRequest): LlmStageResponse {
                    calls += RecordedStageCall(request.stage, request, provider, model)
                    return responses[request.stage]
                        ?: error("No fake response configured for ${request.stage}")
                }
            }
        },
        routingConfigProvider = { routingConfig },
        apiKeysProvider = { apiKeys }
    )

    return StageRouterFixture(router = router, calls = calls)
}
