import grails.test.GroovyPagesTestCase
import org.codehaus.groovy.grails.validation.Validateable
import org.springframework.context.MessageSourceResolvable
import org.junit.*
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

class BeanTagLibEnumTests extends GroovyPagesTestCase {

	def grailsApplication

	@Before void stubMessageSource() {
		grailsApplication.mainContext.messageSource.useCodeAsDefaultMessage = true
	}

	@After void restoreMessageSource() {
		grailsApplication.mainContext.messageSource.useCodeAsDefaultMessage = false
	}

	@Test void radioGroupCanBeUsedForAnEnumProperty() {
		def template = """
<bean:radioGroup beanName="bean" property="season"/>
"""
		def bean = build(BeanWithEnumProperty).withProperties(season: Season.Summer).bean

		def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), containsString("""<input type="radio" name="season" value="Spring" id="season_0"  />""")
		assertThat result.trim(), containsString("""<label for="season_0">bean.season.Spring</label>""")
		assertThat result.trim(), containsString("""<input type="radio" name="season" checked="checked" value="Summer" id="season_1"  />""")
	}

	@Test void selectCanBeUsedForAnEnumProperty() {
		def template = """
<bean:select beanName="bean" property="season"/>
"""
		def bean = build(BeanWithEnumProperty).withProperties(season: Season.Summer).bean

		def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), containsString("""<option value="Spring" >Spring</option>""")
		assertThat result.trim(), containsString("""<option value="Summer" selected="selected" >Summer</option>""")
	}

	@Test void radioGroupRecognisesI18nEnumProperty() {
		def template = """
<bean:radioGroup beanName="bean" property="season"/>
"""
		def bean = build(BeanWithI18nEnumProperty).withProperties(season: I18nSeason.Summer).bean

		def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), containsString("""<label for="season_0">*Spring*</label>""")
		assertThat result.trim(), containsString("""<label for="season_1">*Summer*</label>""")
		assertThat result.trim(), containsString("""<label for="season_2">*Autumn*</label>""")
		assertThat result.trim(), containsString("""<label for="season_3">*Winter*</label>""")
	}

	@Test void selectRecognisesI18nEnumProperty() {
		def template = """
<bean:select beanName="bean" property="season"/>
"""
		def bean = build(BeanWithI18nEnumProperty).withProperties(season: I18nSeason.Summer).bean

		def result = applyTemplate(template, [bean: bean])

		assertThat result.trim(), containsString("""<option value="Spring" >*Spring*</option>""")
		assertThat result.trim(), containsString("""<option value="Summer" selected="selected" >*Summer*</option>""")
		assertThat result.trim(), containsString("""<option value="Autumn" >*Autumn*</option>""")
		assertThat result.trim(), containsString("""<option value="Winter" >*Winter*</option>""")
	}

	private static <T> BeanBuilder<T> build(Class<T> type) {
		new BeanBuilder(type)
	}

}

@Validateable
class BeanWithEnumProperty {

	Season season

	static constraints = {
		season nullable: false
	}
}

enum Season {
	Spring, Summer, Autumn, Winter
}

@Validateable
class BeanWithI18nEnumProperty {

	I18nSeason season

	static constraints = {
		season nullable: false
	}
}

enum I18nSeason implements MessageSourceResolvable {
	Spring, Summer, Autumn, Winter

	String[] getCodes() {
		["season.${name()}"] as String[]
	}

	Object[] getArguments() {
		new Object[0]
	}

	String getDefaultMessage() {
		"*${name()}*"
	}
}
