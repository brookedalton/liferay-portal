/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {openModal} from 'frontend-js-components-web';

import CreationTagModalContent from '../../categorization/tags/modal/CreationTagModalContent';

export default function createTagAction(data: {assetLibraryId?: string}) {
	openModal({
		contentComponent: ({closeModal}: {closeModal: () => void}) =>
			CreationTagModalContent({
				...data,
				closeModal,
			}),
		size: 'md',
	});
}
