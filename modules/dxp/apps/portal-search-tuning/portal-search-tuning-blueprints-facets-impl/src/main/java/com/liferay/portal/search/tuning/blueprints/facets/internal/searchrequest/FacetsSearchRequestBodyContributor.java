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

package com.liferay.portal.search.tuning.blueprints.facets.internal.searchrequest;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.aggregation.Aggregations;
import com.liferay.portal.search.aggregation.bucket.DateRangeAggregation;
import com.liferay.portal.search.aggregation.bucket.TermsAggregation;
import com.liferay.portal.search.filter.ComplexQueryPartBuilderFactory;
import com.liferay.portal.search.query.BooleanQuery;
import com.liferay.portal.search.query.Queries;
import com.liferay.portal.search.query.TermQuery;
import com.liferay.portal.search.searcher.SearchRequestBuilder;
import com.liferay.portal.search.tuning.blueprints.constants.json.values.FilterMode;
import com.liferay.portal.search.tuning.blueprints.constants.json.values.Operator;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.Parameter;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.engine.spi.searchrequest.SearchRequestBodyContributor;
import com.liferay.portal.search.tuning.blueprints.engine.template.variable.BlueprintTemplateVariableParser;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetsBlueprintKeys;
import com.liferay.portal.search.tuning.blueprints.facets.internal.util.FacetConfigurationUtil;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.util.BlueprintHelper;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.SetterHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=facets",
	service = SearchRequestBodyContributor.class
)
public class FacetsSearchRequestBodyContributor
	implements SearchRequestBodyContributor {

	@Override
	public void contribute(
		SearchRequestBuilder searchRequestBuilder, Blueprint blueprint,
		ParameterData parameterData, Messages messages) {

		Optional<JSONArray> optional =
			_blueprintHelper.getJSONArrayConfigurationOptional(
				blueprint,
				"JSONArray/" + FacetsBlueprintKeys.CONFIGURATION_SECTION);

		if (!optional.isPresent()) {
			return;
		}

		_contribute(
			searchRequestBuilder, optional.get(), parameterData, messages);
	}

	private void _addAggregation(
		SearchRequestBuilder searchRequestBuilder, JSONObject jsonObject,
		Messages messages) {

		String aggregationType = jsonObject.getString(
			FacetConfigurationKeys.AGGREGATION_TYPE.getJsonKey(), "terms");

		if (aggregationType.contentEquals("terms")) {
			_addTermsAggregation(searchRequestBuilder, jsonObject);
		}
		else if (aggregationType.contentEquals("date_range")) {
			_addDateRangeAggregation(
				searchRequestBuilder, jsonObject, messages);
		}
		else {
			MessagesUtil.warning(
				messages, getClass().getName(), "Unknown aggregation type",
				jsonObject,
				FacetConfigurationKeys.AGGREGATION_TYPE.getJsonKey(),
				aggregationType,
				"core.error.unusupported-facet-aggregation-type");
		}
	}

	private void _addDateRangeAggregation(
		SearchRequestBuilder searchRequestBuilder, JSONObject jsonObject,
		Messages messages) {

		JSONObject handlerParametersJSONObject = jsonObject.getJSONObject(
			FacetConfigurationKeys.HANDLER_PARAMETERS.getJsonKey());

		if ((handlerParametersJSONObject == null) ||
			!handlerParametersJSONObject.has("ranges")) {

			return;
		}

		JSONArray rangesJSONArray = handlerParametersJSONObject.getJSONArray(
			"ranges");

		String dateFormat = handlerParametersJSONObject.getString(
			"date_format");

		DateRangeAggregation dateRangeAggregation = _aggregations.dateRange(
			FacetConfigurationUtil.getAggregationName(jsonObject),
			FacetConfigurationUtil.getFieldName(jsonObject));

		for (int i = 0; i < rangesJSONArray.length(); i++) {
			JSONObject rangeJSONObject = rangesJSONArray.getJSONObject(i);

			String from = _getDateRangeString(
				rangeJSONObject.getString("from"), dateFormat, true, messages);
			String label = rangeJSONObject.getString("label", "label");
			String to = _getDateRangeString(
				rangeJSONObject.getString("to"), dateFormat, false, messages);

			dateRangeAggregation.addRange(label, from, to);
		}

		searchRequestBuilder.addAggregation(dateRangeAggregation);
	}

	private void _addDateRangeFacetFilter(
		SearchRequestBuilder searchRequestBuilder, JSONObject jsonObject,
		Object value, Messages messages) {

		Optional<FilterMode> filterModeOptional = _getFilterMode(
			jsonObject, messages);

		if (!filterModeOptional.isPresent()) {
			return;
		}

		BooleanQuery query = _getDateRangeFilterQuery(
			jsonObject, GetterUtil.getString(value), messages);

		if (query.hasClauses()) {
			if (FilterMode.PRE.equals(filterModeOptional.get())) {
				searchRequestBuilder.addComplexQueryPart(
					_complexQueryPartBuilderFactory.builder(
					).query(
						query
					).occur(
						"filter"
					).build());
			}
			else {
				searchRequestBuilder.addPostFilterQueryPart(
					_complexQueryPartBuilderFactory.builder(
					).query(
						query
					).occur(
						"must"
					).build());
			}
		}
	}

	private void _addFilter(
		SearchRequestBuilder searchRequestBuilder, ParameterData parameterData,
		JSONObject jsonObject, Messages messages) {

		Optional<Parameter> optional = parameterData.getByNameOptional(
			FacetConfigurationUtil.getParameterName(jsonObject));

		if (!optional.isPresent()) {
			return;
		}

		Parameter parameter = optional.get();

		String handlerName = FacetConfigurationUtil.getHandlerName(jsonObject);

		try {
			if (handlerName.equals("date_range")) {
				_addDateRangeFacetFilter(
					searchRequestBuilder, jsonObject, parameter.getValue(),
					messages);
			}
			else {
				_addTermsFacetFilter(
					searchRequestBuilder, jsonObject, parameter.getValue(),
					messages);
			}
		}
		catch (Exception exception) {
			MessagesUtil.error(
				messages, getClass().getName(), exception, jsonObject, null,
				null, "facets.error.unknown-error-in-creating-filter");
		}
	}

	private void _addTermsAggregation(
		SearchRequestBuilder searchRequestBuilder, JSONObject jsonObject) {

		TermsAggregation aggregation = _aggregations.terms(
			FacetConfigurationUtil.getAggregationName(jsonObject),
			FacetConfigurationUtil.getFieldName(jsonObject));

		int size = jsonObject.getInt(
			FacetConfigurationKeys.SIZE.getJsonKey(), 50);

		aggregation.setSize(size);

		if (jsonObject.has(FacetConfigurationKeys.SHARD_SIZE.getJsonKey())) {
			int defaultShardSize = (int)Math.floor((size * 1.5) + 10);

			aggregation.setShardSize(
				jsonObject.getInt(
					FacetConfigurationKeys.SHARD_SIZE.getJsonKey(),
					defaultShardSize));
		}

		_setterHelper.setIntegerValue(
			jsonObject, FacetConfigurationKeys.MIN_DOC_COUNT.getJsonKey(),
			aggregation::setMinDocCount);

		searchRequestBuilder.addAggregation(aggregation);
	}

	private void _addTermsFacetFilter(
		SearchRequestBuilder searchRequestBuilder, JSONObject jsonObject,
		Object value, Messages messages) {

		Optional<FilterMode> filterModeOptional = _getFilterMode(
			jsonObject, messages);

		if (!filterModeOptional.isPresent()) {
			return;
		}

		Optional<Operator> operatorOptional = _getOperator(
			jsonObject, messages);

		if (!operatorOptional.isPresent()) {
			return;
		}

		BooleanQuery query = _getTermFilterQuery(
			operatorOptional.get(),
			FacetConfigurationUtil.getFieldName(jsonObject), value);

		if (query.hasClauses()) {
			if (FilterMode.PRE.equals(filterModeOptional.get())) {
				searchRequestBuilder.addComplexQueryPart(
					_complexQueryPartBuilderFactory.builder(
					).query(
						query
					).occur(
						"filter"
					).build());
			}
			else {
				searchRequestBuilder.addPostFilterQueryPart(
					_complexQueryPartBuilderFactory.builder(
					).query(
						query
					).occur(
						"must"
					).build());
			}
		}
	}

	private void _contribute(
		SearchRequestBuilder searchRequestBuilder, JSONArray jsonArray,
		ParameterData parameterData, Messages messages) {

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject configurationJSONObject = jsonArray.getJSONObject(i);

			Optional<JSONObject> parsedConfigurationJSONObjectOptional =
				_blueprintTemplateVariableParser.parseObject(
					configurationJSONObject, parameterData, messages);

			if (!parsedConfigurationJSONObjectOptional.isPresent()) {
				continue;
			}

			JSONObject parsedConfigurationJSONObject =
				parsedConfigurationJSONObjectOptional.get();

			if (!FacetConfigurationUtil.isEnabled(
					parsedConfigurationJSONObject)) {

				continue;
			}

			_addAggregation(
				searchRequestBuilder, parsedConfigurationJSONObject, messages);

			_addFilter(
				searchRequestBuilder, parameterData,
				parsedConfigurationJSONObject, messages);
		}
	}

	private BooleanQuery _getDateRangeFilterQuery(
		JSONObject jsonObject, String value, Messages messages) {

		BooleanQuery booleanQuery = _queries.booleanQuery();

		String field = FacetConfigurationUtil.getFieldName(jsonObject);

		JSONObject handlerParametersJSONObject = jsonObject.getJSONObject(
			FacetConfigurationKeys.HANDLER_PARAMETERS.getJsonKey());

		if ((handlerParametersJSONObject == null) ||
			!handlerParametersJSONObject.has("ranges")) {

			return booleanQuery;
		}

		JSONArray rangesJSONArray = handlerParametersJSONObject.getJSONArray(
			"ranges");

		String dateFormatString = handlerParametersJSONObject.getString(
			"date_format");

		for (int i = 0; i < rangesJSONArray.length(); i++) {
			JSONObject rangeJSONObject = rangesJSONArray.getJSONObject(i);

			String label = rangeJSONObject.getString("label");

			if (value.equals(label)) {
				booleanQuery.addMustQueryClauses(
					_queries.dateRangeTerm(
						field, true, true,
						_getDateRangeString(
							rangeJSONObject.getString("from"), dateFormatString,
							false, messages),
						_getDateRangeString(
							rangeJSONObject.getString("to"), dateFormatString,
							true, messages)));

				break;
			}
		}

		return booleanQuery;
	}

	private String _getDateRangeString(
		String str, String dateFormatString, boolean future,
		Messages messages) {

		try {
			Date date = null;

			if (str.equals("*")) {
				if (future) {
					date = new Date(Long.MAX_VALUE);
				}
				else {
					date = new Date(Long.MIN_VALUE);
				}
			}
			else if (Validator.isBlank(str)) {
				date = new Date();
			}

			if (date != null) {
				DateFormat dateFormat = new SimpleDateFormat(dateFormatString);

				return dateFormat.format(date);
			}
		}
		catch (Exception exception) {
			MessagesUtil.error(
				messages, getClass().getName(), exception, null, null,
				dateFormatString, "core.error.invalid-date-format");
		}

		return str;
	}

	private Optional<FilterMode> _getFilterMode(
		JSONObject jsonObject, Messages messages) {

		String s = jsonObject.getString(
			FacetConfigurationKeys.FILTER_MODE.getJsonKey(),
			FilterMode.PRE.getjsonValue());

		try {
			FilterMode filterMode = FilterMode.valueOf(
				StringUtil.toUpperCase(s));

			return Optional.of(filterMode);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, FacetConfigurationKeys.FILTER_MODE.getJsonKey(), s);
		}

		return Optional.empty();
	}

	private Optional<Operator> _getOperator(
		JSONObject jsonObject, Messages messages) {

		String s = jsonObject.getString(
			FacetConfigurationKeys.MULTI_VALUE_OPERATOR.getJsonKey(),
			Operator.AND.getjsonValue());

		try {
			Operator operator = Operator.valueOf(StringUtil.toUpperCase(s));

			return Optional.of(operator);
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject,
				FacetConfigurationKeys.MULTI_VALUE_OPERATOR.getJsonKey(), s);
		}

		return Optional.empty();
	}

	private BooleanQuery _getTermFilterQuery(
		Operator operator, String field, Object value) {

		BooleanQuery query = _queries.booleanQuery();

		if (value instanceof String) {
			query.addMustQueryClauses(_queries.term(field, value));
		}
		else if (value instanceof String[]) {
			String[] values = (String[])value;

			for (String val : values) {
				TermQuery condition = _queries.term(field, val);

				if (values.length > 1) {
					if (operator.equals(Operator.AND)) {
						query.addMustQueryClauses(condition);
					}
					else {
						query.addShouldQueryClauses(condition);
					}
				}
				else {
					query.addMustQueryClauses(condition);
				}
			}
		}

		return query;
	}

	@Reference
	private Aggregations _aggregations;

	@Reference
	private BlueprintHelper _blueprintHelper;

	@Reference
	private BlueprintTemplateVariableParser _blueprintTemplateVariableParser;

	@Reference
	private ComplexQueryPartBuilderFactory _complexQueryPartBuilderFactory;

	@Reference
	private Queries _queries;

	@Reference
	private SetterHelper _setterHelper;

}