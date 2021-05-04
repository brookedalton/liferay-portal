
package com.liferay.portal.search.tuning.blueprints.util.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.search.tuning.blueprints.message.Message;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.message.Severity;

public class MessagesUtil {

	public static void error(
		Messages messages, Throwable throwable, Object rootObject,
		String rootProperty, String rootValue, String localizationKey) {

		messages.addMessage(
			new Message.Builder().className(
				MessagesUtil.class.getName()
			).localizationKey(
				localizationKey
			).msg(
				_getMsg(throwable)
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

			sb.append("There was an error. ");

			_addLogMessageDetails(sb, rootObject, rootProperty, rootValue);

			_log.error(sb.toString());
		}
	}

	public static void invalidConfigurationValueError(
		Throwable throwable, Messages messages, Object rootObject,
		String rootProperty, String rootValue) {

		messages.addMessage(
			new Message.Builder().className(
				MessagesUtil.class.getName()
			).localizationKey(
				"core.error.invalid-configuration-value"
			).msg(
				_getMsg(throwable)
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

		_addLogMessageDetails(sb, rootObject, rootProperty, rootValue);

		_log.error(sb.toString());
	}

	public static void invalidConfigurationValueTypeError(
		Messages messages, String correctType, Object rootObject,
		String rootProperty, String rootValue) {

		messages.addMessage(
			new Message.Builder().className(
				MessagesUtil.class.getName()
			).localizationKey(
				"core.error.invalid-configuration-value-type"
			).msg(
				"Invalid type in configuration value  " + rootValue + ". " +
					correctType + " expected."
			).rootObject(
				rootObject
			).rootProperty(
				rootProperty
			).rootValue(
				rootValue
			).severity(
				Severity.ERROR
			).build());

		StringBundler sb = new StringBundler(9);

		sb.append("Invalid configuration value type. ");
		sb.append(correctType);
		sb.append(" expected.");

		_addLogMessageDetails(sb, rootObject, rootProperty, rootValue);

		_log.error(sb.toString());
	}

	public static void requiredFieldMissingError(
		Messages messages, Object rootObject, String field) {

		messages.addMessage(
			new Message.Builder().className(
				MessagesUtil.class.getName()
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
		_addLogMessageDetails(sb, rootObject, null, null);

		_log.error(sb.toString());
	}

	public static Message toErrorMessage(
		Throwable throwable, Object rootObject, String rootProperty,
		String rootValue, String localizationKey) {

		return new Message.Builder().className(
			MessagesUtil.class.getName()
		).localizationKey(
			localizationKey
		).msg(
			_getMsg(throwable)
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
		Throwable throwable, Messages messages, Object rootObject,
		String rootProperty, String rootValue) {

		messages.addMessage(
			new Message.Builder().className(
				MessagesUtil.class.getName()
			).localizationKey(
				"core.error.unknown-error"
			).msg(
				_getMsg(throwable)
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

			sb.append("There was an unknown error. ");

			_addLogMessageDetails(sb, rootObject, rootProperty, rootValue);

			_log.error(sb.toString());
		}
	}

	public static void warning(
		Messages messages, String message, Object rootObject,
		String rootProperty, String rootValue, String localizationKey) {

		messages.addMessage(
			new Message.Builder().className(
				MessagesUtil.class.getName()
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
				Severity.ERROR
			).build());

		if (_log.isWarnEnabled()) {
			StringBundler sb = new StringBundler();

			sb.append("Warning: ");
			sb.append(message);

			_addLogMessageDetails(sb, rootObject, rootProperty, rootValue);

			_log.warn(message);
		}
	}

	private static void _addLogMessageDetails(
		StringBundler sb, Object rootObject, String rootProperty,
		String rootValue) {

		if (rootValue != null) {
			sb.append(" Root value: ");
			sb.append(rootValue);
		}

		if (rootProperty != null) {
			sb.append(", Root property: ");
			sb.append(rootProperty);
		}

		if (rootObject != null) {
			sb.append(", Root object: [ ");
			sb.append(rootProperty);
			sb.append(" ]");
		}
	}

	private static String _getMsg(Throwable throwable) {
		if (throwable != null) {
			return throwable.getMessage();
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(MessagesUtil.class);

}