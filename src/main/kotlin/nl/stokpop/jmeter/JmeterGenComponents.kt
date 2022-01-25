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

import nl.stokpop.jmeter.domain.Argument
import nl.stokpop.jmeter.domain.SampleErrorAction
import nl.stokpop.jmeter.domain.ThreadLifeTime
import nl.stokpop.jmeter.domain.Variable
import org.apache.jmeter.assertions.ResponseAssertion
import org.apache.jmeter.assertions.gui.AssertionGui
import org.apache.jmeter.config.Arguments
import org.apache.jmeter.config.CSVDataSet
import org.apache.jmeter.config.ConfigTestElement
import org.apache.jmeter.config.gui.ArgumentsPanel
import org.apache.jmeter.control.TransactionController
import org.apache.jmeter.control.gui.LoopControlPanel
import org.apache.jmeter.control.gui.TestPlanGui
import org.apache.jmeter.control.gui.TransactionControllerGui
import org.apache.jmeter.gui.util.PowerTableModel
import org.apache.jmeter.protocol.http.config.gui.HttpDefaultsGui
import org.apache.jmeter.protocol.http.config.gui.UrlConfigGui
import org.apache.jmeter.protocol.http.control.CookieManager
import org.apache.jmeter.protocol.http.control.Header
import org.apache.jmeter.protocol.http.control.HeaderManager
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui
import org.apache.jmeter.protocol.http.gui.CookiePanel
import org.apache.jmeter.protocol.http.gui.HTTPArgumentsPanel
import org.apache.jmeter.protocol.http.gui.HeaderPanel
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy
import org.apache.jmeter.protocol.http.util.HTTPArgument
import org.apache.jmeter.reporters.ResultCollector
import org.apache.jmeter.save.SaveService
import org.apache.jmeter.testbeans.gui.TestBeanGUI
import org.apache.jmeter.testelement.TestElement
import org.apache.jmeter.testelement.TestPlan
import org.apache.jmeter.threads.ThreadGroup
import org.apache.jmeter.threads.gui.ThreadGroupGui
import org.apache.jmeter.timers.GaussianRandomTimer
import org.apache.jmeter.timers.gui.GaussianRandomTimerGui
import org.apache.jmeter.util.JMeterUtils
import org.apache.jmeter.util.ScopePanel
import org.apache.jmeter.visualizers.ViewResultsFullVisualizer
import org.apache.jorphan.collections.HashTree
import org.apache.jorphan.gui.ObjectTableModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.reflect.KClass

class JmeterGenComponents {

    companion object {

        const val DEFAULT_COMMENT = "Generated from jmeter-dsl with defaults"

        fun initJmeter(resourceFilesPath: File = File(".")) {
            // needed to avoid DISPLAY not set issues when this property is not present on Linux
            System.setProperty("java.awt.headless", true.toString())
            // needed to load saveservice.properties with class mappings
            JMeterUtils.setJMeterHome(resourceFilesPath.toString())
            // needed to have name labels from a resource bundle
            JMeterUtils.setLocale(Locale.ENGLISH)
            // needed for the ViewResultsFullVisualizer, to prevent NPE
            JMeterUtils.loadJMeterProperties(File(resourceFilesPath, "jmeter.properties").toString())
        }

        @Throws(IOException::class)
        fun writeJmeterScript(tree: HashTree, jmeterFile: File) {

            val isTestPlan = tree.array[0] is TestPlan
            if (!isTestPlan) {
                throw IOException("Tree is not a valid jMeter TestPlan");
            }
            FileOutputStream(jmeterFile).use { out -> SaveService.saveTree(tree, out) }
        }
    }

    fun createResultCollector(): ResultCollector {
        val resultCollector = ResultCollector()
        defaultTestElementValues(resultCollector, "Result Collector",
            DEFAULT_COMMENT, ResultCollector::class, ViewResultsFullVisualizer::class)
        resultCollector.isErrorLogging = false
        return resultCollector
    }

    fun createResponseAssertion(): ResponseAssertion {
        val assertionGui = AssertionGui()
        JmeterGenUtils.injectJRadioButtonSelect(assertionGui, "responseCodeButton")
        JmeterGenUtils.injectJRadioButtonSelect(assertionGui, "equalsBox")
        val scopePanel: ScopePanel = JmeterGenUtils.findSubComponent(assertionGui, "scopePanel")
        JmeterGenUtils.injectJRadioButtonSelect(scopePanel, "parentButton")
        val tableModel: PowerTableModel = JmeterGenUtils.findSubComponent(assertionGui, "tableModel")
        tableModel.addRow(arrayOf("\${httpStatusCode}"))
        return assertionGui.createTestElement() as ResponseAssertion
    }

    fun createCsvDataSet(name: String?, filename: String): CSVDataSet {
        val testBeanGUI = TestBeanGUI(CSVDataSet::class.java)
        testBeanGUI.name = name
        val propertyMap: MutableMap<String, Any> = JmeterGenUtils.findSubComponent(testBeanGUI, "propertyMap")
        // check CSVDataSetBeanInfo for possible fields and values
        propertyMap["filename"] = filename
        propertyMap["shareMode"] = "shareMode.group"
        return testBeanGUI.createTestElement() as CSVDataSet
    }

    fun createHeaderManager(headers: List<nl.stokpop.jmeter.domain.Header>): HeaderManager {
        val headerPanel = HeaderPanel()
        val headerManager: HeaderManager = headerPanel.createTestElement() as HeaderManager
        // this is much easier than to work on the tableModel
        headers.forEach {
            headerManager.add(Header(it.name, it.value))
        }
        return headerManager
    }

    fun createHttpSamplerProxy(
        name: String,
        comment: String,
        hasBody: Boolean,
        method: String,
        path: String,
        queryParams: List<Argument> = emptyList(),
        bodyParams: List<Argument> = emptyList(),
        bodyContents: String = ""
    ): HTTPSamplerProxy {
        val httpTestSampleGui = HttpTestSampleGui()
        httpTestSampleGui.comment = comment
        httpTestSampleGui.name = name
        val urlConfigGui: UrlConfigGui = JmeterGenUtils.findSubComponent(httpTestSampleGui, "urlConfigGui")

        println("DEBUG: urlConfigGui created with showRawBodyPane: ${JmeterGenUtils.readBoolean(urlConfigGui, "showRawBodyPane")}")
        println("DEBUG: received: $bodyContents")

        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "domain", "")
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "port", "")
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "protocol", "")
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "contentEncoding", "")
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "path", path)
        JmeterGenUtils.injectJChoiceSelect(urlConfigGui, "method", method)
        JmeterGenUtils.injectJCheckBoxBoolean(urlConfigGui, "followRedirects", true)
        JmeterGenUtils.injectJCheckBoxBoolean(urlConfigGui, "autoRedirects", false)
        JmeterGenUtils.injectJCheckBoxBoolean(urlConfigGui, "useKeepAlive", true)
        JmeterGenUtils.injectJCheckBoxBoolean(urlConfigGui, "useMultipart", false)
        JmeterGenUtils.injectJCheckBoxBoolean(urlConfigGui, "useBrowserCompatibleMultipartMode", false)

        val hasBodyContents = bodyContents.isNotBlank()

        if (hasBody != hasBodyContents) {
            println("WARN: hasBody($hasBody) != hasBodyContents($hasBodyContents) for HttpSamplerProxy $name (using hasBodyContents to add bodyContents)")
        }

        if (hasBodyContents) {
            JmeterGenUtils.injectBoolean(urlConfigGui, "showRawBodyPane", true)
            JmeterGenUtils.injectJSyntaxTextArea(urlConfigGui, "postBodyContent", bodyContents)
        }
        else {
            JmeterGenUtils.injectBoolean(urlConfigGui, "showRawBodyPane", false)
            val argsPanel: HTTPArgumentsPanel = JmeterGenUtils.findSubComponent(urlConfigGui, "argsPanel")
            val tableModel: ObjectTableModel = JmeterGenUtils.findSubComponent(argsPanel, "tableModel")
            queryParams.forEach {
                tableModel.addRow(HTTPArgument(it.name, it.value, false, "text/plain"))
            }
            bodyParams.forEach {
                tableModel.addRow(HTTPArgument(it.name, it.value, false, "text/plain"))
            }
        }
        return httpTestSampleGui.createTestElement() as HTTPSamplerProxy
    }

    fun createHttpDefaults(protocol: String, serverNameOrIP: String, port: String): ConfigTestElement {
        val httpDefaultsGui = HttpDefaultsGui()
        val urlConfigGui: UrlConfigGui = JmeterGenUtils.findSubComponent(httpDefaultsGui, "urlConfigGui")
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "domain", serverNameOrIP)
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "port", port)
        JmeterGenUtils.injectJTextFieldText(urlConfigGui, "protocol", protocol)
        return httpDefaultsGui.createTestElement() as ConfigTestElement
    }

    fun createThreadGroup(
        name: String = "Thread Group",
        comment: String = DEFAULT_COMMENT,
        sampleErrorAction: SampleErrorAction = SampleErrorAction.continueBox,
        nrOfThreads: Int = 1,
        rampUpSeconds: Int = 1,
        loops: Int = 1,
        infinite: Boolean = false,
        sameUser: Boolean = true,
        delayedStart: Boolean = false,
        threadLifetime: ThreadLifeTime = ThreadLifeTime(false, 0, 0)
    ): ThreadGroup {
        val threadGroupGui = ThreadGroupGui()
        threadGroupGui.name = name
        threadGroupGui.comment = comment
        JmeterGenUtils.injectJTextFieldText(threadGroupGui, "threadInput", nrOfThreads)
        JmeterGenUtils.injectJTextFieldText(threadGroupGui, "rampInput", rampUpSeconds)
        val loopPanel: LoopControlPanel = JmeterGenUtils.findSubComponent(threadGroupGui, "loopPanel")
        JmeterGenUtils.injectJTextFieldText(loopPanel, "loops", loops)
        JmeterGenUtils.injectJCheckBoxBoolean(loopPanel, "infinite", infinite)
        JmeterGenUtils.injectJCheckBoxBoolean(threadGroupGui, "sameUserBox", sameUser)
        JmeterGenUtils.injectJCheckBoxBoolean(threadGroupGui, "delayedStart", delayedStart)

        if (threadLifetime.active) {
            JmeterGenUtils.injectBoolean(threadGroupGui, "showDelayedStart", threadLifetime.active)
            JmeterGenUtils.injectJTextFieldText(threadGroupGui, "duration", threadLifetime.durationSeconds)
            JmeterGenUtils.injectJTextFieldText(threadGroupGui, "delay", threadLifetime.lifetimeSeconds)
        }
        JmeterGenUtils.injectJRadioButtonSelect(threadGroupGui, sampleErrorAction.toString())
        return threadGroupGui.createTestElement() as ThreadGroup
    }

    fun createArguments(variables: List<Variable>): Arguments {
        val arguments = Arguments()
        arguments.setProperty(TestElement.GUI_CLASS, ArgumentsPanel::class.java.simpleName)
        arguments.setProperty(TestElement.TEST_CLASS, Arguments::class.java.simpleName)
        arguments.name = "User Defined Variables"
        arguments.comment = DEFAULT_COMMENT
        arguments.isEnabled = true
        variables.forEach {
            arguments.addArgument(it.name, it.value, "=", it.description)
        }
        return arguments
    }

    fun createTestPlan(name: String,
                       comment: String = DEFAULT_COMMENT): TestPlan {
        val testPlan = TestPlan()
        defaultTestElementValues(testPlan, name, comment, TestPlan::class, TestPlanGui::class)
        testPlan.isFunctionalMode = false
        testPlan.isSerialized = false
        testPlan.testPlanClasspath = ""
        testPlan.setUserDefinedVariables(Arguments())
        return testPlan
    }

    fun createHttpCookieManager(name: String = "Cookie Manager",
                                comment: String = DEFAULT_COMMENT): CookieManager {
        val cookieManager = CookieManager()
        defaultTestElementValues(cookieManager, name, comment, CookieManager::class, CookiePanel::class)
        return cookieManager
    }

    fun createTransactionController(name: String = "Transaction Controller",
                                    comment: String = DEFAULT_COMMENT): TransactionController {
        val controller = TransactionController()
        defaultTestElementValues(controller, name, comment, TransactionController::class, TransactionControllerGui::class)
        return controller
    }

    fun createGaussianRandomTimer(name: String = "Gaussian Random Timer",
                                  comment: String = DEFAULT_COMMENT): GaussianRandomTimer {
        val timer = GaussianRandomTimer()
        defaultTestElementValues(timer, name, comment, GaussianRandomTimer::class, GaussianRandomTimerGui::class)
        timer.delay = "300"
        timer.range = 100.0
        return timer
    }

    private fun defaultTestElementValues(
        testElement: TestElement,
        name: String,
        comment: String = DEFAULT_COMMENT,
        testClassName: KClass<*>,
        guiClassName: KClass<*>
    ) {
        testElement.setProperty(TestElement.GUI_CLASS, guiClassName.java.simpleName)
        testElement.setProperty(TestElement.TEST_CLASS, testClassName.java.simpleName)
        testElement.isEnabled = true
        testElement.name = name
        testElement.comment = comment
    }

}