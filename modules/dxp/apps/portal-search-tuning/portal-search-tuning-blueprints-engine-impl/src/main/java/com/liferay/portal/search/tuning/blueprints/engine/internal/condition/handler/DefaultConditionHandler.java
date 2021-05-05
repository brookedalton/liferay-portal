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

package com.liferay.portal.search.tuning.blueprints.engine.internal.condition.handler;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.tuning.blueprints.constants.json.keys.query.ConditionConfigurationKeys;
import com.liferay.portal.search.tuning.blueprints.constants.json.values.EvaluationType;
import com.liferay.portal.search.tuning.blueprints.engine.exception.ParameterEvaluationException;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.visitor.AnyWordInVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.visitor.ContainsVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.visitor.EqualsVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.visitor.GreaterThanVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.visitor.InRangeVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.internal.condition.visitor.InVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ConditionEvaluationVisitor;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.Parameter;
import com.liferay.portal.search.tuning.blueprints.engine.parameter.ParameterData;
import com.liferay.portal.search.tuning.blueprints.engine.spi.clause.ConditionHandler;
import com.liferay.portal.search.tuning.blueprints.engine.template.variable.BlueprintTemplateVariableParser;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.util.util.BlueprintJSONValidationUtil;
import com.liferay.portal.search.tuning.blueprints.util.util.MessagesUtil;

import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Petteri Karttunen
 */
@Component(
	immediate = true, property = "name=default",
	service = ConditionHandler.class
)
public class DefaultConditionHandler implements ConditionHandler {

	@Override
	public boolean isTrue(
		JSONObject jsonObject, ParameterData parameterData, Messages messages) {

		if (!_validateCondition(messages, jsonObject)) {
			return false;
		}

		EvaluationType evaluationType = _resolveEvaluationType(
			jsonObject, messages);

		if (evaluationType == null) {
			return false;
		}

		Optional<Parameter> parameterOptional =
			parameterData.getByTemplateVariableNameOptional(
				jsonObject.getString(
					ConditionConfigurationKeys.PARAMETER_NAME.getJsonKey()));

		if (EvaluationType.EXISTS.equals(evaluationType) ||
			EvaluationType.NOT_EXISTS.equals(evaluationType)) {

			return _evaluateExistsCondition(evaluationType, parameterOptional);
		}

		if (!parameterOptional.isPresent()) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"Clause condition parameter is not present [ " +
						jsonObject + " ].");
			}

			return false;
		}

		Optional<Object> parsedValueOptional =
			_blueprintTemplateVariableParser.parse(
				jsonObject.get(ConditionConfigurationKeys.VALUE.getJsonKey()),
				parameterData, messages);

		if (!parsedValueOptional.isPresent()) {
			return false;
		}

		jsonObject.put(
			ConditionConfigurationKeys.VALUE.getJsonKey(),
			parsedValueOptional.get());

		Parameter parameter = parameterOptional.get();

		ConditionEvaluationVisitor conditionEvaluationVisitor = _resolveVisitor(
			parameter, jsonObject, evaluationType, messages);

		if (conditionEvaluationVisitor == null) {
			return false;
		}

		return _evaluate(parameter, conditionEvaluationVisitor, messages);
	}

	private boolean _evaluate(
		Parameter parameter, ConditionEvaluationVisitor visitor,
		Messages messages) {

		try {
			return parameter.accept(visitor);
		}
		catch (ParameterEvaluationException parameterEvaluationException) {
			_log.error(
				parameterEvaluationException.getMessage(),
				parameterEvaluationException);

			messages.addMessage(
				parameterEvaluationException.getDetailsMessage());

			return false;
		}
	}

	private boolean _evaluateExistsCondition(
		EvaluationType evaluationType, Optional<Parameter> parameterOptional) {

		if (EvaluationType.EXISTS.equals(evaluationType)) {
			if (parameterOptional.isPresent()) {
				return true;
			}

			return false;
		}

		if (!parameterOptional.isPresent()) {
			return true;
		}

		return false;
	}

	private ConditionEvaluationVisitor _getEvaluationVisitor(
		Parameter parameter, JSONObject jsonObject,
		EvaluationType evaluationType) {

		ConditionEvaluationVisitor visitor = null;

		List<EvaluationType> supportedEvaluationTypes =
			parameter.getSupportedEvaluationTypes();

		if (EvaluationType.ANY_WORD_IN.equals(evaluationType) &&
			supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new AnyWordInVisitor(jsonObject, false);
		}
		else if (EvaluationType.CONTAINS.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new ContainsVisitor(jsonObject, false);
		}
		else if (EvaluationType.EQ.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new EqualsVisitor(jsonObject, false);
		}
		else if (EvaluationType.GT.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new GreaterThanVisitor(jsonObject, false, false);
		}
		else if (EvaluationType.GTE.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new GreaterThanVisitor(jsonObject, false, true);
		}
		else if (EvaluationType.IN.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new InVisitor(jsonObject, false);
		}
		else if (EvaluationType.IN_RANGE.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new InRangeVisitor(jsonObject, false);
		}
		else if (EvaluationType.LT.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new GreaterThanVisitor(jsonObject, true, false);
		}
		else if (EvaluationType.LTE.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new GreaterThanVisitor(jsonObject, true, true);
		}
		else if (EvaluationType.NE.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new EqualsVisitor(jsonObject, true);
		}
		else if (EvaluationType.NO_WORD_IN.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new AnyWordInVisitor(jsonObject, true);
		}
		else if (EvaluationType.NOT_CONTAINS.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new ContainsVisitor(jsonObject, true);
		}
		else if (EvaluationType.NOT_IN.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new InRangeVisitor(jsonObject, true);
		}
		else if (EvaluationType.NOT_IN_RANGE.equals(evaluationType) &&
				 supportedEvaluationTypes.contains(evaluationType)) {

			visitor = new InRangeVisitor(jsonObject, true);
		}

		return visitor;
	}

	private EvaluationType _resolveEvaluationType(
		JSONObject jsonObject, Messages messages) {

		String s = jsonObject.getString(
			ConditionConfigurationKeys.EVALUATION_TYPE.getJsonKey());

		try {
			return EvaluationType.valueOf(StringUtil.toUpperCase(s));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			MessagesUtil.invalidConfigurationValueError(
				messages, getClass().getName(), illegalArgumentException,
				jsonObject,
				ConditionConfigurationKeys.EVALUATION_TYPE.getJsonKey(), s);
		}

		return null;
	}

	private ConditionEvaluationVisitor _resolveVisitor(
		Parameter parameter, JSONObject jsonObject,
		EvaluationType evaluationType, Messages messages) {

		ConditionEvaluationVisitor visitor = _getEvaluationVisitor(
			parameter, jsonObject, evaluationType);

		if (visitor != null) {
			return visitor;
		}

		MessagesUtil.invalidConfigurationValueError(
			messages, getClass().getName(),
			new Throwable("Evaluation visitor could not be resolved"),
			jsonObject, ConditionConfigurationKeys.EVALUATION_TYPE.getJsonKey(),
			evaluationType.name());

		return null;
	}

	private boolean _validateCondition(
		Messages messages, JSONObject jsonObject) {

		return BlueprintJSONValidationUtil.validateRequiredFieldsPresent(getClass().getName(),
			jsonObject, messages,
			ConditionConfigurationKeys.PARAMETER_NAME.getJsonKey(),
			ConditionConfigurationKeys.EVALUATION_TYPE.getJsonKey());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DefaultConditionHandler.class);

	@Reference
	private BlueprintTemplateVariableParser _blueprintTemplateVariableParser;

}