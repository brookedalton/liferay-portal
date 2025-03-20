/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClayForm, {ClayCheckbox, ClayInput} from '@clayui/form';
import ClayMultiSelect from '@clayui/multi-select';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import ClayModal from '@clayui/modal';
import Label from '@clayui/label';
import {useFormik} from 'formik';
import React, {useEffect, useState} from 'react';

import {getAssetsLibrariesByCompany} from '../../../../api/api';
import {FieldPicker, FieldText} from '../../../components/forms';
import {required, validate} from '../../../components/forms/validations';

export default function CreationTagModalContent({
	assetLibraryId = '',
	closeModal,
}: {
	assetLibraryId?: string;
	closeModal: () => void;
}) {
	const [assetLibraries, setAssetsLibraries] = useState<
		{id: string; name: string}[]
	>([]);
	const [loading, setLoading] = useState(false);
	const [checkbox, setCheckbox] = useState(true);

	useEffect(() => {
		if (!assetLibraryId) {
			setLoading(true);

			getAssetsLibrariesByCompany().then((result: any) => {
				setAssetsLibraries(result);
				setAllSpaces([
					{
						label: 'All Spaces',
						value: result.map(({id}) => id),
					},
				]);
				setLoading(false);
			});
		}
	}, [assetLibraryId]);

	const {errors, handleChange, handleSubmit, setFieldValue, touched, values} =
		useFormik({
			initialValues: {
				assetLibraryIds: assetLibraryId ?
					[assetLibraryId] : [],
				tagName: '',
			},
			onSubmit: (values) => {
				alert(JSON.stringify(values, null, 4));
			},
			validate: (values) => {
				if (!checkbox) {
					validate(
						{
							assetLibraryIds: [required],
							tagName: [required],
						},
						values
					);
				}
			},
		});

	const handleMultiSelectChange = (selectedItems: string[]) => {
		setFieldValue('assetLibraryIds', selectedItems);
    };

	const [allSpaces, setAllSpaces] = useState<
		{label: string; value: string}[]
	>([]);

	useEffect(() => {
		if (checkbox) {
			setFieldValue(
				'assetLibraryIds',
				allSpaces.flatMap((space) => space.value)
			);
		}
	}, [checkbox, allSpaces, setFieldValue]);

	return (
		<form onSubmit={handleSubmit}>
			<ClayModal.Header>
				{Liferay.Language.get('new-tag')}
			</ClayModal.Header>

			<ClayModal.Body>
				{loading ? (
					<div className="loader-container">
						<ClayLoadingIndicator />
					</div>
				) : (
					<>
						<FieldText
							errorMessage={
								touched.tagName
									? errors.tagName
									: undefined
							}
							label={Liferay.Language.get('name')}
							name="tagName"
							onChange={handleChange}
							required
							value={values.tagName}
						/>

					<label htmlFor="multiSelect" id="multi-select-label">
						{Liferay.Language.get('space')}
						<span className="reference-mark ml-1">
							<ClayIcon symbol="asterisk" />
						</span>
					</label>

					{assetLibraries.length > 1 && checkbox && (
						<ClayMultiSelect
							id="multiSelect"
							items={allSpaces}
							disabled={true}
							value={values.assetLibraryId}
						/>
					)}

					{assetLibraries.length > 1 && !checkbox && (
                    	<ClayMultiSelect
                    		loadingState={3}
                    		id="multiSelect"
                    		disabled={checkbox}
                    		sourceItems={assetLibraries.map(({id, name}) => ({
                    			label: name,
                    			value: id,
                    		}))}
                    		onChange={handleChange}
                    		onItemsChange={handleMultiSelectChange}
                    		value={values.assetLibraryId}
                    	>
							{(item) => (
								<ClayMultiSelect.Item
									key={item.value}
									textValue={item.label}
								>
									<div className="autofit-row autofit-row-center">
										<div className="autofit-col mr-3">
											<ClayCheckbox/>
										</div>
										<div className="autofit-col">
											<strong>{item.label}</strong>
										</div>
									</div>
								</ClayMultiSelect.Item>
								)}
							</ClayMultiSelect>
						)}

						<div className="mt-2">
							<ClayCheckbox
								checked={checkbox}
								label={Liferay.Language.get(
									'make-this-tag-available-in-all-spaces'
								)}
								onChange={() =>
									setCheckbox(!checkbox)
								}
							/>
							</div>
						</>
					)}
			</ClayModal.Body>

			<ClayModal.Footer
				last={
					<ClayButton.Group spaced>
						<ClayButton
							displayType="secondary"
							onClick={closeModal}
							type="button"
						>
							{Liferay.Language.get('cancel')}
						</ClayButton>

						<ClayButton
							displayType="secondary"
							type="button"
						>
							{Liferay.Language.get('save-and-add-another')}
						</ClayButton>

						<ClayButton displayType="primary" type="submit">
							{Liferay.Language.get('save')}
						</ClayButton>
					</ClayButton.Group>
				}
			/>
		</form>
	);
}
