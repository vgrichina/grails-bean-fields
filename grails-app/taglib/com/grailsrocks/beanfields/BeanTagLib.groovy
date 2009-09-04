/* Copyright 2004-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grailsrocks.beanfields


import org.springframework.web.servlet.support.RequestContextUtils as RCU
import org.springframework.validation.Errors
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.web.taglib.GroovyPageTagBody

// @todo Add bean:form tag that takes beanName so is not required in individual tags
// (but can be supplied to override)
// @todo Add bean:autoForm that takes a bean name and controller/action/url etc and automatically
// renders the whole form for all properties. Option to include/exclude certain props
// @todo Add $_tag.labelStart and $_tag.labelEnd in addition $_tag.label to give more flexibility
// @todo Make all tags pass through unrecognized attributes such as "id"
// @todo Make modelDate support min and max values for date fields, in terms of the years shown
// @todo Make modelCountry support inList constraint to constrain the list of permitted countries

/**
 * A tag library that contains tags to help rendering of fields to edit bean properties, using their constraints
 * to render them correctly. For example:
 *
 * <bean:input bean="form" name="user.firstName" label="Name"/>
 *
 * will output, given a model containing a form object with a populated User domain class :-
 *
 * <label for="user.firstName">Name</label> * <input id="user.firstName" name="user.firstName" value="Marc" maxlength="50" size="50"/>
 *
 * If there are errors relating to the field it would output those also. Any information used comes from constraints if they are available,
 * or from "overrides" supplied via attributes to the tag.
 *
 * There are tags for all standard HTML field types, including a datePicker and country selection. The code is eminently extensible
 * and it is easy to defer to existing tags from FormTagLib for the actual rendering.
 *
 * There is a pattern to using these tags - you are not fixed with a specific way of rendering these fields.
 * To parameterize rendering they support setting of parameters that persist between tag invocations, for
 * the duration of the current request.
 *
 * The tags render the current value of an existing bean's properties, and automatically render label elements if desired.
 * They also allow you to render field errors local to the field, and whether or not the field is required.
 *
 * The templates for each tag can be set and changed repeatedly at any point in a single request, allowing you to batch up display settings
 * and modify them if needed. So when constructing an input tag for example you can position the errors and label wherever
 * you like or not at all. You prefix the variables you need to access with _tag:
 *
 * <bean:textFieldTemplate>$_tag.label<br/>$_tag.errors<br/>$_tag.input $_tag.required</bean:textFieldTemplate>
 *
 * From thereonin the <bean:input> tag would use the above template.
 *
 * Each bean:XXXX tag uses one or more templates (closures) to render the tag itself. If no template is available
 * a default will be used. However by customizing it you can get almost full control of the appearance
 * of your forms without editing the taglib.
 *
 * Even the way labels and errors are rendered is templated and customizable
 *
 * There is also a common pattern whereby you can provide a value to override the current bean value (but still
 * have the field name automatically set and constraints applied) and also provide a default value to be used if there
 * is no value and no overriden value.
 *
 * Attributes supported on all tags:
 *
 * property - name of the property relating to the field, including dotted notation for nested properties
 * beanName - name of the bean to which the property relates
 * value (optional) - an explicit value if you don't want to use the current value of the property
 * default (optional) - a default value to use if the value and overridden value are null
 * label (optional) - a label caption for the field
 * labelKey (optional) - a label key to use for the field, resolved against the message source
 *
 * Other attributes are passed through as with most Grails taglibs
 *
 *
 * bean:input - input field
 * Extra attributes:
 * size - display size of the field
 * maxLength - maximum number of characters to accept (if overriding maxSize/size constraint)
 * type - text or password
 *
 * bean:select - select field
 * Extra attributes:
 * from - override the list of available options - else uses values from inList constraint
 * ...also any standard g:select attributes.
 *
 * bean:date - date picker
 * No extra attributes other than standard datePicker attributes
 *
 * bean:textArea - text area
 * No extra attributes other than standard textArea attributes
 *
 * bean:checkBox - check box
 * No extra attributes other than standard checkBox attributes
 *
 * bean:country - country select box
 * No extra attributes other than standard country attributes
 *
 * Setting parameters with modelParam:
 *
 * <bean:requiredIndicator>(required)</bean:requiredIndicator>s
 *
 * This sets one of the supported parametes, used when rendering fields. Currently supported parameters:
 * MAX_INPUT_DISPLAY_LENGTH - length to clamp input fields to if the constraint allows more chars than this
 * ERROR_CLASS - the CSS class to use when rendering errors
 * REQUIRED_INDICATOR - the indication used for required fields
 * SHOW_ERRORS - a boolean determining whether or not to render errors when rendering the fields
 * INPUT_TEMPLATE - template for input fields (label, input, required, errors)
 * RADIO_TEMPLATE - template for radio buttons (label, input, radio, required, errors)
 * CHECKBOX_TEMPLATE - template for checkbox fields (label, checkbox, required, errors)
 * DATE_TEMPLATE - template for checkbox fields (label, datePicker, required, errors)
 * SELECT_TEMPLATE - template for checkbox fields (label, select, required, errors)
 * TEXTAREA_TEMPLATE - template for checkbox fields (label, textArea, required, errors)
 * COUNTRY_TEMPLATE - template for checkbox fields (label, countrySelect, required, errors)
 * LABEL_TEMPLATE - template for labels (errorClass, mandatoryFieldFlagToUse, fieldName, label,
 * fieldValue, defaultValue, varArgs, bean, propertyName, errors)
 * ERROR_TEMPLATE - template for errors, repeated for each error (error, message)
 *
 * The words in brackets after the templates denote the properties available within each template.
 *
 *
 * @author Marc Palmer (based on code by Joe Mooney/Diane Hewitt)
 * @since 17-Jan-2006
 */
class BeanTagLib {

    static namespace = 'bean'

	static final String BEAN_PARAMS = '_bean_taglib_params'

    static final Closure DEFAULT_FIELD_RENDERING = { args -> 
	    def sb = new StringBuilder()
	    sb <<= "${args.label}"
	    if (args.errors) sb <<= "<br/><div>${args.errors}</div>"
	    sb <<= "${args.field}<br/>"
	    return sb.toString()
	}
	
    static final Closure DEFAULT_LABEL_RENDERING = { args ->
        "<label for=\"${args.fieldId}\" class=\"${args.errorClassToUse}\">${args.label}${args.required}</label>"
    }
	
	static final Map DEFAULT_PARAMS = Collections.unmodifiableMap([
		MAX_INPUT_DISPLAY_LENGTH : 60,
		ERROR_CLASS: "error",
		REQUIRED_INDICATOR: "*",
		SHOW_ERRORS: true,
		LABEL_TEMPLATE: DEFAULT_LABEL_RENDERING,
		CUSTOM_TEMPLATE: DEFAULT_FIELD_RENDERING,
		DATE_TEMPLATE: DEFAULT_FIELD_RENDERING,
		INPUT_TEMPLATE: DEFAULT_FIELD_RENDERING,
		SELECT_TEMPLATE: DEFAULT_FIELD_RENDERING,
		CHECKBOX_TEMPLATE: DEFAULT_FIELD_RENDERING,
		RADIO_TEMPLATE: DEFAULT_FIELD_RENDERING,
		COUNTRY_TEMPLATE: DEFAULT_FIELD_RENDERING,
		RADIOGROUP_TEMPLATE: DEFAULT_FIELD_RENDERING,
		TEXTAREA_TEMPLATE: DEFAULT_FIELD_RENDERING
	])

    def grailsApplication
    
	/**
	 * Return request-specific modified params, or global immutable params if none in request yet
	 */
	protected getTagParams() {
		def p = request[BEAN_PARAMS]
		if (p) {
		    return p
		} else {
		    def tagSettings = [:]
			request[BEAN_PARAMS] = tagSettings
			// Clone default closures
			DEFAULT_PARAMS.each { k, v ->
			    def code
			    if (v instanceof Closure) {
			        code = v.clone()
			    } else if (v instanceof GroovyPageTagBody) {
			        code = v.bodyClosure.clone()
			    }
		        tagSettings[k] = code != null ? code : v
			}
			return tagSettings
		}
    }

    /**
     * Set the template for labels
     */
    def labelTemplate = { attrs, body ->
        setParam('LABEL_TEMPLATE', body)
    }

    /**
     * Set the template for text input fields
     */
    def inputTemplate = { attrs, body -> 
        setParam('INPUT_TEMPLATE', body)
    }

    /**
     * Set the template for text input fields
     */
    def textAreaTemplate = { attrs, body -> 
        setParam('TEXTAREA_TEMPLATE', body)
    }

    /**
     * Set the template for a checkbox
     */
    def checkBoxTemplate = { attrs, body ->
        setParam('CHECKBOX_TEMPLATE', body)
    }

    /**
     * Set the template for radio button
     */
    def radioTemplate = { attrs, body ->
        setParam('RADIO_TEMPLATE', body)
    }

    /**
     * Set the template for radio groups
     */
    def radioGroupTemplate = { attrs, body ->
        setParam('RADIOGROUP_TEMPLATE', body)
    }

    /**
     * Set the template for date picker
     */
    def dateTemplate = { attrs, body ->
        setParam('DATE_TEMPLATE', body)
    }

    /**
     * Set the template for select box
     */
    def selectTemplate = { attrs, body ->
        setParam('SELECT_TEMPLATE', body)
    }

    /**
     * Set the template for custom render
     */
    def customTemplate = { attrs, body ->
        setParam('CUSTOM_TEMPLATE', body)
    }

    /**
     * Set the max length of any input field, even if constraints exceed it
     */
    def maxInputLength = { attrs, body ->
        setParam('MAX_INPUT_DISPLAY_LENGTH', body().toInteger())
    }

    /**
     * Set the CSS class to use when rendering errors
     */
    def errorClass = { attrs, body ->
        setParam('ERROR_CLASS', body())
    }

    /**
     * Set the template for a single error
     */
    def errorTemplate = { attrs, body ->
        setParam('ERROR_TEMPLATE', body)
    }

    /**
     * Set the "required" indicator html
     */
    def requiredIndicator = { attrs, body ->
        setParam('REQUIRED_INDICATOR', body() )
    }

    /**
     * Set whether or not errors are rendered inline
     */
    def showErrors = { attrs ->
        setParam('SHOW_ERRORS', attrs.value)
    }

    def label = { attrs ->
		def tagInfo = tagParams
        attrs.noLabel = false

		doTag( attrs, { renderParams ->
		    out << tagInfo.LABEL_TEMPLATE(renderParams)
	    })
    }
    
    def customField = { attrs, body ->
        def tagInfo = tagParams
		doTag( attrs, { renderParams ->

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.CUSTOM_TEMPLATE(label:label, field:body(),
				required:renderParams.mandatoryFieldFlagToUse, errors: errors)
		})
    }
    
    /**
     * Render the required indicator IF the specified bean and property are required
     */
    def required = { attrs ->
		def tagInfo = tagParams

		def originalPropertyPath = attrs.remove("property")
		def fieldName = originalPropertyPath

		def beanName = attrs.remove("beanName")

		assertBeanName(beanName)

		def mandatoryFieldIndicator = attrs.remove("mandatoryField")

		// Get the bean so we can get the current value and check for errors
		def bean = pageScope[beanName]

		def resolvedBeanInfo = getActualBeanAndProperty(bean, fieldName)
		bean = resolvedBeanInfo[0]
		fieldName = resolvedBeanInfo[1]

		if (isFieldMandatory(bean, fieldName)) {
			if (mandatoryFieldIndicator) { 
				out << mandatoryFieldIndicator
			} else {
				out << tagInfo.REQUIRED_INDICATOR
			}
		}
    }
    
    /**
	 * Set parameters relating to the g:modelXXX tags. These are remembered for the duration of the request
	 * using a trivial "copy on write" mechanism
     * Note: When the static params are copied, it is not a deep clone. Any static values will be used among multiple threads
     * so exercise caution if we ever add "complex" default parameters such as Closures, Maps or other objects
	 */
	private def setParam(String name, def value) {
        if(name == null)
            throwTagError("Cannot set parameter with no name spaceified")

		// Implement "copy on write" params
		def tagSettings = tagParams
		tagSettings[name] = value
        if (log.debugEnabled) log.debug "Set tag request parameter [$name] to [${value?.dump()}]"
	}

    /**
     * Set the beanName for all enclosed bean tags so you don't need to repeat it
     */
    def withBean = { attrs, body -> 
        if (!attrs.beanName) {
            throwTagError "Tag [withBean] requires attribute [beanName]"
        }
        if (!body) {
            throwTagError "Tag [withBean] requires a body"
        }
        tagParams['BEANNAME'] = attrs.beanName
        out << body()
        tagParams['BEANNAME'] = null
    }
    
    /**
     * Render a field for a bean property, using "smart" logic to work out what type to use
     * This is a convenience method, it is not particularly efficient!
     */
    def field = { attrs ->
        resolveBeanAndProperty(attrs)
        def fieldName = attrs._BEAN.fieldName
        def propertyType = attrs._BEAN.bean.metaClass.getMetaProperty(fieldName).type
	    
	    def isDomainClass = grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, propertyType)
	    
	    if (isDomainClass) {
	        out << select( attrs )
	    } else {
            def tagName = 'input'

            def constraints = attrs._BEAN.constraints?.get(fieldName)
            def inList = constraints?.inList
            
            if (Date.isAssignableFrom(propertyType)) {
                tagName = 'date'
            } else if (inList) {
                tagName = inList.size < 4 ? 'radioGroup' : 'select'
            } else if (Boolean.isAssignableFrom(propertyType)) {
                tagName = 'checkBox'
            } else if (String.isAssignableFrom(propertyType) && (constraints?.maxSize == 0 || constraints?.maxSize > 80)) {
                tagName = "textArea"
                attrs.cols = "40"
                attrs.rows = (Math.min(80,constraints?.maxSize) / 40).toString()
            }

            out << this."$tagName"(attrs)
        }
    }
	
	/**
     * Render an input field based on the value of a bean property.
     * Uses LABEL_TEMPLATE, INPUT_TEMPLATE and ERROR_TEMPLATE to render.
     * TODO: Make this use g:input or add support for arbitrary attributes and auto id= etc
	 */
	def input = { attrs ->
		def tagInfo = tagParams

		def fieldDisplaySize = getAttribute(attrs, "size", "")
		def fieldMaxLength = getAttribute(attrs, "maxLength", "")
		def type = getAttribute(attrs, "type", "text")
		def suppliedClass = getAttribute(attrs, "class", "")

		def sizeToUse = getAttributeToUse("size", fieldDisplaySize)
		def maxLengthToUse = getAttributeToUse("maxlength", fieldMaxLength)


		doTag( attrs, { renderParams ->

			/*
			 * Now see if we can set size and maxlength if not explicitly set
			 * based on any constraint in the domain object
			 */
			if (renderParams.beanConstraints) {
				def constrainedMaxLength = renderParams.beanConstraints[renderParams.propertyName]?.maxSize
				if (constrainedMaxLength) {
					if (!sizeToUse) {
						def maxDisplay = constrainedMaxLength
						if (maxDisplay > tagInfo.MAX_INPUT_DISPLAY_LENGTH) {
							maxDisplay = tagInfo.MAX_INPUT_DISPLAY_LENGTH
						}
						sizeToUse = 'size="' + maxDisplay + '"'
					}
					if (!maxLengthToUse) {
						maxLengthToUse = 'maxlength="' + constrainedMaxLength + '"'
					}
				}
			}

			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''
			
			def input = """<input class="${suppliedClass} ${renderParams.errorClassToUse}" type="${type}" ${sizeToUse} ${maxLengthToUse} ${renderParams.varArgs} name="${renderParams.fieldName}" value="${renderParams.fieldValue == null ? '' : renderParams.fieldValue.encodeAsHTML()}" />"""
			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			out << tagInfo.INPUT_TEMPLATE(label:label, field:input, required:
				renderParams.mandatoryFieldFlagToUse, errors: errors)
		})
	}

    /**
     * Render a select box from a bean property. Uses LABEL_TEMPLATE, SELECT_TEMPLATE and ERROR_TEMPLATE to render the field.
     * 
     * Attributes:
     *
     * from - The list of elements to select from, or a closure or null. If the field the select is for is a domain class,
     * a closure will be run to produce the from list - or if null it will run YourDomainClass.list(). If the field is not a domain class,
     * and from is null, it will be set to the inList or range constraint of the property if any is set.
     * 
     * Also accepts standard g:select attributes eg optionKey / optionValue
     */
	def select = { attrs ->
		def tagInfo = tagParams
		def overrideFrom = attrs.remove("from")
		def noSel = attrs.remove("noSelection")

		doTag( attrs, { renderParams ->
			def from = overrideFrom

            def fldname = renderParams.fieldName
            
            def checkValue = renderParams.fieldValue
            
            // If bean is a domain class, ask grails what kind of relationship it is
            // Hibernate proxies/shenanigans means that sometimes getMetaProperty might not be right
            // so we must do this instead
            def domainArtefact = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, renderParams.bean.class.name)
		    def prop = domainArtefact ? 
		        domainArtefact.getPropertyByName(renderParams.fieldName) : 
		        renderParams.bean.metaClass.getMetaProperty(renderParams.fieldName)
		    
		    // See if its an association
		    // @Todo add multiselect support for associations of Set and List
		    if (grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, prop.type)) {
		        if (from instanceof Closure) {
		            // Let caller apply some kind of logic/sort/criteria
		            from = overrideFrom()
	            } else if (from == null) {
		            from = prop.type.list() 
	            }
	            // Hack for Grails 1.1 bug requiring xxx.id assignment for selecting domain instances
	            fldname += '.id'
	            checkValue = renderParams.fieldValue?.id // Compare value to id
		    } else if (from == null) {
		        from = renderParams.beanConstraints?."${renderParams.propertyName}"?.inList
			    if (!from) {
			        from = renderParams.beanConstraints?."${renderParams.propertyName}"?.range
		        }
	        }

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

            if (!renderParams.mandatoryFieldFlagToUse) {
                attrs.noSelection = noSel
            }

			// Defer to form taglib
			attrs['name'] = fldname
			attrs['value'] = checkValue
			attrs['from'] = from

			def select = g.select(attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			out << tagInfo.SELECT_TEMPLATE(label:label, field:select,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors)
		})
	}

    /**
     * Render a date picker based on the value of a bean property.
     * Uses LABEL_TEMPLATE, DATE_TEMPLATE and ERROR_TEMPLATE to render.
     */
	def date = { attrs ->
		def tagInfo = tagParams

		doTag( attrs, { renderParams ->

			// Do label
			def labelParams = new HashMap(renderParams)
			labelParams.fieldName = labelParams.fieldName + "_day" // point label to day element
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			attrs['value'] = renderParams.fieldValue
			attrs['default'] = renderParams.defaultValue

			// @todo If min/max specified in constraints, pull out the years
			// attrs['years'] = ...

			def datePicker = datePicker(attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.DATE_TEMPLATE(label:label, field:datePicker,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors)
		})
	}

	/**
     * Render a text area based on the value of a bean property.
     * Uses LABEL_TEMPLATE, TEXTAREA_TEMPLATE and ERROR_TEMPLATE to render.
	 */
	def textArea = { attrs ->

		def tagInfo = tagParams

		doTag( attrs, { renderParams ->
			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			attrs['value'] = renderParams.fieldValue

			def textArea = g.textArea(attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.TEXTAREA_TEMPLATE(label:label, field:textArea,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors)
		})

	}

	/**
     * Render a checkbox based on the value of a bean property.
     * Uses LABEL_TEMPLATE, CHECKBOX_TEMPLATE and ERROR_TEMPLATE to render.
	 */
	def checkBox = { attrs ->
		def tagInfo = tagParams
		// Value is the value submitted to the server if the item is checked, not whether or not it is checked
        def cbSubmitValue = attrs.remove('value')
        
		doTag( attrs, { renderParams ->

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			// workaround grails 1.0.2 and earlier bug where null checked ticks the box
			attrs['checked'] = renderParams.fieldValue == null ? false : Boolean.valueOf(renderParams.fieldValue.toString())
			// Workaround for checkBox's ugly use of 'value' not for state but for submitted value to controller
			attrs.value = renderParams.fieldValue

			def checkBox = g.checkBox( attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.CHECKBOX_TEMPLATE(label:label, field:checkBox,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors)
		})
	}

	def radioGroup = { attrs ->
		def tagInfo = tagParams

        doTag( attrs, { renderParams ->

            renderParams.beanConstraints?.get(renderParams.fieldName).inList?.eachWithIndex() { currentValue, idx ->

    			// Defer to form taglib
    			attrs['name'] = renderParams.fieldName
    			attrs['id'] = renderParams.fieldName + "_" + idx
    			attrs['value'] = currentValue
    			attrs['checked'] = renderParams.fieldValue == currentValue

    			// Do label per field based on value
    			def labelParams = new HashMap(renderParams)
    			labelParams.fieldName = attrs['id']
    			labelParams.labelKey = renderParams.beanName +'.label.' + renderParams.fieldName + '.' + currentValue
    			labelParams.label = '' // clear what is there
    			labelParams.label = getLabelForField( labelParams, renderParams.beanName, renderParams.fieldName)
    			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

    			def r = g.radio( attrs)

    			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

    			// Use the current template closure if set
				out << tagInfo.RADIO_TEMPLATE(label:label, field:r,
					required:renderParams.mandatoryFieldFlagToUse, errors: errors)
    		}
		})
	}

	/**
     * Render a country selection box based on the value (ISO 3-digit country code) of a bean property.
     * Uses LABEL_TEMPLATE, COUNTRY_TEMPLATE and ERROR_TEMPLATE to render.
	 */
	def country = { attrs ->
		def tagInfo = tagParams

		doTag( attrs, { renderParams ->

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			attrs['value'] = renderParams.fieldValue
			attrs['default'] = renderParams.defaultValue

			def countrySelect = countrySelect( attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.COUNTRY_TEMPLATE( label:label, field:countrySelect,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors)
		})
	}

    /**
     * Tag to create instance of a bean if none exists in the model
     * Used to ensure constraints are available on a bean even if controller doesn't put it into the model,
     * for example when reusing fragments/includes
     * @TODO make this accept a map attribute that allows you to provide default values for properties of
     * the bean, for example to populate relationships
     */
    def require = { attrs ->
        def cls = attrs['className']
        def name = attrs.beanName
        if (!cls)
            throwTagError("requireBean tag requires attribute [className] indicating the class to instantiate")
        if (!name)
            throwTagError("requireBean tag requires attribute [beanName] indicating the class to instantiate")

        def bean = request.getAttribute(name)
        if (bean == null) {
            bean = ApplicationHolder.application.classLoader.loadClass(cls).newInstance()
            pageScope[name] = bean
        }
    }

    void assertBeanName(beanName) {
		if (!beanName) {
            throwTagError("All bean tags require attribute [beanName], or an enclosing [bean:form] tag with it defined there")
		}
    }

    /**
     * Resolve all the bean and property stuff using dot notation, defaulting to form bean
     * etc and store this info in the attribs so that this current invocation does not need to
     * resolve it any more
     */
    def resolveBeanAndProperty(attrs) {
        // Don't do this twice in an invocation
        if (attrs._BEAN) {
            return
        }
        attrs._BEAN = [:]
        
        // Resolve the property name and beanName
		attrs._BEAN.originalPropertyPath = attrs.remove("property")
		attrs._BEAN.fieldName = attrs._BEAN.originalPropertyPath
		// Use field name as id if none supplied
		if (attrs['id'] == null) {
			attrs['id'] = attrs._BEAN.originalPropertyPath
		}

		// Use bean name supplied, or fallback to that defined by current form tag
		def tagInfo = tagParams
		attrs._BEAN.beanName = attrs.remove("beanName") ?: tagInfo.BEANNAME
	    assertBeanName(attrs._BEAN.beanName)

		// Get the root bean so we can get the current value and check for errors
		attrs._BEAN.bean = pageScope[attrs._BEAN.beanName]

		if (attrs._BEAN.bean) {
    		def resolvedBeanInfo = getActualBeanAndProperty(attrs._BEAN.bean, attrs._BEAN.fieldName)
			attrs._BEAN.bean = resolvedBeanInfo[0]
			attrs._BEAN.fieldName = resolvedBeanInfo[1]
            attrs._BEAN.constraints = getBeanConstraints(attrs._BEAN.bean)
		}        
    }
    
	def doTag(def attrs, Closure renderPart) {
        resolveBeanAndProperty(attrs)

		def tagInfo = tagParams

		def bean = attrs._BEAN.bean
        def beanName = attrs._BEAN.beanName
        def fieldName = attrs._BEAN.fieldName

        if (bean == null)
            throwTagError("""All model tags require attribute [beanName] to resolve to a bean
in the model, but it is null. beanName was [${beanName}] and property was [${attrs._BEAN.originalPropertyPath}]""")

        // Now we have the bean and fieldName we can get one with things
        
		def showErrors = attrs.remove("showErrors")?.toBoolean()
		if (showErrors == null) {
			showErrors = tagInfo['SHOW_ERRORS']
		}
		def mandatoryFieldIndicator = attrs.remove("mandatoryField")
		

		def overrideValue = attrs.remove("value")
		def defaultValue = attrs.remove("default")

		def useValueCondition = getAttribute(attrs, "useValue", null)

		def useValue = true
		if (useValueCondition != null) {
			useValue = Boolean.valueOf(useValueCondition)
		}

        def useLabel = true
        if (attrs.noLabel != null) {
            useLabel = !attrs.remove('noLabel').toString().toBoolean()
        }
		def label = useLabel ? getLabelForField(attrs, beanName, fieldName) : null

		def hasFieldErrors = false
		def errorClassToUse = ""
		def mandatoryFieldFlagToUse = ""

		if (doesFieldHaveErrors(bean, fieldName)) {
			hasFieldErrors = true
			errorClassToUse = tagInfo.ERROR_CLASS
		}

		if (isFieldMandatory(bean, fieldName)) {
			if (mandatoryFieldIndicator) {
				mandatoryFieldFlagToUse = mandatoryFieldIndicator
			} else {
				mandatoryFieldFlagToUse = tagInfo.REQUIRED_INDICATOR
			}
		}

		def fieldValue = null

		// If there are errors or the value is to be used (see useValueCondition), set it up
		if (hasFieldErrors || modelHasErrors() || useValue) {
			if (overrideValue == null) {
				fieldValue = getFieldValue(bean, fieldName)
				if ((fieldValue == null) && defaultValue) {
					fieldValue = defaultValue
				} else if (fieldValue == null) {
					fieldValue = null
				}
			} else {
				fieldValue = overrideValue
			}
		}

        def origPropPath = attrs._BEAN.originalPropertyPath
        def constraints = attrs._BEAN.constraints
        
		// Get the optional args we do not need so we can echo 'as is'
		attrs.remove('_BEAN')
		def varArgs = ""
		attrs.each { k,v ->
			varArgs += k + "=\"" << v << "\" "
	    }

		def renderParams = [
			"errorClassToUse":errorClassToUse,
			"required":mandatoryFieldFlagToUse,
			"fieldName":origPropPath,
            "label":label,
            "fieldValue":fieldValue,
            "fieldId":attrs.id,
            "defaultValue":defaultValue,
			"varArgs":varArgs,
			"bean":bean,
			"beanConstraints":constraints,
			"beanName":beanName,
			"propertyName":fieldName,
			"errors": showErrors ? (bean?.metaClass?.hasProperty(bean, 'errors') ? bean?.errors?.getFieldErrors(fieldName) : null) : null
		]

		renderPart(renderParams)
	}

	private def doesFieldHaveErrors = {bean, fieldName ->
	    if (bean?.metaClass?.hasProperty(bean, 'errors')) {
		    return bean?.errors?.hasFieldErrors(fieldName)
	    } else return false
	}

	/**
	  * See if the field is mandatory
	  */
	private def isFieldMandatory = {bean, fieldName ->
    	def fieldIsMandatory = false

		// Watch out for Groovy bug here, don't combine lines
		def beanConstraints = getBeanConstraints(bean)
		if (beanConstraints) {
	        fieldIsMandatory |= !(beanConstraints[fieldName]?.nullable)
	        fieldIsMandatory |= !(beanConstraints[fieldName]?.blank)
	        fieldIsMandatory |= (beanConstraints[fieldName]?.minSize > 0)
        }
      	return fieldIsMandatory
	}

    private def getBeanConstraints(bean) {
		if (bean?.metaClass?.hasProperty(bean, 'constraints')) {
		    def cons = bean.constraints
		    if (cons != null) {
		        // Safety check for the case where bean is no a proper domain/command object
		        // This avoids confusing errors where constraints comes back as a Closuret
		        if (!(cons instanceof Map)) {
		            log.warning "Bean of type ${bean.class} is not a domain class, command object or other validateable object"
		        }
	        }
            return cons
	    } else return null
    }

	/**
	  * Get the actual bean and property name to use, following property path
	  *
	  * fieldName can be a simple fieldName (e.g. 'bookName') or compound (e.g. 'author.email').
	  */
	 def getActualBeanAndProperty = {bean, propertyPath ->
      	def actual = bean
		def propName = propertyPath
      	if(propName.indexOf('.')) {
        	def parts = propName.tokenize('.')
			(parts.size()-1).times() { index ->
      			actual = actual[parts[index]]
      		}
			propName = parts[parts.size()-1]
      	}
      	return [actual, propName]
	}
	/**
	  * Get the current value for a given field name on a bean.
	  *
	  * fieldName can be a simple fieldName (e.g. 'bookName') or compound (e.g. 'author.email').
	  */
	 def getFieldValue = {bean, fieldName ->
      	return bean[fieldName]
	}

    def buildErrors(def template, def errors) {
        def output = new StringBuffer()
        errors?.each() {
            if (template) {
                // @todo this could be more efficient but is a little tricky with exceptions
                output << template([error:it, message:getMessage(it)])
            }
            else {
                output << getMessage(it) + "<br/>"
            }
        }
        return output
    }

    def getMessage( messageKey, boolean failOnError = true ) {
        def appContext = grailsAttributes.getApplicationContext()
        def messageSource = appContext.getBean("messageSource")
        def locale = RCU.getLocale(request)
        def message = ((messageKey instanceof String) || (messageKey instanceof GString)) ? 
            (failOnError ? messageSource.getMessage( messageKey, null, locale) : 
                messageSource.getMessage( messageKey, null, null, locale)) : 
            messageSource.getMessage( messageKey, locale)
        return message
    }

	/*
	 * Get the label for the field:-
	 *   - if 'label' attribute is specified use it 'as is'
	 *   - otherwise if 'labelKey' attribute is specified use it
	 *     as the key for an 118N'd message
	 *   - otherwise use the convention of <beanName>.<fieldName>
	 *     as the key for an 118N'd message
	 *   - if still null use the convention of <Field Name>
	*/
	def getLabelForField = {attrs, beanName, fieldName ->
		def label = attrs.remove("label")
		def labelKey = attrs.remove("labelKey")
		if (label == null) {
			if (!labelKey) {
				labelKey = beanName + "." + fieldName
			}
			label = getMessage(labelKey, false)
		}
		if (label == null) {
			label = GrailsClassUtils.getNaturalName(fieldName)
		}
		return label
	}

	def getAttribute(attrs, attrName, defaultValue) {
		def returnValue = attrs.remove(attrName)
		if (returnValue == null) {
			returnValue = defaultValue
		}
		return returnValue
	}

	def getAttributeToUse(attrName, attrValue) {
		def attributeToUse = ""
		if (attrValue) {
			attributeToUse = attrName + '="' + attrValue + '"'
		}
		return attributeToUse
	 }

	private getModelBeanProperty(propertyPath) {
		def info = getActualBeanAndProperty(request, propertyPath)
		return info[0][info[1]]
	}

	/**
	 * Check if there were any objects in the model with errors
	 */
	private boolean modelHasErrors() {
		def errors = false
		if(request.attributeNames) {
			request.attributeNames.find { name ->
				if (checkForErrors(request[name])) {
					errors = true
					return true
				}
			}
        }
		return errors
    }

	private boolean checkForErrors(object) {
		if(object) {
	        if(object instanceof Errors) {
	        	if (object.hasErrors()) {
					return true
				}
	        } else if (object?.metaClass?.hasProperty(object, 'errors') instanceof Errors) {
	        	if (object?.errors.hasErrors()) {
					return true
				}
			}
		}
		return false
	}
}
