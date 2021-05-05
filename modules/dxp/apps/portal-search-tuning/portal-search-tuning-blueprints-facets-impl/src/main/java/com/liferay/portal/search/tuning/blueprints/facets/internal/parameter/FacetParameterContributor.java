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

package com.liferay.portal.search.tuning.blueprints.facets.internal.parameter;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.search.tuning.blueprints.engine.attributes.BlueprintsAttributes;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.Parameter;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterDataBuilder;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterDefinition;
import com.liferay.portal.search.tuning.blueprints.engine.spi.parameter.ParameterContributor;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetsBlueprintKeys;
import com.liferay.portal.search.tuning.blueprints.facets.internal.request.handler.FacetRequestHandlerFactory;
import com.liferay.portal.search.tuning.blueprints.facets.spi.request.FacetRequestHandler;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.util.BlueprintHelper;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONValidationUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=facets",
	service = ParameterContributor.class
)
public class FacetParameterContributor implements ParameterContributor {

	@Override
	public void contribute(
		ParameterDataBuilder parameterDataBuilder, Blueprint blueprint,
		BlueprintsAttributes blueprintsAttributes, Messages messages) {

		Optional<JSONArray> optional =
			_blueprintHelper.getJSONArrayConfigurationOptional(
				blueprint,
				"JSONArray/" + FacetsBlueprintKeys.CONFIGURATION_SECTION);

		if (!optional.isPresent()) {
			return;
		}

		JSONArray jsonArray = optional.get();

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			if (!_isEnabled(jsonObject) ||
				!_validateFacetConfiguration(messages, jsonObject)) {

				continue;
			}

			_parseFacetParameter(
				parameterDataBuilder, blueprintsAttributes, messages,
				jsonObject);
		}
	}

	@Override
	public String getCategoryNameKey() {
		return "facets";
	}

	@Override
	public List<ParameterDefinition> getParameterDefinitions() {
		return new ArrayList<>();
	}

	private boolean _isEnabled(JSONObject jsonObject) {
		return jsonObject.getBoolean(
			FacetConfigurationKeys.ENABLED.getJsonKey(), true);
	}

	private void _parseFacetParameter(
		ParameterDataBuilder parameterDataBuilder,
		BlueprintsAttributes blueprintsAttributes, Messages messages,
		JSONObject jsonObject) {

		String handler = jsonObject.getString(
			FacetConfigurationKeys.HANDLER.getJsonKey(), "default");

		try {
			FacetRequestHandler facetRequestHandler =
				_facetRequestHandlerFactory.getHandler(handler);

			Optional<Parameter> parameter =
				facetRequestHandler.getParameterOptional(
					blueprintsAttributes, messages, jsonObject);

			if (parameter.isPresent()) {
				parameterDataBuilder.addParameter(parameter.get());
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, FacetConfigurationKeys.HANDLER.getJsonKey(),
				handler);
		}
	}

	private boolean _validateFacetConfiguration(
		Messages messages, JSONObject jsonObject) {

		return BlueprintJSONValidationUtil.validateRequiredFieldsPresent(
			jsonObject, messages, FacetConfigurationKeys.HANDLER.getJsonKey());
	}

	@Reference
	private BlueprintHelper _blueprintHelper;

	@Reference(target = "(type=internal)")
	private FacetRequestHandlerFactory _facetRequestHandlerFactory;

}