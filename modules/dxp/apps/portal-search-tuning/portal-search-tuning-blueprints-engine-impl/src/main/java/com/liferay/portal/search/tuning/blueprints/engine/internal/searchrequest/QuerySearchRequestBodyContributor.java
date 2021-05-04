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
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.filter.ComplexQueryPartBuilderFactory;
import com.liferay.portal.search.query.Queries;
import com.liferay.portal.search.query.Query;
import com.liferay.portal.search.rescore.RescoreBuilder;
import com.liferay.portal.search.rescore.RescoreBuilderFactory;
import com.liferay.portal.search.searcher.SearchRequestBuilder;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.query.ClauseConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.query.ConditionConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.query.QueryConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.constants.json.values.ClauseContext;
import com.liferay.portal.search.tuning.blueprints.constants.json.values.Occur;
import com.liferay.portal.search.tuning.blueprints.constants.json.values.Operator;
import com.liferay.portal.search.tuning.blueprints.engine.internal.clause.util.ClauseHelper;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.ConditionHandlerFactory;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.engine.spi.clause.ConditionHandler;
import com.liferay.portal.search.tuning.blueprints.engine.spi.query.QueryContributor;
import com.liferay.portal.search.tuning.blueprints.engine.spi.searchrequest.SearchRequestBodyContributor;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.util.BlueprintHelper;
import com.liferay.portal.search.tuning.blueprints.util.component.ServiceComponentReference;
import com.liferay.portal.search.tuning.blueprints.util.component.ServiceComponentReferenceUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=query",
	service = SearchRequestBodyContributor.class
)
public class QuerySearchRequestBodyContributor
	implements SearchRequestBodyContributor {

	@Override
	public void contribute(
		SearchRequestBuilder searchRequestBuilder, Blueprint blueprint,
		ParameterData parameterData, Messages messages) {

		Optional<JSONArray> optional =
			_blueprintHelper.getQueryConfigurationOptional(blueprint);

		if (optional.isPresent()) {
			_contribute(
				searchRequestBuilder, optional.get(), parameterData, messages);
		}

		_executeQueryContributors(
			searchRequestBuilder, parameterData, blueprint, messages);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC
	)
	protected void registerQueryContributor(
		QueryContributor queryContributor, Map<String, Object> properties) {

		ServiceComponentReferenceUtil.addToMapByName(
			_queryContributors, queryContributor, properties);
	}

	protected void unregisterQueryContributor(
		QueryContributor queryContributor, Map<String, Object> properties) {

		ServiceComponentReferenceUtil.removeFromMapByName(
			_queryContributors, queryContributor, properties);
	}

	private void _addPostFilterClause(
		SearchRequestBuilder searchRequestBuilder, Query query, Occur occur) {

		searchRequestBuilder.addPostFilterQueryPart(
			_complexQueryPartBuilderFactory.builder(
			).query(
				query
			).occur(
				occur.getjsonValue()
			).build());
	}

	private void _addQueryClause(
		SearchRequestBuilder searchRequestBuilder, Query query, Occur occur) {

		searchRequestBuilder.addComplexQueryPart(
			_complexQueryPartBuilderFactory.builder(
			).query(
				query
			).occur(
				occur.getjsonValue()
			).build());
	}

	private void _addRescoreClause(
		SearchRequestBuilder searchRequestBuilder, Query query,
		Integer windowSize) {

		RescoreBuilder rescoreBuilder = _rescoreBuilderFactory.builder(query);

		if (windowSize != null) {
			rescoreBuilder.windowSize(windowSize);
		}

		searchRequestBuilder.addRescore(rescoreBuilder.build());
	}

	private void _contribute(
		SearchRequestBuilder searchRequestBuilder,
		JSONArray configurationJSONArray, ParameterData parameterData,
		Messages messages) {

		Messages queryBuildingMessages = new Messages();

		for (int i = 0; i < configurationJSONArray.length(); i++) {
			JSONObject configurationJSONObject =
				configurationJSONArray.getJSONObject(i);

			messages.setElementId("queryElement-" + i);

			if (!configurationJSONObject.getBoolean(
					QueryConfigurationKeys.ENABLED.getJsonKey(), true) ||
				!_isConditionsTrue(
					parameterData, queryBuildingMessages,
					configurationJSONObject)) {

				messages.unsetElementId();

				continue;
			}

			JSONArray clausesJSONArray = configurationJSONObject.getJSONArray(
				QueryConfigurationKeys.CLAUSES.getJsonKey());

			for (int j = 0; j < clausesJSONArray.length(); j++) {
				JSONObject clauseJSONObject = clausesJSONArray.getJSONObject(j);

				Optional<Query> clauseOptional = _clauseHelper.getClause(
					clauseJSONObject.getJSONObject(
						ClauseConfigurationKeys.QUERY.getJsonKey()),
					parameterData, messages);

				if (!clauseOptional.isPresent()) {
					continue;
				}

				ClauseContext clauseContext = _getClauseContext(
					clauseJSONObject, messages);

				Occur occur = _getOccur(clauseJSONObject, messages);

				if ((clauseContext == null) || (occur == null)) {
					continue;
				}

				if (clauseContext.equals(ClauseContext.POST_FILTER)) {
					_addPostFilterClause(
						searchRequestBuilder, clauseOptional.get(), occur);
				}
				else if (clauseContext.equals(ClauseContext.QUERY)) {
					_addQueryClause(
						searchRequestBuilder, clauseOptional.get(), occur);
				}
				else if (clauseContext.equals(ClauseContext.RESCORE)) {
					_addRescoreClause(
						searchRequestBuilder, clauseOptional.get(),
						_getRescoreWindoSize(clauseJSONObject));
				}
			}

			messages.unsetElementId();
		}
	}

	private void _executeQueryContributors(
		SearchRequestBuilder searchRequestBuilder, ParameterData parameterData,
		Blueprint blueprint, Messages messages) {

		if (_log.isDebugEnabled()) {
			_log.debug("Processing query contributors");
		}

		if (_queryContributors.isEmpty()) {
			return;
		}

		for (Map.Entry<String, ServiceComponentReference<QueryContributor>>
				entry : _queryContributors.entrySet()) {

			try {
				ServiceComponentReference<QueryContributor> value =
					entry.getValue();

				QueryContributor queryContributor = value.getServiceComponent();

				Class<?> clazz = queryContributor.getClass();

				if (_isQueryContributorExcluded(blueprint, clazz.getName())) {
					continue;
				}

				Optional<Query> queryOptional = queryContributor.build(
					blueprint, parameterData, messages);

				if (!queryOptional.isPresent()) {
					continue;
				}

				ClauseContext clauseContext =
					queryContributor.getClauseContext();

				if (clauseContext.equals(ClauseContext.POST_FILTER)) {
					_addPostFilterClause(
						searchRequestBuilder, queryOptional.get(),
						queryContributor.getOccur());
				}
				else if (clauseContext.equals(ClauseContext.QUERY)) {
					_addQueryClause(
						searchRequestBuilder, queryOptional.get(),
						queryContributor.getOccur());
				}
				else if (clauseContext.equals(ClauseContext.RESCORE)) {
					_addRescoreClause(
						searchRequestBuilder, queryOptional.get(),
						_getQueryContributorRescoreWindoSize(queryContributor));
				}
			}
			catch (Exception exception) {
				MessagesUtil.unknownError(
					messages, getClass().getName(), exception, null, null,
					null);
			}
		}
	}

	private ClauseContext _getClauseContext(
		JSONObject jsonObject, Messages messages) {

		String context = jsonObject.getString(
			ClauseConfigurationKeys.CONTEXT.getJsonKey());

		try {
			return ClauseContext.valueOf(StringUtil.toUpperCase(context));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, ClauseConfigurationKeys.CONTEXT.getJsonKey(),
				context);
		}

		return null;
	}

	private Occur _getOccur(JSONObject jsonObject, Messages messages) {
		String occur = jsonObject.getString(
			ClauseConfigurationKeys.OCCUR.getJsonKey(), "must");

		try {
			return Occur.valueOf(StringUtil.toUpperCase(occur));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, ClauseConfigurationKeys.OCCUR.getJsonKey(), occur);
		}

		return null;
	}

	private Integer _getQueryContributorRescoreWindoSize(
		QueryContributor queryContributor) {

		if (queryContributor.getAttributes() == null) {
			return null;
		}

		Map<String, Object> attributes = queryContributor.getAttributes();

		if (attributes.containsKey(
				ClauseConfigurationKeys.WINDOW_SIZE.getJsonKey())) {

			return GetterUtil.getInteger(
				attributes.containsKey(
					ClauseConfigurationKeys.WINDOW_SIZE.getJsonKey()));
		}

		return null;
	}

	private Integer _getRescoreWindoSize(JSONObject jsonObject) {
		if (jsonObject.has(ClauseConfigurationKeys.WINDOW_SIZE.getJsonKey())) {
			return jsonObject.getInt(
				ClauseConfigurationKeys.WINDOW_SIZE.getJsonKey());
		}

		return null;
	}

	private boolean _isConditionsTrue(
		ParameterData parameterData, Messages messages,
		JSONObject configurationJSONObject) {

		JSONArray conditionsJSONArray = configurationJSONObject.getJSONArray(
			QueryConfigurationKeys.CONDITIONS.getJsonKey());

		if ((conditionsJSONArray == null) ||
			(conditionsJSONArray.length() == 0)) {

			return true;
		}

		boolean valid = false;

		for (int i = 0; i < conditionsJSONArray.length(); i++) {
			JSONObject conditionJSONObject = conditionsJSONArray.getJSONObject(
				i);

			String handler = conditionJSONObject.getString(
				ConditionConfigurationKeys.HANDLER.getJsonKey(), "default");

			try {
				ConditionHandler conditionHandler =
					_conditionHandlerFactory.getHandler(handler);

				String operatorString = conditionJSONObject.getString(
					ConditionConfigurationKeys.OPERATOR.getJsonKey(),
					Operator.AND.name());

				Operator operator = Operator.valueOf(
					StringUtil.toUpperCase(operatorString));

				JSONObject handlerConfigurationJSONObject =
					conditionJSONObject.getJSONObject(
						ConditionConfigurationKeys.CONFIGURATION.getJsonKey());

				boolean conditionTrue = conditionHandler.isTrue(
					handlerConfigurationJSONObject, parameterData, messages);

				if (operator.equals(Operator.AND) && !conditionTrue) {
					return false;
				}
				else if (operator.equals(Operator.NOT) && conditionTrue) {
					return false;
				}
				else if (conditionTrue) {
					valid = true;
				}
			}
			catch (IllegalArgumentException illegalArgumentException) {
				MessagesUtil.invalidConfigurationValueError(
					messages, getClass().getName(), illegalArgumentException,
					conditionJSONObject, null, null);
			}
			catch (Exception exception) {
				MessagesUtil.unknownError(
					messages, getClass().getName(), exception,
					conditionJSONObject, null, null);
			}
		}

		return valid;
	}

	private boolean _isQueryContributorExcluded(
		Blueprint blueprint, String className) {

		Optional<List<String>> excludedQueryContributorsOptional =
			_blueprintHelper.getExcludedQueryContributorsOptional(blueprint);

		if (!excludedQueryContributorsOptional.isPresent()) {
			return false;
		}

		List<String> excludedQueryContributors =
			excludedQueryContributorsOptional.get();

		Stream<String> stream = excludedQueryContributors.stream();

		if (stream.anyMatch(s -> s.contentEquals(className) || s.equals("*"))) {
			return true;
		}

		return false;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		QuerySearchRequestBodyContributor.class);

	@Reference
	private BlueprintHelper _blueprintHelper;

	@Reference
	private ClauseHelper _clauseHelper;

	@Reference
	private ComplexQueryPartBuilderFactory _complexQueryPartBuilderFactory;

	@Reference
	private ConditionHandlerFactory _conditionHandlerFactory;

	@Reference
	private Queries _queries;

	private volatile Map<String, ServiceComponentReference<QueryContributor>>
		_queryContributors = new ConcurrentHashMap<>();

	@Reference
	private RescoreBuilderFactory _rescoreBuilderFactory;

}