package investments.performance.services

import investments.performance.model.Models.*
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

fun getPerformanceForTitles(
    mode: OpMode,
    environment: String,
    fees: FeesType,
    secAccountId: String,
    accessToken: String?
): Performance {
    val titles: ArrayList<Title> = getTitles(mode, environment, fees, secAccountId, accessToken)
    val firstTitle: Title? = titles.firstOrNull() //Initialise first title
    var performanceSum: BigDecimal = BigDecimal.ZERO
    val precision: Int
    var currency = ""

    return if (firstTitle != null) {
        precision = firstTitle.performance.Precision
        currency = firstTitle.performance.Currency

        titles.filter { it.performance.Currency == firstTitle.performance.Currency }.forEach {
            performanceSum += formatDecimal(it.performance.Value, it.performance.Precision)
        }

        val performanceSumAmount = formatAmount(performanceSum, precision, currency)
        Performance(secAccountId, titlesCount = titles.count(), amount = performanceSumAmount)
    } else {
        val emptyAmount = Amount(Value = BigInteger.ZERO, Precision = 0, currency)
        Performance(secAccountId, 0, emptyAmount)
    }
}

private fun getTitles(
    mode: OpMode,
    environment: String,
    fees: FeesType,
    secAccountId: String,
    accessToken: String?
): ArrayList<Title> {
    val jsonObjSecurities: JSONObject = getJsonObjectSecurities(mode, environment, secAccountId, accessToken)
    val subSecAccountsJson: JSONArray = getSubSecAccountsJson(jsonObjSecurities)

    val performanceObjName = getPerformanceObjectName(fees)
    val subSecAccountsList = getSubSecAccountList(subSecAccountsJson, performanceObjName)

    val titleList = arrayListOf<Title>()

    subSecAccountsList.filter { it.titles.isNotEmpty() }.forEach {
        titleList.addAll(it.titles)
    }

    return titleList
}

private fun getJsonObjectSecurities(
    mode: OpMode,
    environment: String,
    secAccountId: String,
    accessToken: String?
): JSONObject {
    if (mode == OpMode.PROXY) {
        val fileName = getFileNameSecurities(environment)
        val path = getFilePath(fileName)
        val f = File(path)

        return if (f.exists()) {
            val jsonString: String = f.readText(Charsets.UTF_8)
            JSONObject(jsonString)
        } else
            JSONObject()
    } else //Mode = API call - not working on localhost
    {
        val url = getApiUrl(environment).plus(secAccountId)
        val restTemplate = RestTemplate()
        val headers = createHttpHeaders(accessToken)
        val entity = HttpEntity("parameters", headers)
        val response: ResponseEntity<String> = restTemplate.exchange<String>(
            url, HttpMethod.GET, entity,
            String::class.java
        )

        return JSONObject(
            response.toString()
                .substring(response.toString().indexOf("{"), response.toString().lastIndexOf("}") + 1)
        )
    }
}

private fun getSubSecAccountsJson(jsonObjSecurities: JSONObject): JSONArray {
    return if (!jsonObjSecurities.isNull("securitiesAccount"))
        jsonObjSecurities.getJSONObject("securitiesAccount").getJSONArray("subSecAccounts") //AT
    else
        jsonObjSecurities.getJSONArray("subSecAccounts")
}

private fun getSubSecAccountList(subSecAccountsJson: JSONArray, performanceObj: String): List<SubSecAccount> {
    val subSecAccountList = arrayListOf<SubSecAccount>()
    for (a in 0 until subSecAccountsJson.length()) {
        val subSecAccount = SubSecAccount(
            subSecAccountsJson.getJSONObject(a).getString("id"),
            subSecAccountsJson.getJSONObject(a).getString("name"),
            getTitlesList(subSecAccountsJson.getJSONObject(a), performanceObj)
        )
        subSecAccountList.add(subSecAccount)
    }
    return subSecAccountList
}

private fun getTitlesList(subSecAccountsJson: JSONObject, performanceObj: String): List<Title> {
    val titleList = arrayListOf<Title>()
    val titles = subSecAccountsJson.getJSONArray("titles")

    for (b in 0 until titles!!.length()) {
        val isin = titles.getJSONObject(b).getString("isin")
        var performanceAmount = Amount(BigInteger.ZERO, 0, "")

        if (!titles.getJSONObject(b).isNull(performanceObj)) {
            val performanceJsonObject: JSONObject = titles.getJSONObject(b).getJSONObject(performanceObj)

            performanceAmount = Amount(
                performanceJsonObject.getString("value").toBigInteger(),
                performanceJsonObject.getString("precision").toInt(),
                performanceJsonObject.getString("currency")
            )
        }

        val title = Title(isin, performanceAmount)
        titleList.add(title)
    }

    return titleList
}