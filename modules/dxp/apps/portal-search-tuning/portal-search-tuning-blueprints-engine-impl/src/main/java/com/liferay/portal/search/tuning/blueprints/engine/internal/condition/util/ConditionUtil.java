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

package com.liferay.portal.search.tuning.blueprints.engine.internal.condition.util;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.query.ConditionConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.engine.exception.ParameterEvaluationException;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

/**
 * @author Petteri Karttunen
 */
public class ConditionUtil {

	public static Date getDateValue(JSONObject conditionJSONObject)
		throws ParameterEvaluationException {

		String dateString = conditionJSONObject.getString(
			ConditionConfigurationKeys.VALUE.getJsonKey());

		String dateFormatString = conditionJSONObject.getString(
			ConditionConfigurationKeys.DATE_FORMAT.getJsonKey());

		if (Validator.isNull(dateFormatString)) {
			throw new ParameterEvaluationException(
				MessagesUtil.toErrorMessage(
					ConditionUtil.class.getName(),
					new Throwable("Date format missing"), conditionJSONObject,
					ConditionConfigurationKeys.DATE_FORMAT.getJsonKey(),
					dateFormatString,
					"core.error.clause-condition-date-format-missing"));
		}

		try {
			DateFormat dateFormat = new SimpleDateFormat(dateFormatString);

			return dateFormat.parse(dateString);
		}
		catch (Exception exception) {
			throw new ParameterEvaluationException(
				MessagesUtil.toErrorMessage(
					ConditionUtil.class.getName(), exception,
					conditionJSONObject,
					ConditionConfigurationKeys.VALUE.getJsonKey(), dateString,
					"core.error.clause-condition-date-parsing-error"));
		}
	}

}