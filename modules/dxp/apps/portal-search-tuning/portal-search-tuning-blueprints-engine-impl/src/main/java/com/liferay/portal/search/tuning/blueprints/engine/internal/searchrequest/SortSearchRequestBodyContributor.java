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

package com.liferay.portal.search.tuning.blueprints.engine.internal.searchrequest;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.searcher.SearchRequestBuilder;
import com.liferay.portal.search.sort.Sort;
import com.liferay.portal.search.sort.SortOrder;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.sort.SortConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.engine.internal.sort.SortTranslatorFactory;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.Parameter;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.engine.spi.searchrequest.SearchRequestBodyContributor;
import com.liferay.portal.search.tuning.blueprints.engine.spi.sort.SortTranslator;
import com.liferay.portal.search.tuning.blueprints.engine.template.variable.BlueprintTemplateVariableParser;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.util.BlueprintHelper;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONValidationUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=sort",
	service = SearchRequestBodyContributor.class
)
public class SortSearchRequestBodyContributor
	implements SearchRequestBodyContributor {

	@Override
	public void contribute(
		SearchRequestBuilder searchRequestBuilder, Blueprint blueprint,
		ParameterData parameterData, Messages messages) {

		List<Sort> sorts = _getSortsFromParameters(
			parameterData, blueprint, messages);

		if (sorts.isEmpty()) {
			sorts = _getDefaultSorts(parameterData, blueprint, messages);
		}

		if (!sorts.isEmpty()) {
			Stream<Sort> stream = sorts.stream();

			stream.forEach(sort -> searchRequestBuilder.addSort(sort));
		}
	}

	private Optional<Sort> _getDefaulSort(
		ParameterData parameterData, JSONObject jsonObject, Messages messages) {

		if (!_validateConfiguration(jsonObject, messages)) {
			return Optional.empty();
		}

		Optional<JSONObject> optional =
			_blueprintTemplateVariableParser.parseObject(
				jsonObject, parameterData, messages);

		if (!optional.isPresent()) {
			return Optional.empty();
		}

		JSONObject configurationJSONObject = optional.get();

		SortOrder sortOrder = _getSortOrder(
			jsonObject,
			configurationJSONObject.getString(
				SortConfigurationKeys.ORDER.getJsonKey()),
			messages);

		if (sortOrder == null) {
			return Optional.empty();
		}

		return _getSort(configurationJSONObject, sortOrder, messages);
	}

	private List<Sort> _getDefaultSorts(
		ParameterData parameterData, Blueprint blueprint, Messages messages) {

		List<Sort> sorts = new ArrayList<>();

		Optional<JSONArray> optional1 =
			_blueprintHelper.getDefaultSortConfigurationOptional(blueprint);

		if (!optional1.isPresent()) {
			return sorts;
		}

		JSONArray jsonArray = optional1.get();

		for (int i = 0; i < jsonArray.length(); i++) {
			Optional<Sort> optional2 = _getDefaulSort(
				parameterData, jsonArray.getJSONObject(i), messages);

			if (optional2.isPresent()) {
				sorts.add(optional2.get());
			}
		}

		return sorts;
	}

	private Optional<Sort> _getSort(
		JSONObject jsonObject, SortOrder sortOrder, Messages messages) {

		String type = jsonObject.getString(
			SortConfigurationKeys.TYPE.getJsonKey(), "field");

		try {
			SortTranslator sortTranslator =
				_sortTranslatorFactory.getTranslator(type);

			return sortTranslator.translate(jsonObject, sortOrder, messages);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, SortConfigurationKeys.TYPE.getJsonKey(), type);
		}

		return Optional.empty();
	}

	private Optional<Sort> _getSortFromParameter(
		ParameterData parameterData, JSONObject jsonObject, Messages messages) {

		if (!_validateConfiguration(jsonObject, messages)) {
			return Optional.empty();
		}

		Optional<JSONObject> optional =
			_blueprintTemplateVariableParser.parseObject(
				jsonObject, parameterData, messages);

		if (!optional.isPresent()) {
			return Optional.empty();
		}

		JSONObject configurationJSONObject = optional.get();

		SortOrder sortOrder = _getSortOrderFromParameter(
			parameterData, configurationJSONObject, messages);

		if (sortOrder == null) {
			return Optional.empty();
		}

		return _getSort(configurationJSONObject, sortOrder, messages);
	}

	private SortOrder _getSortOrder(
		JSONObject jsonObject, String s, Messages messages) {

		try {
			return SortOrder.valueOf(StringUtil.toUpperCase(s));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, SortConfigurationKeys.ORDER.getJsonKey(), s);
		}

		return null;
	}

	private SortOrder _getSortOrderFromParameter(
		ParameterData parameterData, JSONObject jsonObject, Messages messages) {

		String parameterName = jsonObject.getString(
			SortConfigurationKeys.PARAMETER_NAME.getJsonKey());

		if (Validator.isBlank(parameterName)) {
			return null;
		}

		Optional<Parameter> optional = parameterData.getByNameOptional(
			parameterName);

		if (!optional.isPresent()) {
			return null;
		}

		Parameter parameter = optional.get();

		return _getSortOrder(
			jsonObject, GetterUtil.getString(parameter.getValue()), messages);
	}

	private List<Sort> _getSortsFromParameters(
		ParameterData parameterData, Blueprint blueprint, Messages messages) {

		List<Sort> sorts = new ArrayList<>();

		Optional<JSONArray> optional1 =
			_blueprintHelper.getSortParameterConfigurationOptional(blueprint);

		if (!optional1.isPresent()) {
			return sorts;
		}

		JSONArray jsonArray = optional1.get();

		for (int i = 0; i < jsonArray.length(); i++) {
			Optional<Sort> optional2 = _getSortFromParameter(
				parameterData, jsonArray.getJSONObject(i), messages);

			if (optional2.isPresent()) {
				sorts.add(optional2.get());
			}
		}

		return sorts;
	}

	private boolean _validateConfiguration(
		JSONObject jsonObject, Messages messages) {

		return BlueprintJSONValidationUtil.validateRequiredFieldsPresent(
			jsonObject, messages, SortConfigurationKeys.FIELD.getJsonKey());
	}

	@Reference
	private BlueprintHelper _blueprintHelper;

	@Reference
	private BlueprintTemplateVariableParser _blueprintTemplateVariableParser;

	@Reference
	private SortTranslatorFactory _sortTranslatorFactory;

}