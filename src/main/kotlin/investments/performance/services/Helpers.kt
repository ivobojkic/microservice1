package investments.performance.services

import investments.performance.model.Models
import org.json.JSONObject
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

fun getPerformanceObjectName(fees: Models.FeesType): String {
    return when (fees) {
        Models.FeesType.FEES_IN -> "performanceInclFees"
        Models.FeesType.FEES_OUT -> "performanceExclFees"
        Models.FeesType.FEES -> "performance"
    }
}

fun getFileNameSecurities(environment: String): String {
    val countryCode = environment.substring(0, environment.indexOf("_"))
    return "mySecurities$countryCode.json"
}

fun getFilePath(fileName: String): String {
    val directory = File("./").absolutePath.replace("/.", "")
    return "$directory/src/main/assets/$fileName"
}

fun getApiUrl(environment: String): String {
    val path = getFilePath("environments.json")
    val jsonObjEnvironment: JSONObject = getJsonObjectEnvironments(path)
    return jsonObjEnvironment.getString(environment)
}

fun getJsonObjectEnvironments(filePath: String): JSONObject {
    val f = File(filePath)

    return if (f.exists()) {
        val jsonString: String = f.readText(Charsets.UTF_8)
        JSONObject(jsonString)
    } else
        JSONObject()
}

fun createHttpHeaders(accessToken: String?): HttpHeaders {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_JSON
    headers.add("Authorization", "$accessToken")
    return headers
}

fun formatDecimal(value: BigInteger, precision: Int): BigDecimal {
    return BigDecimal(value).setScale(precision)
}

fun formatAmount(value: Number, precision: Int, currency: String): Models.Amount {
    val valueAsString = value.toString().replace(".", "")
    return Models.Amount(valueAsString.toBigInteger(), precision, currency)
}