/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {ClayCheckbox} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import ClayModal from '@clayui/modal';
import ClayMultiSelect from '@clayui/multi-select';
import {useFormik} from 'formik';
import {openToast} from 'frontend-js-components-web';
import {fetch} from 'frontend-js-web';
import React, {useEffect, useState} from 'react';

import {getAssetsLibrariesByCompany} from '../../../../api/api';
import SpaceSticker from '../../../components/SpaceSticker';
import {FieldText} from '../../../components/forms';
import {required, validate} from '../../../components/forms/validations';

export default function CreationTagModalContent({
	assetLibraryId = '',
	closeModal,
}: {
	assetLibraryId?: string;

	closeModal: () => void;
}) {
	const [allSpaces, setAllSpaces] = useState<
		{label: string; value: number[]}[]
	>([]);
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
						value: result.map(({id}) => Number(id)),
					},
				]);
				setLoading(false);
			});
		}
	}, [assetLibraryId]);

	const {errors, handleChange, handleSubmit, setFieldValue, touched, values} =
		useFormik({
			initialValues: {
				assetLibraryIds: assetLibraryId ? [Number(assetLibraryId)] : [],
				tagName: '',
			},
			onSubmit: (values) => {
				const url = '/o/headless-admin-taxonomy/v1.0/sites/' +
					Liferay.ThemeDisplay.getScopeGroupId() +
					'/keywords';

				const body = {
					assetLibraryIds: values.assetLibraryIds,
					name: values.tagName,
				};

				fetch(url,
					{
						body: JSON.stringify(body),
						headers: {
							'Content-Type': 'application/json',
						},
						method: 'POST',
					}
				)
					.then((response) => {
						if (response.ok) {
							openToast({
								message: Liferay.Language.get(
									'your-request-completed-successfully'
								),
								title: Liferay.Language.get('success'),
								type: 'success',
							});
						} else {
							openToast({
								message: Liferay.Language.get(
									'an-unexpected-error-occurred'
								),
								title: Liferay.Language.get('error'),
								type: 'danger',
							});
						}
					})
					.catch(() => {
						openToast({
							message: Liferay.Language.get(
								'an-unexpected-error-occurred'
							),
							title: Liferay.Language.get('error'),
							type: 'danger',
						});
					});
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
		setFieldValue(
			'assetLibraryIds',
			selectedItems.map((id) => Number(id))
		);
	};

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
								touched.tagName ? errors.tagName : undefined
							}
							label={Liferay.Language.get('name')}
							name="tagName"
							onChange={handleChange}
							required
							value={values.tagName}
						/>

						<label htmlFor="multiSelect" id="multi-select-label">
							{Liferay.Language.get('space')}

							<span className="ml-1 reference-mark">
								<ClayIcon symbol="asterisk" />
							</span>
						</label>

						{assetLibraries.length > 1 && checkbox && (
							<ClayMultiSelect
								disabled={true}
								id="multiSelect"
								items={allSpaces}
							/>
						)}

						{assetLibraries.length > 1 && !checkbox && (
							<ClayMultiSelect
								disabled={checkbox}
								id="multiSelect"
								loadingState={3}
								onChange={handleChange}
								onItemsChange={handleMultiSelectChange}
								sourceItems={assetLibraries.map(
									({id, name}) => ({
										label: name,
										value: id,
									})
								)}
							>
								{(item) => (
									<ClayMultiSelect.Item
										key={item.value}
										textValue={item.label}
									>
										<div className="autofit-row autofit-row-center">
											<div className="autofit-col mr-3">
												<ClayCheckbox />
											</div>

											<div>
												<SpaceSticker
													name={item.label}
													size="sm"
												/>
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
								onChange={() => setCheckbox(!checkbox)}
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

						<ClayButton displayType="secondary"
							onClick={() => {resetForm();}}
							type="submit"
						>
							{Liferay.Language.get('save-and-add-another')}
						</ClayButton>

						<ClayButton displayType="primary"
							onClick={closeModal}
							type="submit"
						>
							{Liferay.Language.get('save')}
						</ClayButton>
					</ClayButton.Group>
				}
			/>
		</form>
	);
}