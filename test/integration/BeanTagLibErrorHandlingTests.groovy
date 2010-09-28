import grails.test.GroovyPagesTestCase
import static grails.test.MockUtils.prepareForConstraintsTests
import static org.hamcrest.CoreMatchers.equalTo
import org.junit.*
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

class BeanTagLibErrorHandlingTests extends GroovyPagesTestCase {

	def grailsApplication
	
	@Before void stubMessageSource() {
	 	grailsApplication.mainContext.messageSource.useCodeAsDefaultMessage = true
	}
	
	@After void restoreMessageSource() {
		grailsApplication.mainContext.messageSource.useCodeAsDefaultMessage = false
	}

    @Test void errorClassIsPassedToInputTemplate() {
        def template = """
<bean:inputTemplate>\${errorClassToUse}</bean:inputTemplate>
<bean:input beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).withErrors(stringfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToInputTemplate() {
        def template = """
<bean:inputTemplate>\${required}</bean:inputTemplate>
<bean:input beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToSelectTemplate() {
        def template = """
<bean:selectTemplate>\${errorClassToUse}</bean:selectTemplate>
<bean:select beanName="bean" property="enumfield"/>
"""
		def bean = build(ValidateableBean).withErrors(enumfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToSelectTemplate() {
        def template = """
<bean:selectTemplate>\${required}</bean:selectTemplate>
<bean:select beanName="bean" property="enumfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToCustomFieldTemplate() {
        def template = """
<bean:customTemplate>\${errorClassToUse}</bean:customTemplate>
<bean:customField beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).withErrors(stringfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToCustomFieldTemplate() {
        def template = """
<bean:customTemplate>\${required}</bean:customTemplate>
<bean:customField beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToDateTemplate() {
        def template = """
<bean:dateTemplate>\${errorClassToUse}</bean:dateTemplate>
<bean:date beanName="bean" property="datefield"/>
"""
		def bean = build(ValidateableBean).withErrors(datefield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToDateTemplate() {
        def template = """
<bean:dateTemplate>\${required}</bean:dateTemplate>
<bean:date beanName="bean" property="datefield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToTextAreaTemplate() {
        def template = """
<bean:textAreaTemplate>\${errorClassToUse}</bean:textAreaTemplate>
<bean:textArea beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).withErrors(stringfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToTextAreaTemplate() {
        def template = """
<bean:textAreaTemplate>\${required}</bean:textAreaTemplate>
<bean:textArea beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToCheckBoxTemplate() {
        def template = """
<bean:checkBoxTemplate>\${errorClassToUse}</bean:checkBoxTemplate>
<bean:checkBox beanName="bean" property="booleanfield"/>
"""
		def bean = build(ValidateableBean).withErrors(booleanfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToCheckBoxTemplate() {
        def template = """
<bean:checkBoxTemplate>\${required}</bean:checkBoxTemplate>
<bean:checkBox beanName="bean" property="booleanfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToRadioGroupTemplate() {
        def template = """
<bean:radioGroupTemplate>\${errorClassToUse}</bean:radioGroupTemplate>
<bean:radioGroup beanName="bean" property="enumfield"/>
"""
		def bean = build(ValidateableBean).withErrors(enumfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToRadioGroupTemplate() {
        def template = """
<bean:radioGroupTemplate>\${required}</bean:radioGroupTemplate>
<bean:radioGroup beanName="bean" property="enumfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

    @Test void errorClassIsPassedToCountryTemplate() {
        def template = """
<bean:countryTemplate>\${errorClassToUse}</bean:countryTemplate>
<bean:country beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).withErrors(stringfield: "nullable").bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("error")
    }

    @Test void requiredFlagIsPassedToCountryTemplate() {
        def template = """
<bean:countryTemplate>\${required}</bean:countryTemplate>
<bean:country beanName="bean" property="stringfield"/>
"""
		def bean = build(ValidateableBean).bean

        def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), equalTo("*")
    }

	@Test void errorsAreRenderedOnTopLevelProperties() {
		def template = """
<bean:field beanName="person" property="title"/>
		"""

		def person = build(MyPerson).withErrors("title": "blank").bean

		def result = applyTemplate(template, [person: person])

		assertThat result, containsString("""<label for="title" class=" error">""")
		assertThat result, containsString("""<div>blank.MyPerson.title<br/></div>""")
	}
	
	@Test void errorsAreRenderedOnNestedProperties() {
		def template = """
<bean:field beanName="person" property="shippingAddress.country"/>
		"""

		def person = build(MyPerson).withProperties(shippingAddress: build(MyAddress)).withErrors("shippingAddress.country": "nullable").bean

		def result = applyTemplate(template, [person: person])

		assertThat result, containsString("""<label for="shippingAddress.country" class=" error">""")
		assertThat result, containsString("""<div>nullable.MyPerson.shippingAddress.country<br/></div>""")
	}

	private static <T> BeanBuilder<T> build(Class<T> type) {
		new BeanBuilder(type)
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

class BeanBuilder<T> {

	private T instance

	BeanBuilder(Class<T> type) {
		instance = type.newInstance()
		prepareForConstraintsTests type, [:], [instance]
	}

	BeanBuilder<T> withProperties(Map properties) {
		instance.properties = properties
		this
	}

	BeanBuilder<T> withErrors(Map errors) {
		errors.each { field, code ->
			instance.errors.rejectValue(field, code)
		}
		this
	}
	
	T getBean() {
		instance
	}
}