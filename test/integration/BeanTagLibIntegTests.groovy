import grails.test.GroovyPagesTestCase

import com.grailsrocks.beanfields.BeanTagLib
/**
 * Tests for bean tag rendering
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Antony Stubbs (antony.stubbs@gmail.com)
 */
class BeanTagLibIntegTests extends GroovyPagesTestCase {

    static transactional = false
    
    void testTemplateVarExpansion() {
        def t = """
<bean:inputTemplate>TEST: \${field}</bean:inputTemplate>
<bean:input beanName="bean1" property="field1"/>
"""
        def result = applyTemplate(t, [bean1:[field1:'value1']])
        
        assertTrue result.indexOf("TEST: ") > 0
        assertTrue result.indexOf('input') > 0
        assertTrue result.indexOf('type="text"') > 0
        assertTrue result.indexOf('name="field1"') > 0
    }


    void testInputField() {
        def t = """
<bean:input beanName="bean1" property="field1"/>
"""
        def result = applyTemplate(t, [bean1:[field1:'value1']])

        println "Result: ${result}"
        assertTrue result.indexOf('<label for="field1" class=" ">Field1<') > 0
        assertTrue result.indexOf('input') > 0
        assertTrue result.indexOf('type="text"') > 0
        assertTrue result.indexOf('name="field1"') > 0
        assertTrue result.indexOf('value="value1"') > 0
    }
    
    void testInputFieldCustonLabel() {
        def t = """
<bean:input beanName="bean1" property="field1" label="I don't do i18n"/>
"""
        def result = applyTemplate(t, [bean1:[field1:'value1']])

        println "Result: ${result}"
        assertTrue result.indexOf('<label for="field1" class=" ">I don\'t do i18n<') > 0
        assertTrue result.indexOf('input') > 0
        assertTrue result.indexOf('type="text"') > 0
        assertTrue result.indexOf('name="field1"') > 0
        assertTrue result.indexOf('value="value1"') > 0
    }
        
    void testSubscriptRootProperty() {
        def t = """
<bean:input beanName="bean1" property="items[0]"/>
"""
        def result = applyTemplate(t, [bean1:new ListPropertyTester(items:['valueX', 'valueY'])])

        println "Result: ${result}"
        assertTrue result.indexOf('<label for="items[0]" class=" ">Items 0<') > 0
        assertTrue result.indexOf('name="items[0]"') > 0
        assertTrue result.indexOf('value="valueX"') > 0
    }
            
    void testSubscriptNestedProperty() {
        def t = """
<bean:input beanName="bean1" property="items[0].name"/>
"""
        def result = applyTemplate(t, [bean1:new ListPropertyTester(items:[ [name:'valueP'], [name:'valueQ'] ])])

        println "Result: ${result}"
        assertTrue result.indexOf('<label for="items[0].name" class=" ">Items 0 Name<') > 0
        assertTrue result.indexOf('name="items[0].name"') > 0
        assertTrue result.indexOf('value="valueP"') > 0
    }

    void testChainedSubscriptNestedProperty() {
        def t = """
<bean:input beanName="bean1" property="items[0].products[1].name"/>
"""
        def result = applyTemplate(t, [bean1:new ListPropertyTester(items:[ 
                [products: [ [name:'product0_0'], [name:'product0_1'], [name:'product0_2'] ] ],
                [products: [ [name:'product1_0'], [name:'product1_1'], [name:'product1_2'] ] ]
            ])
        ])

        println "Result: ${result}"
        assertTrue result.indexOf('<label for="items[0].products[1].name" class=" ">Items 0 Products 1 Name<') > 0
        assertTrue result.indexOf('name="items[0].products[1].name"') > 0
        assertTrue result.indexOf('value="product0_1"') > 0
    }

    void testLabelKeyDoesNotContainArraySubscripts() {
        def t = """
<bean:inputTemplate>\${field} labelKey: [\${labelKey}]</bean:inputTemplate>
<bean:input beanName="bean1" property="items[3]"/>
"""
        def result = applyTemplate(t, [bean1:new ListPropertyTester(items:[1,2,3,4])])

        println "Result: ${result}"

        assertTrue result.indexOf("[bean1.items]") > 0
        assertTrue result.indexOf('input') > 0
        assertTrue result.indexOf('type="text"') > 0
        assertTrue result.indexOf('name="items[3]"') > 0
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
        
        assertTrue result.indexOf('<label for="field1.subfield1" class=" ">Field1 Subfield1<') > 0
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
    
     void testBasicSelectRender() {
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        grails.test.MockUtils.prepareForConstraintsTests(MyPerson, [:], [p])
        grails.test.MockUtils.prepareForConstraintsTests(MyAddress, [:], [p.shippingAddress])

        def expected ="""<label for="title" class=" ">Title*</label><select name="title" id="title" >
<option value="Mr." >Mr.</option>
<option value="Mrs." >Mrs.</option>
</select><br/>"""

        def template = '<bean:select beanName="personInstance" property="title"/>'
        def result = applyTemplate( template, [personInstance: p] )

        println "Result is: ${result}"
        assertEquals expected.normalize() , result.normalize()
      }

    void testBasicSelectRenderUsingFieldTag() {
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        grails.test.MockUtils.prepareForConstraintsTests(MyPerson, [:], [p])
        grails.test.MockUtils.prepareForConstraintsTests(MyAddress, [:], [p.shippingAddress])

        def expected ="""<label for="title" class=" ">Title*</label><label for="title_0" class=" ">Mister</label><input type="radio" name="title" value="Mr." id="title_0"  /><br/><label for="title_1" class=" ">Misses</label><input type="radio" name="title" value="Mrs." id="title_1"  /><br/>"""

        def template = '<bean:field beanName="personInstance" property="title"/>'
        def result = applyTemplate( template, [personInstance: p] )

        println "Result is: ${result}"
        assertEquals expected.normalize() , result.normalize()
    }

      void testAdvancedNestedRender() {

        def template = '<bean:field beanName="personInstance" property="shippingAddress.street"/>'
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        grails.test.MockUtils.prepareForConstraintsTests(MyAddress, [:], [p.shippingAddress])

        def result = applyTemplate( template, [personInstance: p] )
        println "Result:"
        println result

        def expected = """<label for="shippingAddress.street" class=" ">Shipping Address Street*</label><input class=" " type="text"   id="shippingAddress.street"  name="shippingAddress.street" value="" /><br/>"""

        assertEquals expected, result

      }

      void testAdvancedNestedSelectRender() {

        def template = '<bean:select beanName="personInstance" property="shippingAddress.country"/>'
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        def result = applyTemplate( template, [personInstance: p] )
        println "Result:"
        println result

        def expected = """<label for="shippingAddress.country" class=" ">Your country*</label><select name="shippingAddress.country" id="shippingAddress.country" >
<option value="US" >US</option>
<option value="UK" >UK</option>
</select><br/>"""

        assertEquals expected.normalize(), result.normalize()

      }

      void testRadioNestedDomain(){
        def template = '<bean:radioGroup beanName="personInstance" property="shippingAddress.country"/>'
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        grails.test.MockUtils.prepareForConstraintsTests(MyPerson, [:], [p])
        grails.test.MockUtils.prepareForConstraintsTests(MyAddress, [:], [p.shippingAddress])

        def result = applyTemplate( template, [personInstance: p] )

        println "Result:"
        println result

        def expected = """<label for="shippingAddress.country" class=" ">Your country*</label><label for="shippingAddress.country_0" class=" ">U.S.A.</label><input type="radio" name="shippingAddress.country" value="US" id="shippingAddress.country_0"  /><br/><label for="shippingAddress.country_1" class=" ">United Kingdom</label><input type="radio" name="shippingAddress.country" value="UK" id="shippingAddress.country_1"  /><br/>"""

        assertEquals expected.normalize(), result.normalize()
      }

      void testCheckBoxNestedDomain(){
        def template = '<bean:checkBox beanName="personInstance" property="shippingAddress.billing"/>'
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        def result = applyTemplate( template, [personInstance: p] )
        println "Result:"
        println result

        def expected = """<label for="shippingAddress.billing" class=" ">Shipping Address Billing*</label><input type="hidden" name="shippingAddress._billing" /><input type="checkbox" name="shippingAddress.billing" id="shippingAddress.billing"  /><br/>"""
        assertEquals expected.normalize(), result.normalize()
      }

      void testNestedDomainLabelFormat(){

        def template = '<bean:field beanName="personInstance" property="shippingAddress.street"/>'
        def p = new MyPerson()
        p.shippingAddress = new MyAddress()

        grails.test.MockUtils.prepareForConstraintsTests(MyAddress, [:], [p.shippingAddress])

        def result = applyTemplate( template, [personInstance: p] )
        println "Result:"
        println result

        def expected = """<label for="shippingAddress.street" class=" ">Shipping Address Street*</label><input class=" " type="text"   id="shippingAddress.street"  name="shippingAddress.street" value="" /><br/>"""

        assertEquals expected, result
      }
}

@org.codehaus.groovy.grails.validation.Validateable
class MyPerson {
  
  String name, title
  
  MyAddress shippingAddress
  
  static constraints = {
    shippingAddress(required:true, nullable:false)
    title(blank:false, inList:["Mr.", "Mrs."])
  }
}

@org.codehaus.groovy.grails.validation.Validateable
class MyAddress {
  
  String country, street
  Boolean billing
  
  static constraints = {
    country(inList:["US","UK"])
  }
}

@org.codehaus.groovy.grails.validation.Validateable
class ListPropertyTester {
  
  String title
  List items
}

class TestBean {
    Date datefield
    String stringfield
    String longstringfield
    Integer intfield
    Long longfield
    Boolean booleanfield
}