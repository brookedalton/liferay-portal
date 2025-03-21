/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {FrontendDataSet} from '@liferay/frontend-data-set-web';
import React from 'react';

import CategorizationToolbar from '../CategorizationToolbar';

export default function TagsView({
	tagsURL,
	vocabularyURL,
}: {
	tagsURL: string;
	vocabularyURL: string;
}) {
	return (
		<div className="categorization-section">
			<CategorizationToolbar
				activeTab="tags"
				tagsURL={tagsURL}
				vocabularyURL={vocabularyURL}
			/>
		</div>
	);
}
