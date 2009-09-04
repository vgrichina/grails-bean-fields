import grails.test.GroovyPagesTestCase

import com.grailsrocks.beanfields.BeanTagLib

class BeanTagLibIntegTests extends GroovyPagesTestCase {

    static transactional = false
    
    void testTemplateVarExpansion() {
        def t = """
<bean:inputTemplate>TEST: \${field}</bean:inputTemplate>
<bean:input beanName="bean1" property="field1"/>
"""
        def result = applyTemplate(t, [bean1:[field1:'value1']])
        
        assertTrue result.indexOf("TEST: ") > 0
        assertTrue result.indexOf('name="field1"') > 0
    }


    void testInputField() {
        def t = """
<bean:input beanName="bean1" property="field1"/>
"""
        def result = applyTemplate(t, [bean1:[field1:'value1']])

        assertTrue result.indexOf('name="field1"') > 0
        assertTrue result.indexOf('value="value1"') > 0
    }
    
    void testAutoFieldForDate() {
        def t = """
<bean:field beanName="bean1" property="datefield"/>
"""
        def b = new TestBean(datefield:new Date())
        def result = applyTemplate(t, [bean1:b])

        println "RES: "+result

        assertTrue result.indexOf('name="datefield_day"') > 0
        assertTrue result.indexOf('name="datefield_month"') > 0
        assertTrue result.indexOf('name="datefield_year"') > 0
    }

    void testAutoFieldForString() {
        def t = """
<bean:field beanName="bean1" property="stringfield"/>
"""
        def b = new TestBean(stringfield:"stringvalue")
        def result = applyTemplate(t, [bean1:b])

        println "RES: "+result

        assertTrue result.indexOf('<input') > 0
        assertTrue result.indexOf('type="text"') > 0
        assertTrue result.indexOf('name="stringfield"') > 0
        assertTrue result.indexOf('value="stringvalue"') > 0
    }

    void testAutoFieldForBoolean() {
        def t = """
<bean:field beanName="bean1" property="booleanfield"/>
"""
        def b = new TestBean(booleanfield:true)
        def result = applyTemplate(t, [bean1:b])

        println "RES: "+result

        assertTrue result.indexOf('<input') > 0
        assertTrue result.indexOf('type="checkbox"') > 0
        assertTrue result.indexOf('name="booleanfield"') > 0
        assertTrue result.indexOf('checked') > 0

        b.booleanfield = false
        result = applyTemplate(t, [bean1:b])

        println "RES: "+result

        assertTrue result.indexOf('<input') > 0
        assertTrue result.indexOf('type="checkbox"') > 0
        assertTrue result.indexOf('name="booleanfield"') > 0
        assertTrue result.indexOf('checked') < 0
    }

    void testInputFieldNestedProperty() {
        def t = """
<bean:input beanName="bean1" property="field1.subfield1"/>
"""
        def result = applyTemplate(t, [bean1:[field1:[subfield1:'subvalue1']]])

        println "RES: "+result
        
        assertTrue result.indexOf('name="field1.subfield1"') > 0
        assertTrue result.indexOf('value="subvalue1"') > 0
    }

    void testForm() {
        def t = """
<bean:withBean beanName="bean1">
    <bean:input property="field1"/>
    <bean:input property="field2"/>
</bean:withBean>
"""
        def result = applyTemplate(t, [bean1:[field1:'value1', field2:'value2']])

        assertTrue result.indexOf('name="field1"') > 0
        assertTrue result.indexOf('value="value1"') > 0
        assertTrue result.indexOf('name="field2"') > 0
        assertTrue result.indexOf('value="value2"') > 0
    }
}

class TestBean {
    Date datefield
    String stringfield
    String longstringfield
    Integer intfield
    Long longfield
    Boolean booleanfield
}