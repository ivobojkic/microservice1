package msInvestments.service1

import msInvestments.service1.Models.*
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger


class Services() {
    fun getTitles(mode: String, environment: String, fees: String, secAccountId : String, accessToken : String) : ArrayList<Title> {
        val jsonObj = getJsonObjectSecurities(mode, environment, secAccountId, accessToken)
        val titleList = arrayListOf<Title>()

        val performanceObj = getPerformanceObjectName(fees)

        val subSecAccountsJson : JSONArray = if(!jsonObj.isNull("securitiesAccount"))
            jsonObj.getJSONObject("securitiesAccount").getJSONArray("subSecAccounts") //AT
        else
            jsonObj.getJSONArray("subSecAccounts") //All other countries


        val subSecAccountsList = getSubSecAccountList(subSecAccountsJson, performanceObj)

        subSecAccountsList.forEach() {
                    titleList.addAll(it.titles)
            }

        return titleList
    }

    private fun getPerformanceObjectName(fees: String): String{
        return when (fees) {
            "FEES_IN" -> "performanceInclFees"
            "FEES_OUT" -> "performanceExclFees"
            else -> "performance"
        }
    }

    private fun getJsonObjectSecurities(mode: String, environment: String, secAccountId : String, accessToken : String): JSONObject {
        if(mode == "FILE") {
            val fileName = getFileNameSecurities(environment)
            val path = this.getFilePath(fileName)
            val f = File(path)

            return if (f.exists()) {
                val jsonString: String = f.readText(Charsets.UTF_8)
                JSONObject(jsonString)
            } else
                JSONObject()
        }
        else //Mode = API call - not working on localhost
        {
            val url = this.getApiUrl(environment).plus(secAccountId)

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

    private fun getFileNameSecurities(environment: String) : String {
        val countryCode = environment.substring(0, environment.indexOf("_"))
        return "mySecurities$countryCode.json"
    }

    private fun getFilePath(fileName: String) : String {
        val directory = File("./").absolutePath.replace("/.", "")
        return "$directory/src/main/assets/$fileName"
    }

    private fun getApiUrl(environment: String): String {
        val path = this.getFilePath("environments.json")
        val jsonObjEnvironment : JSONObject = this.getJsonObjectEnvironments(path)
        return jsonObjEnvironment.getString(environment)
    }

    private fun getJsonObjectEnvironments(filePath : String): JSONObject {
        val f = File(filePath)

        return if (f.exists()) {
            val jsonString: String = f.readText(Charsets.UTF_8)
            JSONObject(jsonString)
        } else
            JSONObject()
    }

    private fun createHttpHeaders(accessToken: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.add("Authorization", "Bearer $accessToken")
        return headers
    }

    private fun getSubSecAccountList(subSecAccountsJson : JSONArray, performanceObj : String): List<SubSecAccount> {
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

    private fun getTitlesList(subSecAccountsJson : JSONObject, performanceObj : String): List<Title> {
        val titleList = arrayListOf<Title>()

        val titles = subSecAccountsJson.getJSONArray("titles")
        for (b in 0 until titles!!.length()) {
            val isin = titles.getJSONObject(b).getString("isin")
            var performanceAmount = Amount(BigInteger.ZERO, 0, "")

            if(!titles.getJSONObject(b).isNull(performanceObj)) {
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

    fun getPerformanceForTitles(secAccountId: String, titles: ArrayList<Title>) : Performance {
        val firstTitle: Title? = titles.firstOrNull() //Initialise first title
        var performanceSum: BigDecimal = BigDecimal.ZERO
        val precision: Int
        var currency = ""

        if (firstTitle != null) {
            precision = firstTitle.performance.Precision
            currency = firstTitle.performance.Currency

            titles.forEach {
                if (firstTitle.performance.Currency == it.performance.Currency)
                    performanceSum += formatDecimal(it.performance.Value, it.performance.Precision)
            }

            val performanceSumAmount = formatAmount(performanceSum, precision, currency)
            return Performance(secAccountId, titlesCount =  titles.count(), amount = performanceSumAmount)
        }
        else
        {
            val emptyAmount = Amount(Value = BigInteger.ZERO, Precision = 0, currency)
            return Performance(secAccountId, 0, emptyAmount)
        }
    }

    private fun formatDecimal(value: BigInteger, precision: Int): BigDecimal {
        return BigDecimal(value).setScale(precision)
    }

    private fun formatAmount(value: Number, precision: Int, currency: String): Amount {
        val valueAsString = value.toString().replace(".", "")
        return Amount(valueAsString.toBigInteger(), precision, currency)
    }
}


