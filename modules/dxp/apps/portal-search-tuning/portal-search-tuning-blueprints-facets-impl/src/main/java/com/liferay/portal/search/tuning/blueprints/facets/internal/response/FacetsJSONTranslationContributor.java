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

package com.liferay.portal.search.tuning.blueprints.facets.internal.response;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.aggregation.AggregationResult;
import com.liferay.portal.search.searcher.SearchResponse;
import com.liferay.portal.search.tuning.blueprints.engine.attributes.BlueprintsAttributes;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetsBlueprintKeys;
import com.liferay.portal.search.tuning.blueprints.facets.constants.FacetsJSONResponseKeys;
import com.liferay.portal.search.tuning.blueprints.facets.internal.response.handler.FacetResponseHandlerFactory;
import com.liferay.portal.search.tuning.blueprints.facets.internal.util.FacetConfigurationUtil;
import com.liferay.portal.search.tuning.blueprints.facets.spi.response.FacetResponseHandler;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.spi.contributor.JSONTranslationContributor;
import com.liferay.portal.search.tuning.blueprints.util.BlueprintHelper;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=facets",
	service = JSONTranslationContributor.class
)
public class FacetsJSONTranslationContributor
	implements JSONTranslationContributor {

	@Override
	public void contribute(
		JSONObject responseJSONObject, SearchResponse searchResponse,
		Blueprint blueprint, BlueprintsAttributes blueprintsAttributes,
		ResourceBundle resourceBundle, Messages messages) {

		responseJSONObject.put(
			FacetsJSONResponseKeys.FACETS,
			_getFacetsJSONObject(
				searchResponse, blueprint, blueprintsAttributes, resourceBundle,
				messages));
	}

	private JSONObject _getFacetsJSONObject(
		SearchResponse searchResponse, Blueprint blueprint,
		BlueprintsAttributes blueprintsAttributes,
		ResourceBundle resourceBundle, Messages messages) {

		Optional<JSONArray> configurationJSONArrayOptional =
			_blueprintHelper.getJSONArrayConfigurationOptional(
				blueprint,
				"JSONArray/" + FacetsBlueprintKeys.CONFIGURATION_SECTION);

		Map<String, AggregationResult> aggregationResultsMap =
			searchResponse.getAggregationResultsMap();

		if (aggregationResultsMap.isEmpty() ||
			!configurationJSONArrayOptional.isPresent()) {

			return _jsonFactory.createJSONObject();
		}

		return _processFacets(
			aggregationResultsMap, blueprintsAttributes, resourceBundle,
			messages, configurationJSONArrayOptional.get());
	}

	private String _getHandler(JSONObject jsonObject) {
		return jsonObject.getString(
			FacetConfigurationKeys.HANDLER.getJsonKey(), "default");
	}

	private boolean _isEnabled(JSONObject jsonObject) {
		return jsonObject.getBoolean(
			FacetConfigurationKeys.ENABLED.getJsonKey(), true);
	}

	private JSONObject _processFacets(
		Map<String, AggregationResult> aggregationResultsMap,
		BlueprintsAttributes blueprintsAttributes,
		ResourceBundle resourceBundle, Messages messages,
		JSONArray configurationJSONArray) {

		JSONObject facetsResponseJSONObject = _jsonFactory.createJSONObject();

		for (int i = 0; i < configurationJSONArray.length(); i++) {
			JSONObject configurationJSONObject =
				configurationJSONArray.getJSONObject(i);

			if (!_isEnabled(configurationJSONObject)) {
				continue;
			}

			String responseHandlerName = _getHandler(configurationJSONObject);

			String aggregationName = FacetConfigurationUtil.getAggregationName(
				configurationJSONObject);

			for (Map.Entry<String, AggregationResult> entry :
					aggregationResultsMap.entrySet()) {

				if (!StringUtil.equalsIgnoreCase(
						entry.getKey(), aggregationName)) {

					continue;
				}

				try {
					FacetResponseHandler facetResponseHandler =
						_facetResponseHandlerFactory.getHandler(
							responseHandlerName);

					Optional<JSONObject> resultOptional =
						facetResponseHandler.getResultOptional(
							entry.getValue(), blueprintsAttributes,
							resourceBundle, messages, configurationJSONObject);

					if (resultOptional.isPresent()) {
						facetsResponseJSONObject.put(
							FacetConfigurationUtil.getFacetName(
								configurationJSONObject),
							resultOptional.get());
					}
				}
				catch (IllegalArgumentException illegalArgumentException) {
					MessagesUtil.invalidConfigurationValueError(
						messages, getClass().getName(),
						illegalArgumentException, null, null,
						responseHandlerName);
				}
			}
		}

		return facetsResponseJSONObject;
	}

	@Reference
	private BlueprintHelper _blueprintHelper;

	@Reference(target = "(type=internal)")
	private FacetResponseHandlerFactory _facetResponseHandlerFactory;

	@Reference
	private JSONFactory _jsonFactory;

}