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

package com.liferay.portal.search.tuning.blueprints.engine.internal.aggregation.translator.metric;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.search.aggregation.Aggregations;
import com.liferay.portal.search.aggregation.metrics.PercentilesAggregation;
import com.liferay.portal.search.aggregation.metrics.PercentilesMethod;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.aggregation.metric.MaxAggregationBodyConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.aggregation.metric.PercentileRanksAggregationBodyConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.aggregation.metric.PercentilesAggregationBodyConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.engine.aggregation.AggregationWrapper;
import com.liferay.portal.search.tuning.blueprints.engine.internal.aggregation.util.AggregationHelper;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.engine.spi.aggregation.AggregationTranslator;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONValidationUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.SetterHelper;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=percentiles",
	service = AggregationTranslator.class
)
public class PercentilesAggregationTranslator implements AggregationTranslator {

	@Override
	public Optional<AggregationWrapper> translate(
		String aggregationName, JSONObject jsonObject,
		ParameterData parameterData, Messages messages) {

		if (!BlueprintJSONValidationUtil.validateRequiredFieldsPresent(getClass().getName(),
				jsonObject, messages,
				MaxAggregationBodyConfigurationKeys.FIELD.getJsonKey())) {

			return Optional.empty();
		}

		PercentilesAggregation aggregation = _aggregations.percentiles(
			aggregationName,
			jsonObject.getString(
				PercentilesAggregationBodyConfigurationKeys.FIELD.
					getJsonKey()));

		_setterHelper.setBooleanValue(
			jsonObject,
			PercentileRanksAggregationBodyConfigurationKeys.KEYED.getJsonKey(),
			aggregation::setKeyed);

		_setMethod(aggregation, jsonObject);

		_setterHelper.setStringValue(
			jsonObject,
			PercentileRanksAggregationBodyConfigurationKeys.MISSING.
				getJsonKey(),
			aggregation::setMissing);

		_setPercents(aggregation, jsonObject);

		_aggregationHelper.setScript(
			jsonObject, aggregation::setScript, messages);

		return _aggregationHelper.wrap(aggregation);
	}

	private void _setHDR(
		PercentilesAggregation aggregation, JSONObject jsonObject) {

		JSONObject hdrJSONObject = jsonObject.getJSONObject(
			PercentilesAggregationBodyConfigurationKeys.HDR.getJsonKey());

		if (!hdrJSONObject.has("number_of_significant_value_digits")) {
			return;
		}

		aggregation.setPercentilesMethod(PercentilesMethod.HDR);
		aggregation.setHdrSignificantValueDigits(
			hdrJSONObject.getInt("number_of_significant_value_digits"));
	}

	private void _setMethod(
		PercentilesAggregation aggregation, JSONObject jsonObject) {

		if (jsonObject.has(
				PercentileRanksAggregationBodyConfigurationKeys.HDR.
					getJsonKey())) {

			_setHDR(aggregation, jsonObject);
		}
		else if (jsonObject.has(
					PercentileRanksAggregationBodyConfigurationKeys.TDIGEST.
						getJsonKey())) {

			_setTDigest(aggregation, jsonObject);
		}
	}

	private void _setPercents(
		PercentilesAggregation aggregation, JSONObject jsonObject) {

		if (!jsonObject.has(
				PercentilesAggregationBodyConfigurationKeys.PERCENTS.
					getJsonKey())) {

			return;
		}

		aggregation.setPercents(
			BlueprintJSONUtil.toDoubleArray(
				jsonObject.getJSONArray(
					PercentilesAggregationBodyConfigurationKeys.PERCENTS.
						getJsonKey())));
	}

	private void _setTDigest(
		PercentilesAggregation aggregation, JSONObject jsonObject) {

		JSONObject tDigestJSONObject = jsonObject.getJSONObject(
			PercentilesAggregationBodyConfigurationKeys.TDIGEST.getJsonKey());

		if (!tDigestJSONObject.has("compression")) {
			return;
		}

		aggregation.setPercentilesMethod(PercentilesMethod.TDIGEST);
		aggregation.setCompression(tDigestJSONObject.getInt("compression"));
	}

	@Reference
	private AggregationHelper _aggregationHelper;

	@Reference
	private Aggregations _aggregations;

	@Reference
	private SetterHelper _setterHelper;

}