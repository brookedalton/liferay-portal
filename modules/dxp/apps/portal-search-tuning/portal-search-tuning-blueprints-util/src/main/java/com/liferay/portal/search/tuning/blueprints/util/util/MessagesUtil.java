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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.search.tuning.blueprints.message.Message;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.message.Severity;

/**
 * @author Petteri Karttunen
 */
public class MessagesUtil {

	public static void error(
		Messages messages, String className, Throwable throwable,
		Object rootObject, String rootProperty, String rootValue,
		String localizationKey) {

		messages.addMessage(
			new Message.Builder().className(
				className
			).localizationKey(
				localizationKey
			).msg(
				_getMsg(throwable, className)
			).rootObject(
				rootObject
			).rootProperty(
				rootProperty
			).rootValue(
				rootValue
			).severity(
				Severity.ERROR
			).throwable(
				throwable
			).build());

		if (throwable != null) {
			_log.error(throwable.getMessage(), throwable);
		}
		else {
			StringBundler sb = new StringBundler();

			_addLogMessageDetails(
				new StringBundler(), className, rootObject, rootProperty,
				rootValue);

			_log.error(sb.toString());
		}
	}

	public static void invalidConfigurationValueError(
		Messages messages, String className, Throwable throwable,
		Object rootObject, String rootProperty, String rootValue) {

		messages.addMessage(
			new Message.Builder().className(
				className
			).localizationKey(
				"core.error.invalid-configuration-value"
			).msg(
				_getMsg(throwable, className)
			).rootObject(
				rootObject
			).rootProperty(
				rootProperty
			).rootValue(
				rootValue
			).severity(
				Severity.ERROR
			).throwable(
				throwable
			).build());

		if (throwable != null) {
			_log.error(throwable.getMessage(), throwable);
		}

		StringBundler sb = new StringBundler(7);

		sb.append("Invalid or unknown configuration value.");

		_addLogMessageDetails(
			sb, className, rootObject, rootProperty, rootValue);

		_log.error(sb.toString());
	}

	public static void invalidConfigurationValueTypeError(
		Messages messages, String className, String correctType,
		Object rootObject, String rootProperty, String rootValue) {

		messages.addMessage(
			new Message.Builder().className(
				className
			).localizationKey(
				"core.error.invalid-configuration-value-type"
			).msg(
				StringBundler.concat(
					"Invalid type in configuration value  ", rootValue, ". ",
					correctType, " expected.")
			).rootObject(
				rootObject
			).rootProperty(
				rootProperty
			).rootValue(
				rootValue
			).severity(
				Severity.ERROR
			).build());

		StringBundler sb = new StringBundler();

		sb.append("Invalid configuration value type. ");
		sb.append(correctType);
		sb.append(" expected.");

		_addLogMessageDetails(
			sb, className, rootObject, rootProperty, rootValue);

		_log.error(sb.toString());
	}

	public static void requiredFieldMissingError(
		Messages messages, String className, Object rootObject, String field) {

		messages.addMessage(
			new Message.Builder().className(
				className
			).localizationKey(
				"core.error.required-field-missing"
			).msg(
				"A required field " + field + "is missing"
			).rootObject(
				rootObject
			).rootProperty(
				field
			).severity(
				Severity.ERROR
			).build());

		StringBundler sb = new StringBundler(5);

		sb.append("A required field ");
		sb.append(field);
		sb.append(" is missing.");

		_addLogMessageDetails(sb, className, rootObject, field, null);

		_log.error(sb.toString());
	}

	public static Message toErrorMessage(
		String className, Throwable throwable, Object rootObject,
		String rootProperty, String rootValue, String localizationKey) {

		return new Message.Builder().className(
			className
		).localizationKey(
			localizationKey
		).msg(
			_getMsg(throwable, className)
		).rootObject(
			rootObject
		).rootProperty(
			rootProperty
		).rootValue(
			rootValue
		).severity(
			Severity.ERROR
		).throwable(
			throwable
		).build();
	}

	public static void unknownError(
		Messages messages, String className, Throwable throwable,
		Object rootObject, String rootProperty, String rootValue) {

		messages.addMessage(
			new Message.Builder().className(
				className
			).localizationKey(
				"core.error.unknown-error"
			).msg(
				_getMsg(throwable, className)
			).rootObject(
				rootObject
			).rootProperty(
				rootProperty
			).rootValue(
				rootValue
			).severity(
				Severity.ERROR
			).throwable(
				throwable
			).build());

		if (throwable != null) {
			_log.error(throwable.getMessage(), throwable);
		}
		else {
			StringBundler sb = new StringBundler();

			_addLogMessageDetails(
				sb, className, rootObject, rootProperty, rootValue);

			_log.error(sb.toString());
		}
	}

	public static void warning(
		Messages messages, String className, String message, Object rootObject,
		String rootProperty, String rootValue, String localizationKey) {

		messages.addMessage(
			new Message.Builder().className(
				className
			).localizationKey(
				localizationKey
			).msg(
				message
			).rootObject(
				rootObject
			).rootProperty(
				rootProperty
			).rootValue(
				rootValue
			).severity(
				Severity.WARN
			).build());

		StringBundler sb = new StringBundler();

		sb.append("Warning: ");
		sb.append(message);

		_addLogMessageDetails(
			sb, className, rootObject, rootProperty, rootValue);

		if (_log.isWarnEnabled()) {
			_log.warn(sb.toString());
		}
	}

	private static void _addLogMessageDetails(
		StringBundler sb, String className, Object rootObject,
		String rootProperty, String rootValue) {

		if (className != null) {
			sb.append(" Reporting class: ");
			sb.append(className);
		}

		if (rootValue != null) {
			sb.append(" Root value: ");
			sb.append(rootValue);
		}

		if (rootProperty != null) {
			sb.append(" Root property: ");
			sb.append(rootProperty);
		}

		if (rootObject != null) {
			sb.append(" Root object: [ ");
			sb.append(rootObject);
			sb.append(" ]");
		}
	}

	private static String _getMsg(Throwable throwable, String className) {
		if (throwable != null) {
			return throwable.getMessage();
		}

		return className + " reported an error";
	}

	private static final Log _log = LogFactoryUtil.getLog(MessagesUtil.class);

}