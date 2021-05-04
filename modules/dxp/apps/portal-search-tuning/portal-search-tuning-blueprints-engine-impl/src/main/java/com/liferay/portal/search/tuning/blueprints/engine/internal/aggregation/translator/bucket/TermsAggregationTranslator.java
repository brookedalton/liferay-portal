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

package com.liferay.portal.search.tuning.blueprints.engine.internal.aggregation.translator.bucket;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.aggregation.Aggregations;
import com.liferay.portal.search.aggregation.bucket.CollectionMode;
import com.liferay.portal.search.aggregation.bucket.TermsAggregation;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.aggregation.bucket.TermsAggregationBodyConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.engine.aggregation.AggregationWrapper;
import com.liferay.portal.search.tuning.blueprints.engine.internal.aggregation.util.AggregationHelper;
import com.liferay.portal.search.tuning.blueprints.engine.internal.util.SetterHelper;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.engine.spi.aggregation.AggregationTranslator;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONValidationUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=terms",
	service = AggregationTranslator.class
)
public class TermsAggregationTranslator implements AggregationTranslator {

	@Override
	public Optional<AggregationWrapper> translate(
		String aggregationName, JSONObject jsonObject,
		ParameterData parameterData, Messages messages) {

		if (!BlueprintJSONValidationUtil.validateRequiredFieldsPresent(
				jsonObject, messages,
				TermsAggregationBodyConfigurationKeys.FIELD.getJsonKey())) {

			return Optional.empty();
		}

		TermsAggregation aggregation = _aggregations.terms(
			aggregationName,
			jsonObject.getString(
				TermsAggregationBodyConfigurationKeys.FIELD.getJsonKey()));

		_setCollectMode(aggregation, jsonObject, messages);

		_setterHelper.setStringValue(
			jsonObject,
			TermsAggregationBodyConfigurationKeys.EXECUTION_HINT.getJsonKey(),
			aggregation::setExecutionHint);

		_aggregationHelper.setIncludeExcludeClause(
			jsonObject, aggregation::setIncludeExcludeClause);

		_setterHelper.setIntegerValue(
			jsonObject,
			TermsAggregationBodyConfigurationKeys.MIN_DOC_COUNT.getJsonKey(),
			aggregation::setMinDocCount);

		_setterHelper.setStringValue(
			jsonObject,
			TermsAggregationBodyConfigurationKeys.MISSING.getJsonKey(),
			aggregation::setMissing);

		_aggregationHelper.setOrders(
			jsonObject, aggregation::addOrders, messages);

		_aggregationHelper.setScript(
			jsonObject, aggregation::setScript, messages);

		_setterHelper.setIntegerValue(
			jsonObject,
			TermsAggregationBodyConfigurationKeys.SHARD_MIN_DOC_COUNT.
				getJsonKey(),
			aggregation::setShardMinDocCount);

		_setterHelper.setIntegerValue(
			jsonObject,
			TermsAggregationBodyConfigurationKeys.SHARD_SIZE.getJsonKey(),
			aggregation::setShardSize);

		_setterHelper.setBooleanValue(
			jsonObject,
			TermsAggregationBodyConfigurationKeys.SHOW_TERM_DOC_COUNT_ERROR.
				getJsonKey(),
			aggregation::setShowTermDocCountError);

		_setterHelper.setIntegerValue(
			jsonObject, TermsAggregationBodyConfigurationKeys.SIZE.getJsonKey(),
			aggregation::setSize);

		return _aggregationHelper.wrap(aggregation);
	}

	private void _setCollectMode(
		TermsAggregation aggregation, JSONObject jsonObject,
		Messages messages) {

		if (!jsonObject.has(
				TermsAggregationBodyConfigurationKeys.COLLECT_MODE.
					getJsonKey())) {

			return;
		}

		String collectModeString = jsonObject.getString(
			TermsAggregationBodyConfigurationKeys.COLLECT_MODE.getJsonKey());

		try {
			aggregation.setCollectionMode(
				CollectionMode.valueOf(
					StringUtil.toUpperCase(collectModeString)));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject,
				TermsAggregationBodyConfigurationKeys.COLLECT_MODE.getJsonKey(),
				collectModeString);
		}
	}

	@Reference
	private AggregationHelper _aggregationHelper;

	@Reference
	private Aggregations _aggregations;

	@Reference
	private SetterHelper _setterHelper;

}