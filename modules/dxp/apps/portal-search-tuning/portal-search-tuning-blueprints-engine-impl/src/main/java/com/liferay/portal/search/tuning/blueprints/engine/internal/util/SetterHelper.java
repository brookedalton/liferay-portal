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

package com.liferay.portal.search.tuning.blueprints.engine.internal.util;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.search.sort.FieldSort;
import com.liferay.portal.search.sort.SortOrder;
import com.liferay.portal.search.sort.Sorts;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(immediate = true, service = SetterHelper.class)
public class SetterHelper {

	public void setBooleanValue(
		JSONObject jsonObject, String key, Consumer<Boolean> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(jsonObject.getBoolean(key));
	}

	public void setDoubleArrayValue(
		JSONObject jsonObject, String key, Consumer<double[]> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(
			BlueprintJSONUtil.toDoubleArray(jsonObject.getJSONArray(key)));
	}

	public void setDoubleValue(
		JSONObject jsonObject, String key, Consumer<Double> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(jsonObject.getDouble(key));
	}

	public void setFieldSorts(
		JSONObject jsonObject, Consumer<FieldSort> setter, Messages messages) {

		if (!jsonObject.has("sort")) {
			return;
		}

		JSONObject sortJSONObject = jsonObject.getJSONObject("sort");

		Set<String> keySet = sortJSONObject.keySet();

		List<FieldSort> sorts = new ArrayList<>();

		for (String key : keySet) {
			JSONObject orderJSONObject = sortJSONObject.getJSONObject(key);

			if (orderJSONObject.has("order")) {
				_addFieldSortWithOrder(
					sorts, key, jsonObject, orderJSONObject, messages);
			}
			else {
				setter.accept(_sorts.field(key));
			}
		}
	}

	public void setFloatValue(
		JSONObject jsonObject, String key, Consumer<Float> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(GetterUtil.getFloat(jsonObject.get(key)));
	}

	public void setIntegerValue(
		JSONObject jsonObject, String key, Consumer<Integer> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(jsonObject.getInt(key));
	}

	public void setLongValue(
		JSONObject jsonObject, String key, Consumer<Long> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(jsonObject.getLong(key));
	}

	public void setObjectValue(
		JSONObject jsonObject, String key, Consumer<Object> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(jsonObject.get(key));
	}

	public void setStringArrayValue(
		JSONObject jsonObject, String key, Consumer<String[]> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(JSONUtil.toStringArray(jsonObject.getJSONArray(key)));
	}

	public void setStringValue(
		JSONObject jsonObject, String key, Consumer<String> setter) {

		if (!jsonObject.has(key)) {
			return;
		}

		setter.accept(jsonObject.getString(key));
	}

	private void _addFieldSortWithOrder(
		List<FieldSort> sorts, String fieldName, JSONObject jsonObject,
		JSONObject orderJSONObject, Messages messages) {

		String order = orderJSONObject.getString("order");

		try {
			SortOrder sortOrder = SortOrder.valueOf(order);

			sorts.add(_sorts.field(fieldName, sortOrder));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject, "order", order);
		}
	}

	@Reference
	private Sorts _sorts;

}