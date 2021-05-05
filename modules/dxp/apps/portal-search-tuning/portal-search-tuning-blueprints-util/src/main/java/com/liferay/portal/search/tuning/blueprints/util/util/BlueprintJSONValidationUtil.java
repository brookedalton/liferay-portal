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

package com.liferay.portal.search.tuning.blueprints.util.util;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.search.tuning.blueprints.message.Messages;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Petteri Karttunen
 */
public class BlueprintJSONValidationUtil {

	public static boolean validateRequiredFieldsPresent(
		String className, JSONObject jsonObject, Messages messages,
		String... fields) {

		if (jsonObject == null) {
			MessagesUtil.error(
				messages, className,
				new Throwable(
					"Validation for fields " + Arrays.toString(fields) +
						" failed because the configuration object was empty"),
				null, null, null, "core.error.empty-configuration-object");

			return false;
		}

		return Stream.of(
			fields
		).allMatch(
			field -> _validateFieldPresent(
				jsonObject, messages, className, field)
		);
	}

	private static boolean _validateFieldPresent(
		JSONObject jsonObject, Messages messages, String className,
		String field) {

		if (!jsonObject.has(field)) {
			MessagesUtil.requiredFieldMissingError(
				messages, className, jsonObject, field);

			return false;
		}

		return true;
	}

}