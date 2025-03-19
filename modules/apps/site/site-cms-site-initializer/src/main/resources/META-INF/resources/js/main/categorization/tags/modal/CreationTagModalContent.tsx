/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import ClayModal from '@clayui/modal';
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

	useEffect(() => {
		if (!assetLibraryId) {
			setLoading(true);

			getAssetsLibrariesByCompany().then((result: any) => {
				setAssetsLibraries(result);
				setLoading(false);
			});
		}
	}, [assetLibraryId]);

	const {errors, handleChange, handleSubmit, setFieldValue, touched, values} =
		useFormik({
			initialValues: {
				assetLibraryId:
					assetLibraryId || assetLibraries.length === 1
						? assetLibraries[0].id
						: '',
				tagName: '',
			},
			onSubmit: (values) => {
				alert(JSON.stringify(values, null, 4));
			},
			validate: (values) =>
				validate(
					{
						assetLibraryId: [required],
						tagName: [required],
					},
					values
				),
		});

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

						{assetLibraries.length > 1 && (
							<FieldPicker
								errorMessage={
									touched.assetLibraryId
										? errors.assetLibraryId
										: undefined
								}
								helpMessage={Liferay.Language.get(
									'choose-the-space-for-the-new-tag'
								)}
								items={assetLibraries.map(({id, name}) => ({
									label: name,
									value: id,
								}))}
								label={Liferay.Language.get('space')}
								name="tagName"
								onSelectionChange={(value: string) => {
									setFieldValue('assetLibraryId', value);
								}}
								placeholder={Liferay.Language.get(
									'select-a-space'
								)}
								required
								selectedKey={values.assetLibraryId}
							/>
						)}
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

						<ClayButton displayType="primary" type="submit">
							{Liferay.Language.get('save')}
						</ClayButton>
					</ClayButton.Group>
				}
			/>
		</form>
	);
}
