import grails.test.GrailsUnitTestCase

import com.grailsrocks.beanfields.BeanTagLib

class BeanTagLibTests extends GrailsUnitTestCase {

    void setUp() {
        super.setUp()
        
        String.metaClass.encodeAsHTML = { -> delegate }
    }
    
    void testSetTextFieldTemplate() {
        mockTagLib(BeanTagLib)
        def taglib = new BeanTagLib()
        
        taglib.inputTemplate( [:], { args -> "TESTING" } )
        
        assertEquals "TESTING", taglib.request.getAttribute('_bean_taglib_params')?.INPUT_TEMPLATE?.call()
    }
    
    void testBeanAndPropertyResolution() {
        mockTagLib(BeanTagLib)
        def taglib = new BeanTagLib()

        def bean = [
            items: "My items"
        ]

        def result = taglib.getActualBeanAndProperty(bean, null, "items")
        assertTrue bean.is(result.bean)
        assertEquals 'items', result.propertyName
        assertEquals 'My items', result.value

        bean = [
            items: ["My indexed item 1", "My indexed item 2"]
        ]

        result = taglib.getActualBeanAndProperty(bean, null, "items[1]")
        assertTrue bean.is(result.bean)
        assertEquals 'items', result.propertyName
        assertEquals 'My indexed item 2', result.value

        bean = [
            items: [description:"A description"]
        ]

        result = taglib.getActualBeanAndProperty(bean, null, "items.description")
        assertTrue bean.items.is(result.bean)
        assertEquals 'description', result.propertyName
        assertEquals 'A description', result.value

        bean = [
            items: [ [description:"A description"], [description:"B description"] ]
        ]

        result = taglib.getActualBeanAndProperty(bean, null, "items[1].description")
        assertTrue bean.items[1].is(result.bean)
        assertEquals 'description', result.propertyName
        assertEquals 'B description', result.value

        bean = [
            items: [ descriptions: [[value:"A value"], [value:"B value"]] ]
        ]

        result = taglib.getActualBeanAndProperty(bean, null, "items.descriptions[0].value")
        assertTrue bean.items.descriptions[0].is(result.bean)
        assertEquals 'value', result.propertyName
        assertEquals 'A value', result.value
    }
}