/*
 * Copyright (C) 2022 Peter Paul Bakker - Stokpop Software Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.stokpop.jmeter

import nl.stokpop.jmeter.JmeterGenUtils.replaceSome
import nl.stokpop.jmeter.domain.Argument
import org.apache.jmeter.testelement.TestPlan
import org.apache.jorphan.collections.HashTree
import org.apache.jorphan.collections.ListedHashTree

/**
 * Example to generate from open-api for example.
 */
class JmeterGeneratorMini : JmeterTreeGenerator {

    override fun generate(): HashTree {

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
}