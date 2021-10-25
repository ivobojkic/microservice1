package msInvestments.service1

import msInvestments.service1.Models.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class Controllers {
    @GetMapping(value = ["/hello/{name}"], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun getHelloWorld(@PathVariable name: String): String{
        return "Hello world : $name"
    }

    @GetMapping(value = ["/performance/{mode}/{environment}/{fees}/{secAccountId}/{token}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getPerformanceSum(@PathVariable mode: String, @PathVariable environment: String, @PathVariable fees: String, @PathVariable secAccountId: String, @PathVariable token: String): Performance {
        val titles: ArrayList<Title> = Services().getTitles(mode, environment, fees, secAccountId, token)

        return Services().getPerformanceForTitles(secAccountId, titles)
    }
}