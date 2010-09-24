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

	private static MyPerson personWithErrors(properties, errorCodes) {
		def person = new MyPerson(properties)
		prepareForConstraintsTests(MyPerson, [:], [person])
		errorCodes.each { field, code ->
			person.errors.rejectValue(field, code)
		}
		person
	}

    @Test void errorClassIsPassedToInputTemplate() {
        def template = """
<bean:inputTemplate>\${errorClassToUse}</bean:inputTemplate>
<bean:input beanName="person" property="title"/>
"""
		def person = personWithErrors([:], [title: "blank"])

        def result = applyTemplate(template, [person: person])

		assertThat result.trim(), equalTo("error")
    }

}