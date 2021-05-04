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

package com.liferay.portal.search.tuning.blueprints.engine.internal.aggregation.util;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.aggregation.Aggregation;
import com.liferay.portal.search.aggregation.FieldAggregation;
import com.liferay.portal.search.aggregation.bucket.IncludeExcludeClause;
import com.liferay.portal.search.aggregation.bucket.Order;
import com.liferay.portal.search.aggregation.bucket.Range;
import com.liferay.portal.search.aggregation.pipeline.GapPolicy;
import com.liferay.portal.search.aggregation.pipeline.PipelineAggregation;
import com.liferay.portal.search.query.Query;
import com.liferay.portal.search.script.Script;
import com.liferay.portal.search.significance.SignificanceHeuristic;
import com.liferay.portal.search.significance.SignificanceHeuristics;
import com.liferay.portal.search.tuning.blueprints.engine.aggregation.AggregationWrapper;
import com.liferay.portal.search.tuning.blueprints.engine.internal.clause.util.ClauseHelper;
import com.liferay.portal.search.tuning.blueprints.engine.internal.util.ScriptHelper;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(immediate = true, service = AggregationHelper.class)
public class AggregationHelper {

	public Map<String, String> getBucketsPaths(
		JSONObject bodyJSONObject, Messages messages) {

		Map<String, String> bucketsPathMap = new HashMap<>();

		Object object = bodyJSONObject.get("buckets_path");

		if (object instanceof JSONObject) {
			JSONObject bucketsPathJSONObject = (JSONObject)object;

			Set<String> keySet = bucketsPathJSONObject.keySet();

			Stream<String> stream = keySet.stream();

			stream.forEach(
				key -> bucketsPathMap.put(
					key, bucketsPathJSONObject.getString(key)));
		}
		else {
			MessagesUtil.invalidConfigurationValueTypeError(
				messages, getClass().getName(),
				JSONObject.class.getSimpleName(), bodyJSONObject,
				"buckets_path", object.toString());
		}

		return bucketsPathMap;
	}

	public GapPolicy getGapPolicy(
		JSONObject bodyJSONObject, Messages messages) {

		if (!bodyJSONObject.has("gap_policy")) {
			return null;
		}

		String s = bodyJSONObject.getString("gap_policy");

		try {
			return GapPolicy.valueOf(StringUtil.toUpperCase(s));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				bodyJSONObject, "gap_policy", s);
		}

		return null;
	}

	public Optional<Script> getScript(Object object, Messages messages) {
		if (Objects.isNull(object)) {
			return Optional.empty();
		}

		if (object instanceof String) {
			return _scriptHelper.getScript((String)object, messages);
		}
		else if (object instanceof JSONObject) {
			return _scriptHelper.getScript((JSONObject)object, messages);
		}

		return Optional.empty();
	}

	public void setBackgroundFilter(
		JSONObject bodyJSONObject, Consumer<Query> setter,
		ParameterData parameterData, Messages messages) {

		if (!bodyJSONObject.has("background_filter")) {
			return;
		}

		JSONObject queryJSONObject = bodyJSONObject.getJSONObject(
			"background_filter");

		Optional<Query> optional = _clauseHelper.getClause(
			queryJSONObject, parameterData, messages);

		if (optional.isPresent()) {
			setter.accept(optional.get());
		}
	}

	public void setBucketPaths(
		JSONObject bodyJSONObject, BiConsumer<String, String> setter,
		Messages messages) {

		if (!bodyJSONObject.has("bucket_paths")) {
			return;
		}

		Map<String, String> bucketsPathMap = getBucketsPaths(
			bodyJSONObject, messages);

		if (bucketsPathMap.isEmpty()) {
			return;
		}

		Set<Map.Entry<String, String>> entrySet = bucketsPathMap.entrySet();

		Stream<Map.Entry<String, String>> stream = entrySet.stream();

		stream.forEach(entry -> setter.accept(entry.getKey(), entry.getKey()));
	}

	public void setGapPolicy(
		JSONObject bodyJSONObject, Consumer<GapPolicy> setter,
		Messages messages) {

		GapPolicy gapPolicy = getGapPolicy(bodyJSONObject, messages);

		if (gapPolicy != null) {
			setter.accept(gapPolicy);
		}
	}

	public void setIncludeExcludeClause(
		JSONObject jsonObject, Consumer<IncludeExcludeClause> setter) {

		Object excludeObject = jsonObject.get("exclude");
		Object includeObject = jsonObject.get("include");

		if (Validator.isNull(excludeObject) &&
			Validator.isNull(includeObject)) {

			return;
		}

		String[] excludeArray = null;

		String excludeString = null;

		String[] includeArray = null;

		String includeString = null;

		if (Validator.isNotNull(excludeObject)) {
			if (excludeObject instanceof JSONArray) {
				excludeArray = JSONUtil.toStringArray((JSONArray)excludeObject);
			}
			else {
				excludeString = GetterUtil.getString(excludeObject);
			}
		}

		if (Validator.isNotNull(includeObject)) {
			if (includeObject instanceof JSONArray) {
				includeArray = JSONUtil.toStringArray((JSONArray)includeObject);
			}
			else {
				includeString = GetterUtil.getString(includeObject);
			}
		}

		IncludeExcludeClause includeExcludeClause = _getIncludeExcludeClause(
			excludeArray, excludeString, includeArray, includeString);

		setter.accept(includeExcludeClause);
	}

	public void setMissing(
		FieldAggregation fieldAggregation, JSONObject bodyJSONObject) {

		if (!bodyJSONObject.has("missing")) {
			return;
		}

		fieldAggregation.setMissing(bodyJSONObject.getString("missing"));
	}

	public void setOrders(
		JSONObject configurationJSONObject, Consumer<Order[]> setter,
		Messages messages) {

		if (!configurationJSONObject.has("order")) {
			return;
		}

		List<Order> orders = new ArrayList<>();

		JSONObject orderJSONObject = configurationJSONObject.getJSONObject(
			"order");

		Set<String> keySet = orderJSONObject.keySet();

		Stream<String> keyStream = keySet.stream();

		keyStream.forEach(
			key -> orders.add(_getOrder(key, orderJSONObject.getString(key))));

		Stream<Order> orderStream = orders.stream();

		setter.accept(orderStream.toArray(Order[]::new));
	}

	public void setRanges(JSONObject bodyJSONObject, Consumer<Range> setter) {
		JSONArray rangesJSONArray = bodyJSONObject.getJSONArray("ranges");

		if ((rangesJSONArray == null) || (rangesJSONArray.length() == 0)) {
			return;
		}

		for (int i = 0; i < rangesJSONArray.length(); i++) {
			JSONObject rangeJSONObject = rangesJSONArray.getJSONObject(i);

			setter.accept(
				new Range(
					rangeJSONObject.getString("key"),
					rangeJSONObject.getDouble("from"),
					rangeJSONObject.getDouble("to")));
		}
	}

	public void setScript(
		JSONObject jsonObject, Consumer<Script> setter, Messages messages) {

		if (!jsonObject.has("script")) {
			return;
		}

		Optional<Script> scriptOptional = getScript(
			jsonObject.get("script"), messages);

		if (scriptOptional.isPresent()) {
			setter.accept(scriptOptional.get());
		}
	}

	public void setScript(
		JSONObject jsonObject, String scriptKey, Consumer<Script> setter,
		Messages messages) {

		if (!jsonObject.has(scriptKey)) {
			return;
		}

		Optional<Script> scriptOptional = getScript(
			jsonObject.get(scriptKey), messages);

		if (scriptOptional.isPresent()) {
			setter.accept(scriptOptional.get());
		}
	}

	public void setSignificanceHeuristics(
		JSONObject bodyJSONObject, Consumer<SignificanceHeuristic> setter,
		Messages messages) {

		SignificanceHeuristic significanceHeuristic = null;

		if (bodyJSONObject.has("chi_square")) {
			JSONObject jsonObject = bodyJSONObject.getJSONObject("chi_square");

			_significanceHeuristics.chiSquare(
				_getBackGroundIsSuperset(jsonObject),
				_getIncludeNegatives(jsonObject));
		}
		else if (bodyJSONObject.has("gnd")) {
			_significanceHeuristics.gnd(
				_getBackGroundIsSuperset(bodyJSONObject.getJSONObject("gnd")));
		}
		else if (bodyJSONObject.has("jlh")) {
			significanceHeuristic = _significanceHeuristics.jlhScore();
		}
		else if (bodyJSONObject.has("mutual_information")) {
			JSONObject jsonObject = bodyJSONObject.getJSONObject(
				"mutual_information");

			_significanceHeuristics.mutualInformation(
				_getBackGroundIsSuperset(jsonObject),
				_getIncludeNegatives(jsonObject));
		}
		else if (bodyJSONObject.has("percentage")) {
			significanceHeuristic = _significanceHeuristics.percentageScore();
		}
		else if (bodyJSONObject.has("script_heuristic")) {
			JSONObject jsonObject = bodyJSONObject.getJSONObject(
				"script_heuristic");

			Optional<Script> optional = getScript(jsonObject, messages);

			if (optional.isPresent()) {
				_significanceHeuristics.script(optional.get());
			}
		}

		if (significanceHeuristic != null) {
			setter.accept(significanceHeuristic);
		}
	}

	public Optional<AggregationWrapper> wrap(Aggregation aggregation) {
		return Optional.of(new AggregationWrapper(aggregation));
	}

	public Optional<AggregationWrapper> wrap(
		PipelineAggregation pipelineAggregation) {

		return Optional.of(new AggregationWrapper(pipelineAggregation));
	}

	private boolean _getBackGroundIsSuperset(JSONObject jsonObject) {
		return jsonObject.getBoolean("background_is_superset", true);
	}

	private IncludeExcludeClause _getIncludeExcludeClause(
		String[] excludeArray, String excludeString, String[] includeArray,
		String includeString) {

		return new IncludeExcludeClause() {

			@Override
			public String[] getExcludedValues() {
				return excludeArray;
			}

			@Override
			public String getExcludeRegex() {
				return excludeString;
			}

			@Override
			public String[] getIncludedValues() {
				return includeArray;
			}

			@Override
			public String getIncludeRegex() {
				return includeString;
			}

		};
	}

	private boolean _getIncludeNegatives(JSONObject jsonObject) {
		return jsonObject.getBoolean("include_negatives", true);
	}

	private Order _getOrder(String key, String value) {
		boolean ascending = StringUtil.equalsIgnoreCase(value, "asc");

		if (Order.COUNT_METRIC_NAME.equals(key)) {
			return Order.count(ascending);
		}
		else if (Order.KEY_METRIC_NAME.equals(key)) {
			return Order.key(ascending);
		}
		else {
			Order order = new Order(key);

			order.setAscending(ascending);

			return order;
		}
	}

	@Reference
	private ClauseHelper _clauseHelper;

	@Reference
	private ScriptHelper _scriptHelper;

	@Reference
	private SignificanceHeuristics _significanceHeuristics;

}