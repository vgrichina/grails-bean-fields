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

// @todo Add bean:autoForm that takes a bean name and controller/action/url etc and automatically
// renders the whole form for all properties. Option to include/exclude certain props
// @todo Make all tags pass through unrecognized attributes such as "id"
// @todo Make date tag support min and max values for date fields, in terms of the years shown
// @todo Make country tag support inList constraint to constrain the list of permitted countries

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
	static final String BEAN_TEMPLATES_BACKUP = '_bean_taglib_templates_backup'

    static final SUBSCRIPT_PATTERN = /(\w+)\[(\d+)\]/
    
    static final Closure DEFAULT_FIELD_RENDERING = { args -> 
	    def sb = new StringBuilder()
	    sb <<= "${args.label}"
	    if (args.errors) sb <<= "<br/><div>${args.errors}</div>"
	    sb <<= "${args.field}<br/>"
	    return sb.toString()
	}
	
    static final Closure DEFAULT_RADIOGROUPITEM_RENDERING = { args -> 
	    def sb = new StringBuilder()
	    sb <<= "${args.field}<label for=\"${args.fieldId}\">${args.label.encodeAsHTML()}</label><br/>"
	    return sb.toString()
	}

    static final Closure DEFAULT_RADIOGROUP_RENDERING = { args -> 
	    def sb = new StringBuilder()
	    sb <<= "${args.label}"
	    if (args.errors) sb <<= "<br/><div>${args.errors}</div>"
	    sb <<= "${args.field}"
	    return sb.toString()
	}
	
	static final Closure DEFAULT_LABEL_RENDERING = { args ->
        "<label for=\"${args.fieldId}\" class=\"${args.labelClass} ${args.errorClassToUse}\">${args.label.encodeAsHTML()}${args.required}</label>"
    }
	
	static final Map DEFAULT_PARAMS = Collections.unmodifiableMap([
		MAX_INPUT_DISPLAY_LENGTH : 60,
		ERROR_CLASS: "error",
		LABEL_CLASS: '',
		MAX_AUTO_RADIO_BUTTONS: 4,
		REQUIRED_INDICATOR: "*",
		SHOW_ERRORS: true,
		LABEL_TEMPLATE: DEFAULT_LABEL_RENDERING,
		CUSTOM_TEMPLATE: DEFAULT_FIELD_RENDERING,
		DATE_TEMPLATE: DEFAULT_FIELD_RENDERING,
		INPUT_TEMPLATE: DEFAULT_FIELD_RENDERING,
		SELECT_TEMPLATE: DEFAULT_FIELD_RENDERING,
		CHECKBOX_TEMPLATE: DEFAULT_FIELD_RENDERING,
		RADIO_TEMPLATE: DEFAULT_FIELD_RENDERING,
		RADIOGROUPITEM_TEMPLATE: DEFAULT_RADIOGROUPITEM_RENDERING,
		COUNTRY_TEMPLATE: DEFAULT_FIELD_RENDERING,
		RADIOGROUP_TEMPLATE: DEFAULT_RADIOGROUP_RENDERING,
		RADIOLABEL_TEMPLATE: DEFAULT_LABEL_RENDERING,
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
            // Copy default closures, MUST be cloned before calling
			DEFAULT_PARAMS.each { k, v ->
		        tagSettings[k] = v
			}
			return tagSettings
		}
    }

    def backupTemplates = { attrs -> 
        if (request[BEAN_TEMPLATES_BACKUP]) {
            throw new IllegalStateException('Cannot backup templates, they have not been restored yet and we do do not support nested backups sorry!')
        }
        
        def backup = [:]
        request[BEAN_TEMPLATES_BACKUP] = backup
        tagParams.each { k, v ->
            if (k.endsWith('_TEMPLATE')) {
                backup[k] = v
            }
        }
    }
    
    def restoreTemplates = { attrs -> 
        if (request[BEAN_TEMPLATES_BACKUP]) {
            tagParams.putAll(request[BEAN_TEMPLATES_BACKUP])
            request[BEAN_TEMPLATES_BACKUP] = null
        }
    }

    def maxAutoRadioButtons = { attrs, body -> 
        setParam('MAX_AUTO_RADIO_BUTTONS', body().toString().toInteger())
    }
    
    /**
     * Set the template for labels
     */
    def labelTemplate = { attrs, body ->
        setParam('LABEL_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for text input fields
     */
    def inputTemplate = { attrs, body -> 
        setParam('INPUT_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for text input fields
     */
    def textAreaTemplate = { attrs, body -> 
        setParam('TEXTAREA_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for text input fields
     */
    def countryTemplate = { attrs, body -> 
        setParam('COUNTRY_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for a checkbox
     */
    def checkBoxTemplate = { attrs, body ->
        setParam('CHECKBOX_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for radio button
     */
    def radioTemplate = { attrs, body ->
        setParam('RADIO_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for radio groups
     */
    def radioGroupTemplate = { attrs, body ->
        setParam('RADIOGROUP_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for radio group items
     */
    def radioGroupItemTemplate = { attrs, body ->
        setParam('RADIOGROUPITEM_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for the label for radios
     */
    def radioLabelTemplate = { attrs, body ->
        setParam('RADIOLABEL_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for date picker
     */
    def dateTemplate = { attrs, body ->
        setParam('DATE_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for select box
     */
    def selectTemplate = { attrs, body ->
        setParam('SELECT_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the template for custom render
     */
    def customTemplate = { attrs, body ->
        setParam('CUSTOM_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
    }

    /**
     * Set the max length of any input field, even if constraints exceed it
     */
    def maxInputLength = { attrs, body ->
        setParam('MAX_INPUT_DISPLAY_LENGTH', body().toInteger())
    }

    /**
     * Set the CSS class to use when rendering labels
     */
    def labelClass = { attrs, body ->
        setParam('LABEL_CLASS', body())
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
        setParam('ERROR_TEMPLATE', body instanceof Closure ? body : body.@bodyClosure)
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
		    out << tagInfo.LABEL_TEMPLATE.clone().call(renderParams)
	    })
    }
    
    def customField = { attrs, body ->
        def tagInfo = tagParams
		doTag( attrs, { renderParams ->

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if setParam
			out << tagInfo.CUSTOM_TEMPLATE.clone().call(label:label, field:body(),
				required:renderParams.mandatoryFieldFlagToUse, errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
		})
    }
    
    /**
     * Render just the errors for a specific field
     */
    def fieldErrors = { attrs, body ->
        def tagInfo = tagParams
		doTag( attrs, { renderParams ->
			out << buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)
		})
    }
    
    def form = { attrs -> 
        def beanName = attrs.beanName
        if (!beanName) {
            throwTagError "Tag [form] requires attribute [beanName]"
        }
        def props = attrs.properties
        if (!props) {
            throwTagError "Tag [form] requires attribute [properties] containing a list of property names (comma-delimited or List)"
        }
        if (!(props instanceof List)) {
            props = props.toString().tokenize(',')
        }
        props.each { p ->
            out << field(beanName:beanName, property:p.trim())
        }
    }
    
    /**
     * Render the required indicator IF the specified bean and property are required
     */
    def required = { attrs ->
		def tagInfo = tagParams

		def originalPropertyPath = attrs.remove("property")

		def beanName = attrs.remove("beanName")

		assertBeanName(beanName)

		def mandatoryFieldIndicator = attrs.remove("mandatoryField")

		// Get the bean so we can get the current value and check for errors
		def bean = pageScope.variables[beanName]

		def resolvedBeanInfo = getActualBeanAndProperty(bean, attrs.valueOverride, originalPropertyPath)

		if (isFieldMandatory(resolvedBeanInfo.bean, resolvedBeanInfo.propertyName, attrs.constraints)) {
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
        def tagInfo = tagParams
        resolveBeanAndProperty(attrs)
        def propName = attrs._BEAN.propertyName // get the terminating property name minus any subscript!
        def domainArtefact = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, attrs._BEAN.bean.class.name)
        def propertyType
	    def isDomainClass
	    // Use GrailsDomainClass persistent properties to find out if this is an association,
	    // as beans might be proxies with property type Object for association columns, which breaks our logic
        if (domainArtefact) {
            def persistentProp = domainArtefact.getPropertyByName(propName)
            propertyType = persistentProp.type
    	    isDomainClass = persistentProp.association
        } else {
            propertyType = attrs._BEAN.bean.metaClass.getMetaProperty(propName).type
    	    isDomainClass = grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, propertyType)
        }
	    
	    if (isDomainClass) {
	        out << select( attrs )
	    } else {
            def tagName = 'input'

            def constraints = attrs._BEAN.constraints?.get(propName)
            def inList = constraints?.inList
            
            if (Date.isAssignableFrom(propertyType)) {
                tagName = 'date'
            } else if (Number.isAssignableFrom(propertyType)) {
                if (!attrs['size']) {
                    attrs['size'] = 10
                }
            } else if (inList) {
                tagName = (inList.size < tagInfo.MAX_AUTO_RADIO_BUTTONS) ? 'radioGroup' : 'select'
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
    
    def hidden = { attrs ->
        attrs.type = "hidden"
        attrs.noLabel = true
        out << input(attrs)
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

			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''
			
			def input = """<input class="${suppliedClass} ${renderParams.errorClassToUse}" type="${type}" ${sizeToUse} ${maxLengthToUse} ${renderParams.varArgs} name="${renderParams.fieldName}" value="${renderParams.fieldValue == null ? '' : renderParams.fieldValue.encodeAsHTML()}" />"""
			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			out << tagInfo.INPUT_TEMPLATE.clone().call(label:label, 
			    field:input, 
			    required:renderParams.mandatoryFieldFlagToUse, 
			    errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
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
            // NOTE: using class NAME here because for some Java domain classes this returns FALSE if you pass in the class (grails 1.3.1)
            def domainArtefact = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, renderParams.bean.class.name)
		    def propType
		    if (domainArtefact) {
		        def prop = domainArtefact.getPropertyByName(renderParams.propertyName)
		        propType = prop.referencedPropertyType ?: prop.type 
	        } else {
		        propType = renderParams.bean.metaClass.getMetaProperty(renderParams.propertyName)
	        }
		    
		    // See if its an association
		    // @Todo add multiselect support for associations of Set and List
		    if (grailsApplication.isArtefactOfType(DomainClassArtefactHandler.TYPE, propType.name)) {
		        if (from instanceof Closure) {
		            // Let caller apply some kind of logic/sort/criteria
		            from = overrideFrom()
	            } else if (from == null) {
		            from = propType.list() 
	            }
	            // Hack for Grails 1.1 bug requiring xxx.id assignment for selecting domain instances
	            fldname += '.id'
	            checkValue = renderParams.fieldValue?.ident() // Compare value to id
	            attrs.optionKey = { obj -> obj.ident() } // id field is the key
		    } else if (from == null) {
		        from = renderParams.beanConstraints?."${renderParams.propertyName}"?.inList
			    if (!from) {
			        from = renderParams.beanConstraints?."${renderParams.propertyName}"?.range
		        }
	        }

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

            if (!renderParams.mandatoryFieldFlagToUse) {
                attrs.noSelection = noSel
            }

			// Defer to form taglib
			attrs['name'] = fldname
			attrs['value'] = checkValue
			attrs['from'] = from

			def select = g.select(attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			out << tagInfo.SELECT_TEMPLATE.clone().call(label:label, field:select,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
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
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			attrs['value'] = renderParams.fieldValue
			attrs['default'] = renderParams.defaultValue

			// @todo If min/max specified in constraints, pull out the years
			// attrs['years'] = ...

			def datePicker = datePicker(attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.DATE_TEMPLATE.clone().call(label:label, field:datePicker,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
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
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			attrs['value'] = renderParams.fieldValue

			def textArea = g.textArea(attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.TEXTAREA_TEMPLATE.clone().call(label:label, field:textArea,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
		})

	}

	/**
     * Render a checkbox based on the value of a bean property.
     * Uses LABEL_TEMPLATE, CHECKBOX_TEMPLATE and ERROR_TEMPLATE to render.
	 */
	def checkBox = { attrs ->
		def tagInfo = tagParams
		// Value is the value submitted to the server if the item is checked, not whether or not it is checked
		// @todo This breaks using "value" to override the bean's property value
        def cbSubmitValue = attrs.remove('value')
        
		doTag( attrs, { renderParams ->

			// Do label
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			// workaround grails 1.0.2 and earlier bug where null checked ticks the box
			attrs['checked'] = renderParams.fieldValue == null ? false : Boolean.valueOf(renderParams.fieldValue.toString())
			// Workaround for checkBox's ugly use of 'value' not for state but for submitted value to controller
			attrs.value = renderParams.fieldValue

			def checkBox = g.checkBox( attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.CHECKBOX_TEMPLATE.clone().call(label:label, field:checkBox,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
		})
	}

    /**
     * Render a set of radio buttons to represent a range of permitted values (eg. single select)
     *
     * Result is structured like this:
     * MAIN LABEL
     * N * (OPTION LABEL + WIDGET)
     *
     * The special RADIOGROUP_TEMPLATE is used to layout these elements, so that the interaction between the main
     * label and widget "rows" can be modified
     */
	def radioGroup = { attrs ->
		def tagInfo = tagParams

        doTag( attrs, { renderParams ->

            // Write out label for the whole group
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

            def widgetPart = new StringBuilder()
            
            renderParams.beanConstraints?.get(renderParams.propertyName).inList?.eachWithIndex() { currentValue, idx ->

                def tempAttrs = [:] + attrs
    			// Defer to form taglib
    			tempAttrs['name'] = renderParams.fieldName
    			tempAttrs['id'] = renderParams.fieldName + "_" + idx
    			tempAttrs['value'] = currentValue
    			tempAttrs['checked'] = renderParams.fieldValue == currentValue

    			// Do label per field based on value
    			def labelParams = new HashMap(renderParams)
    			labelParams.required = '' 
     			labelParams.fieldName = tempAttrs['id']
    			labelParams.labelKey = renderParams.beanName + '.' + renderParams.propertyPath + '.' + currentValue
    			labelParams.fieldId = tempAttrs['id'] // so "for" is correct
    			// Get label using INLIST CONSTRAINT CURRENT VALUE as the fallback label
    			// Pass null as current label as this is per-field labelling which requires the value as part of the key
    			labelParams.label = getLabelForField( null, labelParams.labelKey, renderParams.propertyPath + '.' + currentValue)
    			def optionlabel = labelParams.label

    			def r = g.radio( tempAttrs)

    			// Use the current template closure if set
				widgetPart << tagInfo.RADIOGROUPITEM_TEMPLATE.clone().call(label:optionlabel, field:r,
    			    bean: renderParams.bean,
        			fieldId: tempAttrs['id'], // so "for" is correct
    			    beanName: renderParams.beanName,
    			    labelKey: renderParams.labelKey,
    			    propertyName: renderParams.propertyName)
    		}
    		
    		out << tagInfo.RADIOGROUP_TEMPLATE.clone().call(
    		    label:label, 
    		    field:widgetPart,
				required:renderParams.mandatoryFieldFlagToUse, 
				errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
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
			def label = renderParams.label ? tagInfo.LABEL_TEMPLATE.clone().call(renderParams) : ''

			// Defer to form taglib
			attrs['name'] = renderParams.fieldName
			attrs['value'] = renderParams.fieldValue
			attrs['default'] = renderParams.defaultValue

			def countrySelect = countrySelect( attrs)

			def errors = buildErrors( tagInfo.ERROR_TEMPLATE, renderParams.errors)

			// Use the current template closure if set
			out << tagInfo.COUNTRY_TEMPLATE.clone().call( label:label, field:countrySelect,
				required:renderParams.mandatoryFieldFlagToUse, errors: errors,
			    bean: renderParams.bean,
			    beanName: renderParams.beanName,
			    labelKey: renderParams.labelKey,
			    propertyName: renderParams.propertyName)
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

        def bean = pageScope.variables[name]
        if (bean == null) {
            bean = ApplicationHolder.application.classLoader.loadClass(cls).newInstance()
            pageScope[name] = bean
        }
    }

    void assertBeanName(beanName) {
		if (!beanName) {
            throwTagError("All bean field tags require attribute [beanName] tag with it defined there")
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
		attrs._BEAN.propertyName = attrs._BEAN.originalPropertyPath
		// Use field name as id if none supplied
		if (attrs['id'] == null) {
			attrs['id'] = attrs._BEAN.originalPropertyPath
		}

		// Use bean name supplied, or fallback to that defined by current form tag
		def tagInfo = tagParams
		attrs._BEAN.beanName = attrs.remove("beanName") ?: tagInfo.BEANNAME
	    assertBeanName(attrs._BEAN.beanName)

		// Get the root bean so we can get the current value and check for errors
		// The user can override with bean="${whatever}" if they really know what they are doing
		attrs._BEAN.bean = attrs.remove('bean') ?: pageScope.variables[attrs._BEAN.beanName]

        // Get the value override if there is one
		attrs._BEAN.value = attrs.remove('valueOverride')

		// If still not resolved to an instance, see if we can create it
	    def cls = attrs.remove('className')
		if (!attrs._BEAN.bean && cls) {
		    attrs._BEAN.bean = ApplicationHolder.application.classLoader.loadClass(cls).newInstance()
		}

		if (attrs._BEAN.bean) {
		    // Only go down the property path if the user did NOT override the value
		    def resolvedBeanInfo = getActualBeanAndProperty(attrs._BEAN.bean, attrs._BEAN.value, attrs._BEAN.propertyName)
		    attrs._BEAN.putAll(resolvedBeanInfo)
            attrs._BEAN.constraints = attrs.remove('constraints') ?: getBeanConstraints(attrs._BEAN.bean)
		}        
    }
    
	def doTag(def attrs, Closure renderPart) {
        resolveBeanAndProperty(attrs)

		def tagInfo = tagParams

        // Get the pre-resolved info for this bean and property
		def bean = attrs._BEAN.bean // the endpoint bean, not the original root level one!
        def beanName = attrs._BEAN.beanName // the name of the originbal root level bean
        def propertyName = attrs._BEAN.propertyName
        def beanPropertyValue = attrs._BEAN.value
        
        if (bean == null)
            throwTagError("""All model tags require attribute [beanName] to resolve to a bean
in the model, but it is null. beanName was [${beanName}] and property was [${attrs._BEAN.originalPropertyPath}]""")

        // Now we have the bean and property Name we can get one with things
        
		def showErrors = attrs.remove("showErrors")?.toBoolean()
		if (showErrors == null) {
			showErrors = tagInfo['SHOW_ERRORS']
		}
		def mandatoryFieldIndicator = attrs.remove("requiredField")
		
		def nameOverride = attrs.remove("name")

		def defaultValue = attrs.remove("default")
        def overrideConstraints = attrs.remove('constraints')
        
		def useValueCondition = getAttribute(attrs, "useValue", null)

		def useValue = true
		if (useValueCondition != null) {
			useValue = Boolean.valueOf(useValueCondition)
		}

        def cleanPropertyPath = attrs._BEAN.propertyPath
        def originalPropertyPath = attrs._BEAN.originalPropertyPath

        def useLabel = true
        if (attrs.noLabel != null) {
            useLabel = !attrs.remove('noLabel').toString().toBoolean()
        }

		def label = attrs.remove('label')
        def labelClass
	    def labelKey = getLabelKeyForField(attrs.remove("labelKey"), beanName, cleanPropertyPath)
		if (useLabel) {
			label = getLabelForField( label, labelKey, originalPropertyPath)
			labelClass = attrs.remove('labelClass') ?: tagInfo.LABEL_CLASS
		} 

		def hasFieldErrors = false
		def errorClassToUse = ""
		def mandatoryFieldFlagToUse = ""

		if (doesFieldHaveErrors(bean, propertyName)) {
			hasFieldErrors = true
			errorClassToUse = tagInfo.ERROR_CLASS
		}

		if (isFieldMandatory(bean, propertyName, overrideConstraints)) {
			if (mandatoryFieldIndicator != null) {
				mandatoryFieldFlagToUse = mandatoryFieldIndicator
			} else {
				mandatoryFieldFlagToUse = tagInfo.REQUIRED_INDICATOR
			}
		}

		def fieldValue = null

		// If there are errors or the value is to be used (see useValueCondition), set it up
		if (hasFieldErrors || modelHasErrors() || useValue) {
			fieldValue = beanPropertyValue
			if ((fieldValue == null) && defaultValue) {
				fieldValue = defaultValue
			}
		}
		
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
			"fieldName":nameOverride ?: originalPropertyPath, // original full dotted and subscripted path
            "label":label,
            "fieldValue":fieldValue, 
            "fieldId":attrs.id,
            "defaultValue":defaultValue,
			"varArgs":varArgs,
			"bean":bean, // The endpoint bean 
			"beanConstraints":constraints,
			"beanName":beanName,
			"propertyName":propertyName,  // Final endpoint bean's property name without subscripts
			"propertyPath":cleanPropertyPath,  // Full dotted but non-subscripted property path
			"labelKey":labelKey,
			"labelClass":labelClass,
			"errors": showErrors ? (bean?.metaClass?.hasProperty(bean, 'errors') ? bean?.errors?.getFieldErrors(propertyName) : null) : null
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
	private def isFieldMandatory = {bean, fieldName, constraints ->
    	def fieldIsMandatory = false

		// Watch out for Groovy bug here, don't combine lines
		def beanConstraints = constraints ?: getBeanConstraints(bean)
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
                if (log.debugEnabled) {
                    log.debug "Bean is of type ${bean.class} - the constraints property was a [${cons.class}]"
                }
                
		        // Safety check for the case where bean is no a proper domain/command object
		        // This avoids confusing errors where constraints comes back as a Closure
		        if (!(cons instanceof Map)) {
    	            if (log.warnEnabled) {
		                log.warn "Bean of type ${bean.class} is not a domain class, command object or other validateable object - the constraints property was a [${cons.class}]"
	                }
		        }
	        } else {
	            if (log.warnEnabled) {
	                log.warn "Bean of type ${bean.class} has no constraints"
                }
	        }
            return cons
	    } else return null
    }

	/**
	  * Get the actual bean and property name to use, following property path
	  *
	  * fieldName can be a simple fieldName (e.g. 'bookName') or compound (e.g. 'author.email') or 'book.authors[2].email'
	  */
	 def getActualBeanAndProperty(bean, valueOverride, propertyPath) {
      	
      	// The final endpoint bean
      	def actual = bean
		
		// The final endpoint bean's property name, excluding the subscript if any
		def propName
        
        // The final value
        def value = valueOverride
        
		// Split the property path eg x.y[4].authors[3].email into the component parts
    	def parts = propertyPath.tokenize('.')
    	def last = parts.size()-1
    	
    	// Stores the property names in the path, without subscripts (for label keys)
    	def propPath = []
    	
    	// Loop over the parts of the property path and resolve the actual final object to get property from
    	parts.eachWithIndex { pn, idx ->
		    def subscriptMatch = (pn =~ SUBSCRIPT_PATTERN) 
		    // If theere was a subscript operator, dereference it
		    if (subscriptMatch) {
                def nameWithNoSubscript = subscriptMatch[0][1]
        	    if (idx < last) {
                    if (valueOverride == null) {
		                actual = actual[nameWithNoSubscript][subscriptMatch[0][2].toInteger()]
	                }
	            } else {
                    if (valueOverride == null) {
		                value = actual[nameWithNoSubscript][subscriptMatch[0][2].toInteger()]
	                }
    	            propName = nameWithNoSubscript
	            }
	            propPath << nameWithNoSubscript
		    } else {
        	    if (idx < last) {
                    if (valueOverride == null) {
  			            actual = actual[pn]
		            }
		        } else {
                    if (valueOverride == null) {
		                value = actual[pn]
	                }
    	            propName = pn
		        }
	            propPath << pn
			}
    	    if (idx == last) {
	        }
	    }
  		// Update our final bean's property name

      	return [bean:actual, propertyName:propName, value:value, propertyPath: propPath.join('.')]
	}

    def buildErrors(def template, def errors) {
        def output = new StringBuffer()
        errors?.each() {
            if (template) {
                // @todo this could be more efficient but is a little tricky with exceptions
                output << template.clone().call([error:it, message:getMessage(it)])
            }
            else {
                output << getMessage(it).encodeAsHTML() + "<br/>"
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

	def getLabelKeyForField = { labelKey, beanName, propertyName -> 
		if (!labelKey) {
			labelKey = beanName + "." + propertyName
		}
		return labelKey
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
	def getLabelForField = { label, labelKey, propPath ->
	    // deliberately check for null - label = '' means "no label thanks!"
		if (label == null) { 
			label = getMessage(labelKey, false)
		}
		if (label == null) {
			label = (propPath.tokenize('.').collect { pn ->
    		    def subscriptMatch = (pn =~ SUBSCRIPT_PATTERN) 
			    if (subscriptMatch) {
			        return GrailsClassUtils.getNaturalName(subscriptMatch[0][1])+' '+subscriptMatch[0][2]
			    } else {
			        return GrailsClassUtils.getNaturalName(pn)
		        }
		    }).join(' ')
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
