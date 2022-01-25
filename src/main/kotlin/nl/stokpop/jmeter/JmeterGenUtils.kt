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

import org.apache.jmeter.gui.util.JSyntaxTextArea
import org.apache.jorphan.collections.HashTree
import org.apache.jorphan.gui.JLabeledChoice
import org.apache.jorphan.gui.JLabeledTextField
import java.io.File
import java.lang.reflect.Field
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JRadioButton
import javax.swing.JTextField

object JmeterGenUtils {

    fun generate(jmeterResourcePath: File, generateJmeterTree: JmeterTreeGenerator) {
        println("Let's Generate!")
        JmeterGenComponents.initJmeter(jmeterResourcePath)
        val tree: HashTree = generateJmeterTree.generate()
        println(tree)
        val pathname = "generated-script.jmx"
        JmeterGenComponents.writeJmeterScript(tree, File(pathname))
    }


    fun injectJChoiceSelect(guiComponent: JComponent, jChoiceName: String, value: String?) {
        try {
            val field = findField(guiComponent, jChoiceName)
            field.isAccessible = true
            val jLabeledChoice: JLabeledChoice = field[guiComponent] as JLabeledChoice
            jLabeledChoice.text = value
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot select on JRadioButton '%s' on gui component '%s'",
                    jChoiceName,
                    guiComponent.name
                ), e
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot select on JRadioButton '%s' on gui component '%s'",
                    jChoiceName,
                    guiComponent.name
                ), e
            )
        }
    }

    fun injectJRadioButtonSelect(guiComponent: JComponent, jRadioButtonName: String) {
        try {
            val field = findField(guiComponent, jRadioButtonName)
            field.isAccessible = true
            val jRadioButton: JRadioButton = field[guiComponent] as JRadioButton
            jRadioButton.isSelected = true
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot select on JRadioButton '%s' on gui component '%s'",
                    jRadioButtonName,
                    guiComponent.name
                ), e
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot select on JRadioButton '%s' on gui component '%s'",
                    jRadioButtonName,
                    guiComponent.name
                ), e
            )
        }
    }

    @Throws(NoSuchFieldException::class)
    private fun findField(type: Any, name: String): Field {
        return findAllDeclaredFields(type.javaClass).stream().filter { f: Field -> name == f.name }
            .findFirst()
            .orElseThrow<Throwable> { NoSuchFieldException(name) }
    }

    /**
     * From: https://stackoverflow.com/questions/1042798/retrieving-the-inherited-attribute-names-values-using-java-reflection#answer-240575
     *
     * As an exercise: try to return a Stream based on a Spliterator that calls super class only when needed (e.g. not terminated by findFirst()).
     *
     * @param type the class to find all fields
     * @return all declared fields for all super classes
     */
    private fun findAllDeclaredFields(type: Class<*>): List<Field> {
        val fields: MutableList<Field> = ArrayList()
        var c: Class<*>? = type
        while (c != null) {
            fields.addAll(listOf<Field>(*c.declaredFields))
            c = c.superclass
        }
        return fields
    }

    fun injectJCheckBoxBoolean(guiComponent: JComponent, jCheckBoxName: String, selected: Boolean) {
        try {
            val field = findField(guiComponent, jCheckBoxName)
            field.isAccessible = true
            val jCheckBox: JCheckBox = field[guiComponent] as JCheckBox
            jCheckBox.isSelected = selected
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot set '%s' on JCheckBox '%s' on gui component '%s'",
                    selected,
                    jCheckBoxName,
                    guiComponent.name
                ), e
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot set '%s' on JCheckBox '%s' on gui component '%s'",
                    selected,
                    jCheckBoxName,
                    guiComponent.name
                ), e
            )
        }
    }

    /**
     * Get a sub component of parent component.
     *
     * @param guiComponent the jmeter gui component
     * @param subComponentName the (private) field name of the sub gui component
     * @return the sub component
     */
    fun <T> findSubComponent(guiComponent: JComponent, subComponentName: String): T {
        return try {
            val field = findField(guiComponent, subComponentName)
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            field[guiComponent] as T
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot find sub gui component '%s' on gui component '%s'",
                    subComponentName,
                    guiComponent.name
                ), e
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot find sub gui component '%s' on gui component '%s'",
                    subComponentName,
                    guiComponent.name
                ), e
            )
        }
    }

    /**
     * Set text field of sub gui component of type JTextField or JLabeledTextField.
     *
     * @param guiComponent the jmeter gui component
     * @param jTextFieldName the name of the (private) JTextField or JLabeledTextField
     * @param jTextFieldText the text to set into the JTextField or JLabeledTextField
     */
    fun injectJTextFieldText(guiComponent: JComponent, jTextFieldName: String, jTextFieldText: String?) {
        try {
            val field = findField(guiComponent, jTextFieldName)
            field.isAccessible = true
            when (val component = field[guiComponent]) {
                is JTextField -> { component.text = jTextFieldText }
                is JLabeledTextField -> { component.text = jTextFieldText }
                else -> throw RuntimeException(
                    String.format(
                        "Component '%s' of '%s' not of type JTextField or JLabeledTextField",
                        jTextFieldName,
                        guiComponent.name
                    )
                )
            }
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot set text '%s' on JTextField '%s' on gui component '%s'",
                    jTextFieldText,
                    jTextFieldName,
                    guiComponent.name
                ), e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot set text '%s' on JTextField '%s' on gui component '%s'",
                    jTextFieldText,
                    jTextFieldName,
                    guiComponent.name
                ), e)
        }
    }

    fun injectJTextFieldText(guiComponent: JComponent, jTextFieldName: String, jTextFieldText: Int) {
        injectJTextFieldText(guiComponent, jTextFieldName, jTextFieldText.toString())
    }

    fun injectBoolean(component: Any, booleanFieldName: String, value: Boolean) {
        try {
            val field = findField(component, booleanFieldName)
            field.isAccessible = true
            field.setBoolean(component, value)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot set '%s' on boolean '%s' on object '%s'",
                    value,
                    booleanFieldName,
                    component::class.java.simpleName), e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot set '%s' on boolean '%s' on object '%s'",
                    value,
                    booleanFieldName,
                    component::class.java.simpleName), e)
        }
    }

    fun readBoolean(component: Any, booleanFieldName: String): Boolean {
        try {
            val field = findField(component, booleanFieldName)
            field.isAccessible = true
            return field.getBoolean(component)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot get from boolean '%s' on object '%s'",
                    booleanFieldName,
                    component::class.java.simpleName), e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot get from boolean '%s' on object '%s'",
                    booleanFieldName,
                    component::class.java.simpleName), e)
        }
    }

    fun replaceSome(text: String): String {
        return text.replace("'", "\"")
    }

    fun injectJSyntaxTextArea(guiComponent: Any, jSyntaxTextAreaName: String, bodyContents: String) {
        try {
            val field = findField(guiComponent, jSyntaxTextAreaName)
            //println("injectJSyntaxTextArea found field: $field")
            field.isAccessible = true
            val component = field[guiComponent]
            //println("injectJSyntaxTextArea found component: $component")
            if (component is JSyntaxTextArea) {
                //println("injectJSyntaxTextArea call setText: $component with $bodyContents")
                component.text = bodyContents
                //println("check: ${component.text}")
            }
            else {
                throw RuntimeException(
                    String.format(
                        "Component '%s' of '%s' not of type JSyntaxTextArea",
                        jSyntaxTextAreaName,
                        component::class.java.simpleName)
                    )
            }
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(
                String.format(
                    "Cannot set text '%s' on JSyntaxTextArea '%s' on gui component '%s'",
                    bodyContents,
                    jSyntaxTextAreaName,
                    guiComponent::class.java.simpleName
                ), e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                String.format(
                    "Cannot set text '%s' on JSyntaxTextArea '%s' on gui component '%s'",
                    bodyContents,
                    jSyntaxTextAreaName,
                    guiComponent::class.java.simpleName
                ), e)
        }
    }
}