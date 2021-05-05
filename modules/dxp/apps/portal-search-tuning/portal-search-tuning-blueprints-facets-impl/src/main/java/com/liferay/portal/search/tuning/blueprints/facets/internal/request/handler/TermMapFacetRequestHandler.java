/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.liferay.portal.search.tuning.blueprints.facets.internal.request.handler;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.search.tuning.blueprints.engine.attributes.BlueprintsAttributes;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.Parameter;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.StringArrayParameter;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.facets.internal.util.FacetConfigurationUtil;
import com.liferay.portal.search.tuning.blueprints.facets.spi.request.FacetRequestHandler;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONValidationUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=term_map",
	service = FacetRequestHandler.class
)
public class TermMapFacetRequestHandler
	extends BaseFacetRequestHandler implements FacetRequestHandler {

	public Optional<Parameter> getParameterOptional(
		BlueprintsAttributes blueprintsAttributes, Messages messages,
		JSONObject jsonObject) {

		if (!_validateConfiguration(jsonObject, messages)) {
			Optional.empty();
		}

		String parameterName = FacetConfigurationUtil.getParameterName(
			jsonObject);

		Optional<Object> valueOptional =
			blueprintsAttributes.getAttributeOptional(parameterName);

		if (!valueOptional.isPresent()) {
			return Optional.empty();
		}

		String[] valueArray;

		if (isMultiValue(jsonObject)) {
			valueArray = GetterUtil.getStringValues(valueOptional.get());
		}
		else {
			valueArray = new String[] {
				GetterUtil.getString(valueOptional.get())
			};
		}

		List<String> values = _getValues(valueArray, jsonObject);

		if (values.isEmpty()) {
			return Optional.empty();
		}

		Stream<String> stream = values.stream();

		Parameter parameter = new StringArrayParameter(
			parameterName, null, stream.toArray(String[]::new));

		return Optional.of(parameter);
	}

	private List<String> _getValues(
		String[] valueArray, JSONObject jsonObject) {

		List<String> values = new ArrayList<>();

		JSONObject handlerParametersJSONObject = jsonObject.getJSONObject(
			FacetConfigurationKeys.HANDLER_PARAMETERS.getJsonKey());

		JSONArray mapJSONArray = handlerParametersJSONObject.getJSONArray(
			"map");

		for (String requestValue : valueArray) {
			JSONArray translatedValuesJSONArray = null;

			for (int i = 0; i < mapJSONArray.length(); i++) {
				try {
					JSONObject itemJSONObject = mapJSONArray.getJSONObject(i);

					if (itemJSONObject.getString(
							"key"
						).equals(
							requestValue
						)) {

						translatedValuesJSONArray = itemJSONObject.getJSONArray(
							"values");

						break;
					}
				}
				catch (Exception exception) {
					_log.error(exception.getMessage(), exception);
				}
			}

			if ((translatedValuesJSONArray != null) &&
				(translatedValuesJSONArray.length() > 0)) {

				Collections.addAll(
					values, JSONUtil.toStringArray(translatedValuesJSONArray));
			}
			else {
				values.add(requestValue);
			}
		}

		return values;
	}

	private boolean _validateConfiguration(
		JSONObject jsonObject, Messages messages) {

		JSONObject handlerParametersJSONObject = jsonObject.getJSONObject(
			FacetConfigurationKeys.HANDLER_PARAMETERS.getJsonKey());

		if (handlerParametersJSONObject == null) {
			MessagesUtil.requiredFieldMissingError(
				messages, getClass().getName(), jsonObject,
				FacetConfigurationKeys.HANDLER_PARAMETERS.getJsonKey());

			return false;
		}

		return BlueprintJSONValidationUtil.validateRequiredFieldsPresent(
			getClass().getName(), handlerParametersJSONObject, messages,
			"mappings");
	}

	private static final Log _log = LogFactoryUtil.getLog(
		TermMapFacetRequestHandler.class);

}