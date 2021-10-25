package msInvestments.service1

import java.math.BigInteger

class Models {
     class SubSecAccount(
         var id: String,
         var name: String,
         var titles: List<Title>)

    data class Title(
        val isin: String,
        val performance: Amount)

    data class Performance(
        val secAccountId: String,
        var titlesCount:Int,
        val amount: Amount)

    data class Amount(
        val Value: BigInteger,
        val Precision: Int,
        val Currency: String)
}