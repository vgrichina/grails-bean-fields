import grails.test.*
import org.junit.*
import org.springframework.validation.*
import static org.junit.Assert.*
import static grails.test.MockUtils.*
import static org.hamcrest.CoreMatchers.*

class BeanTagLibErrorHandlingTests extends GroovyPagesTestCase {

	def grailsApplication
	
	@Before void stubMessageSource() {
	 	grailsApplication.mainContext.messageSource.useCodeAsDefaultMessage = true
	}
	
	@After void restoreMessageSource() {
		grailsApplication.mainContext.messageSource.useCodeAsDefaultMessage = false
	}

	private static ValidateableBean newBeanInstance(properties, errorCodes = [:]) {
		def bean = new ValidateableBean(properties)
		prepareForConstraintsTests(ValidateableBean, [:], [bean])
		errorCodes.each { field, code ->
			bean.errors.rejectValue(field, code)
		}
		bean
	}

    @Test void errorClassIsPassedToInputTemplate() {
        def template = """
<bean:inputTemplate>\${errorClassToUse}</bean:inputTemplate>
<bean:input beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:], [stringfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToInputTemplate() {
        def template = """
<bean:inputTemplate>\${required}</bean:inputTemplate>
<bean:input beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToSelectTemplate() {
        def template = """
<bean:selectTemplate>\${errorClassToUse}</bean:selectTemplate>
<bean:select beanName="bean" property="enumfield"/>
"""
		def bean = newBeanInstance([:], [enumfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToSelectTemplate() {
        def template = """
<bean:selectTemplate>\${required}</bean:selectTemplate>
<bean:select beanName="bean" property="enumfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToCustomFieldTemplate() {
        def template = """
<bean:customTemplate>\${errorClassToUse}</bean:customTemplate>
<bean:customField beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:], [stringfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToCustomFieldTemplate() {
        def template = """
<bean:customTemplate>\${required}</bean:customTemplate>
<bean:customField beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToDateTemplate() {
        def template = """
<bean:dateTemplate>\${errorClassToUse}</bean:dateTemplate>
<bean:date beanName="bean" property="datefield"/>
"""
		def bean = newBeanInstance([:], [datefield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToDateTemplate() {
        def template = """
<bean:dateTemplate>\${required}</bean:dateTemplate>
<bean:date beanName="bean" property="datefield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToTextAreaTemplate() {
        def template = """
<bean:textAreaTemplate>\${errorClassToUse}</bean:textAreaTemplate>
<bean:textArea beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:], [stringfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToTextAreaTemplate() {
        def template = """
<bean:textAreaTemplate>\${required}</bean:textAreaTemplate>
<bean:textArea beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToCheckBoxTemplate() {
        def template = """
<bean:checkBoxTemplate>\${errorClassToUse}</bean:checkBoxTemplate>
<bean:checkBox beanName="bean" property="booleanfield"/>
"""
		def bean = newBeanInstance([:], [booleanfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToCheckBoxTemplate() {
        def template = """
<bean:checkBoxTemplate>\${required}</bean:checkBoxTemplate>
<bean:checkBox beanName="bean" property="booleanfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToRadioGroupTemplate() {
        def template = """
<bean:radioGroupTemplate>\${errorClassToUse}</bean:radioGroupTemplate>
<bean:radioGroup beanName="bean" property="enumfield"/>
"""
		def bean = newBeanInstance([:], [enumfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToRadioGroupTemplate() {
        def template = """
<bean:radioGroupTemplate>\${required}</bean:radioGroupTemplate>
<bean:radioGroup beanName="bean" property="enumfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToCountryTemplate() {
        def template = """
<bean:countryTemplate>\${errorClassToUse}</bean:countryTemplate>
<bean:country beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:], [stringfield: "nullable"])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToCountryTemplate() {
        def template = """
<bean:countryTemplate>\${required}</bean:countryTemplate>
<bean:country beanName="bean" property="stringfield"/>
"""
		def bean = newBeanInstance([:])

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

}

@org.codehaus.groovy.grails.validation.Validateable
class ValidateableBean {
    String stringfield
	String enumfield
	Date datefield
	Boolean booleanfield

	static constraints = {
		stringfield nullable: false
		enumfield nullable: false, inList: ["foo", "bar"]
		datefield nullable: false
		booleanfield nullable: false
	}
}