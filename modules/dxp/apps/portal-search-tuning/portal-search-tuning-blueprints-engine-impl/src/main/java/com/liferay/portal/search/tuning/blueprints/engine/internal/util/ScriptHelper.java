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
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.search.script.Script;
import com.liferay.portal.search.script.ScriptBuilder;
import com.liferay.portal.search.script.ScriptType;
import com.liferay.portal.search.script.Scripts;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.ScriptConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(immediate = true, service = ScriptHelper.class)
public class ScriptHelper {

	public Optional<Script> getScript(
		JSONObject jsonObject, Messages messages) {

		if (jsonObject.length() == 0) {
			return Optional.empty();
		}

		ScriptBuilder scriptBuilder = _scripts.builder();

		_setIdOrSource(scriptBuilder, jsonObject, messages);

		_setLang(scriptBuilder, jsonObject);

		_setOptions(scriptBuilder, jsonObject);

		_setParams(scriptBuilder, jsonObject);

		return Optional.of(scriptBuilder.build());
	}

	public Optional<Script> getScript(String scriptString, Messages messages) {
		if (Validator.isBlank(scriptString)) {
			return Optional.empty();
		}

		ScriptBuilder scriptBuilder = _scripts.builder();

		scriptBuilder.idOrCode(
			scriptString
		).scriptType(
			ScriptType.INLINE
		);

		return Optional.of(scriptBuilder.build());
	}

	private Optional<Script> _returnIdOrSourceMissing(
		JSONObject jsonObject, Messages messages) {

		MessagesUtil.requiredFieldMissingError(
			messages, getClass().getName(), jsonObject,
			ScriptConfigurationKeys.ID.getJsonKey() + " or " +
				ScriptConfigurationKeys.SOURCE.getJsonKey());

		return Optional.empty();
	}

	private void _setIdOrSource(
		ScriptBuilder scriptBuilder, JSONObject jsonObject, Messages messages) {

		if (jsonObject.has(ScriptConfigurationKeys.ID.getJsonKey())) {
			scriptBuilder.idOrCode(
				jsonObject.getString(ScriptConfigurationKeys.ID.getJsonKey())
			).scriptType(
				ScriptType.STORED
			);
		}
		else if (jsonObject.has(ScriptConfigurationKeys.SOURCE.getJsonKey())) {
			scriptBuilder.idOrCode(
				jsonObject.getString(
					ScriptConfigurationKeys.SOURCE.getJsonKey())
			).scriptType(
				ScriptType.INLINE
			);
		}
		else {
			_returnIdOrSourceMissing(jsonObject, messages);
		}
	}

	private void _setLang(ScriptBuilder scriptBuilder, JSONObject jsonObject) {
		if (!jsonObject.has(ScriptConfigurationKeys.LANG.getJsonKey())) {
			return;
		}

		scriptBuilder.language(
			jsonObject.getString(ScriptConfigurationKeys.LANG.getJsonKey()));
	}

	private void _setOptions(
		ScriptBuilder scriptBuilder, JSONObject jsonObject) {

		if (!jsonObject.has(ScriptConfigurationKeys.OPTIONS.getJsonKey())) {
			return;
		}

		JSONObject optionsJSONObject = jsonObject.getJSONObject(
			ScriptConfigurationKeys.OPTIONS.getJsonKey());

		optionsJSONObject.keySet(
		).stream(
		).forEach(
			key -> scriptBuilder.putParameter(key, optionsJSONObject.get(key))
		);
	}

	private void _setParams(
		ScriptBuilder scriptBuilder, JSONObject jsonObject) {

		if (!jsonObject.has(ScriptConfigurationKeys.PARAMS.getJsonKey())) {
			return;
		}

		JSONObject paramsJSONObject = jsonObject.getJSONObject(
			ScriptConfigurationKeys.PARAMS.getJsonKey());

		paramsJSONObject.keySet(
		).stream(
		).forEach(
			key -> scriptBuilder.putParameter(key, paramsJSONObject.get(key))
		);
	}

	@Reference
	private Scripts _scripts;

}