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

package com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.internal.contributor;

import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.aggregation.AggregationResult;
import com.liferay.portal.search.searcher.SearchResponse;
import com.liferay.portal.search.tuning.blueprints.engine.attributes.BlueprintsAttributes;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.constants.JSONKeys;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.internal.aggregation.AggregationJSONTranslatorFactory;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.spi.aggregation.AggregationJSONTranslator;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.spi.contributor.JSONTranslationContributor;
import com.liferay.portal.search.tuning.blueprints.util.BlueprintHelper;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=aggs",
	service = JSONTranslationContributor.class
)
public class AggregationsTranslationContributor
	implements JSONTranslationContributor {

	@Override
	public void contribute(
		JSONObject responseJSONObject, SearchResponse searchResponse,
		Blueprint blueprint, BlueprintsAttributes blueprintsAttributes,
		ResourceBundle resourceBundle, Messages messages) {

		responseJSONObject.put(
			JSONKeys.AGGREGATIONS,
			_getAggregationsJSONObject(searchResponse, blueprint, messages));
	}

	private void _addResult(
		JSONObject responseJSONObject, String aggregationName, String type,
		AggregationResult aggregationResult, Messages messages) {

		try {
			AggregationJSONTranslator aggregationResponseBuilder =
				_aggregationResponseBuilderFactory.getTranslator(type);

			Optional<JSONObject> aggregationJsonOptional =
				aggregationResponseBuilder.translate(aggregationResult);

			if (aggregationJsonOptional.isPresent()) {
				responseJSONObject.put(
					aggregationName, aggregationJsonOptional.get());
			}
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException, null,
				null, type);
		}
	}

	private JSONObject _getAggregationsJSONObject(
		SearchResponse searchResponse, Blueprint blueprint, Messages messages) {

		JSONObject responseJSONObject = _jsonFactory.createJSONObject();

		Optional<JSONObject> configurationJSONObjectOptional =
			_blueprintHelper.getAggsConfigurationOptional(blueprint);

		Map<String, AggregationResult> aggregations =
			searchResponse.getAggregationResultsMap();

		if (aggregations.isEmpty() ||
			!configurationJSONObjectOptional.isPresent()) {

			return responseJSONObject;
		}

		JSONObject configurationJSONObject =
			configurationJSONObjectOptional.get();

		Set<String> keySet = configurationJSONObject.keySet();

		keySet.forEach(
			aggregationName -> {
				JSONObject nameJSONObject =
					configurationJSONObject.getJSONObject(aggregationName);

				Optional<String> typeOptional =
					BlueprintJSONUtil.getFirstKeyOptional(nameJSONObject);

				Set<Map.Entry<String, AggregationResult>> entrySet =
					aggregations.entrySet();

				Stream<Map.Entry<String, AggregationResult>> stream =
					entrySet.stream();

				stream.filter(
					entry -> StringUtil.equalsIgnoreCase(
						entry.getKey(), aggregationName)
				).forEach(
					entry -> _addResult(
						responseJSONObject, entry.getKey(), typeOptional.get(),
						entry.getValue(), messages)
				);
			});

		return responseJSONObject;
	}

	@Reference
	private AggregationJSONTranslatorFactory _aggregationResponseBuilderFactory;

	@Reference
	private BlueprintHelper _blueprintHelper;

	@Reference
	private JSONFactory _jsonFactory;

}