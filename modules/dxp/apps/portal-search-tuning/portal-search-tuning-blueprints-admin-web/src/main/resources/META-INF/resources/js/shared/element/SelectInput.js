/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 */

import {ClaySelect} from '@clayui/form';
import React, {useRef} from 'react';

import NullableCheckbox from './NullableCheckbox';

function SelectInput({
	disabled,
	label,
	name,
	nullable,
	onBlur,
	onChange,
	options = [],
	setFieldValue,
	value,
}) {
	const selectRef = useRef(value || options[0]?.value || '');

	return (
		<>
			<ClaySelect
				aria-label={label}
				className="form-control-sm"
				disabled={disabled || value === null}
				name={name}
				onBlur={onBlur}
				onChange={(event) => {
					selectRef.current = event.target.value;
					onChange(event);
				}}
				value={value || selectRef.current}
			>
				{options.map((item) => (
					<ClaySelect.Option
						key={item.value}
						label={item.label}
						value={item.value}
					/>
				))}
			</ClaySelect>

			{nullable && (
				<NullableCheckbox
					defaultValue={selectRef.current}
					disabled={disabled}
					onChange={(val) => setFieldValue(name, val)}
					value={value}
				/>
			)}
		</>
	);
}

export default React.memo(SelectInput);
