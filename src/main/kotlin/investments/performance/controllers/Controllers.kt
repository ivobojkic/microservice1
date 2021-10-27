package investments.performance.controllers

import investments.performance.model.Models.*
import investments.performance.services.getPerformanceForTitles
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class Controllers {
    @GetMapping(value = ["/hello/{name}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getHelloWorld(@PathVariable name: String): String {
        return "Hello world : $name"
    }

    @GetMapping(
        value = ["/performance/{environment}/{fees}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getPerformanceSumProxy(
        @PathVariable environment: String,
        @PathVariable fees: FeesType,
    ): Performance {
        return getPerformanceForTitles(OpMode.PROXY, environment, fees, "", null)
    }

    @GetMapping(
        value = ["/performance/{environment}/{fees}/{secAccountId}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun getPerformanceSumApi(
        @PathVariable environment: String,
        @PathVariable fees: FeesType,
        @PathVariable secAccountId: String,
        @RequestHeader("Authorization") token: String
    ): Performance {

        return getPerformanceForTitles(OpMode.API, environment, fees, secAccountId, token)
    }
}