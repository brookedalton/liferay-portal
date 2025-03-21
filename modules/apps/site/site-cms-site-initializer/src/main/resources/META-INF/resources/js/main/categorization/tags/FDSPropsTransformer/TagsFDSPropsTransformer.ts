/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import addOnClickToCreationMenuItems from '../../../FDSPropsTransformer/utils/addOnClickToCreationMenuItems';
import createTagAction from './actions/createTagAction';

const ACTIONS = {
	createTag: createTagAction,
};

export default function TagsFDSPropsTransformer({
	creationMenu,
	...otherProps
}: {
	creationMenu: any;
	otherProps: any;
}) {
	return {
		...otherProps,
		creationMenu: {
			...creationMenu,
			primaryItems: addOnClickToCreationMenuItems(
				creationMenu.primaryItems,
				ACTIONS
			),
		},
	};
}
