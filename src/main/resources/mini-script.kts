import nl.stokpop.jmeter.JmeterGenComponents
import nl.stokpop.jmeter.JmeterGenUtils
import nl.stokpop.jmeter.JmeterGenUtils.replaceSome
import org.apache.jmeter.testelement.TestPlan
import org.apache.jorphan.collections.HashTree
import org.apache.jorphan.collections.ListedHashTree
import java.io.File

// uncomment to wait for enter so profilers or debuggers can connect first
// Scanner(System.`in`).nextLine()

val jmeterPropertiesFile = if (args.size == 1) args[0] else "."
JmeterGenUtils.generate(File(jmeterPropertiesFile), JmeterTreeGenerator { generate() })

fun generate(): HashTree {

    val jmeterGen = JmeterGenComponents()
    val tree: HashTree = ListedHashTree()
    val testPlan: TestPlan = jmeterGen.createTestPlan("Spring Boot REST API - 2.0.0", "Afterburner REST API")
    tree.add(testPlan)

        val queryParams: List<Argument> = listOf(
            
        )
        val bodyParams: List<Argument> = listOf(
            Argument("request", replaceSome("request")),
            
        )

        val bodyContents = """
{
  "totalPrice" : 6,
  "prices" : [ 0, 0 ],
  "customer" : "customer",
  "products" : [ "products", "products" ]
}
            """.trimIndent()

        val httpSamplerProxy = jmeterGen.createHttpSamplerProxy(
            name = "purchaseUsingPOST - \${testCase}",
            comment = "Simulates a basket purchase request with validation. What can possibly go wrong?",
            hasBody = true,
            method = "POST",
            path = "/basket/purchase",
            queryParams,
            bodyParams,
            bodyContents = bodyContents
        )
        tree.add(httpSamplerProxy)
    return tree
} 
