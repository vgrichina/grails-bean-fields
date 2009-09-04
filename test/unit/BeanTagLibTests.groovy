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
}