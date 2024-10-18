/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../fixtures/apiHelpersTest';
import {changeTrackingPagesTest} from '../../fixtures/changeTrackingPagesTest';
import {isolatedSiteTest} from '../../fixtures/isolatedSiteTest';
import {productMenuPageTest} from '../../fixtures/productMenuPageTest';
import getRandomString from '../../utils/getRandomString';
import performLogin, {performLogout} from '../../utils/performLogin';
import {waitForAlert} from '../../utils/waitForAlert';

export const test = mergeTests(
	apiHelpersTest,
	changeTrackingPagesTest,
	isolatedSiteTest,
	productMenuPageTest
);

test('LPD-30098 Invite user as admin', async ({
	apiHelpers,
	changeTrackingPage,
	ctCollection,
	page,
	site,
}) => {
	const user1 = await changeTrackingPage.addUserWithPublicationsUserRole();

	const user2 = await changeTrackingPage.addUserWithPublicationsUserRole();

	await changeTrackingPage.addUserToPublication(
		ctCollection.name,
		'Admin',
		user1
	);

	const title = getRandomString();

	await changeTrackingPage.addBasicWebContent(site, title);

	await performLogout(page);

	await performLogin(page, user1.alternateName);

	await page.getByTestId('userPersonalMenu').click();

	await page.getByRole('menuitem', {name: 'Notifications'}).click();

	await expect(
		page.getByText(
			`has invited you to work on ${ctCollection.name} as a Admin.`
		)
	).toBeVisible();

	await changeTrackingPage.goto();

	await page.waitForLoadState('load');

	await page.getByRole('button', {name: 'Actions'}).click();

	await page.getByRole('menuitem', {name: 'Edit'}).click();

	await page.getByPlaceholder('Enter the name of the').fill(title);

	await page.getByRole('button', {name: 'Save'}).click();

	await waitForAlert(page, 'Success:Successfully updated');

	await changeTrackingPage.addUserToPublication(title, 'Admin', user2);

	await changeTrackingPage.assertPublicationCommentsCRUDPermissions();

	await page.reload();

	await page.getByRole('link', {name: 'Publish'}).click();

	await page.getByRole('button', {name: 'Publish'}).click();

	await expect(page.getByRole('link', {name: 'History'})).toBeVisible();

	await expect(
		page.locator('div').filter({hasText: title}).first()
	).toBeVisible();

	await waitForAlert(page, 'Success:Your request completed successfully.');

	await performLogout(page);

	await performLogin(page, 'test');

	await apiHelpers.headlessAdminUser.deleteUserAccount(Number(user1.id));
	await apiHelpers.headlessAdminUser.deleteUserAccount(Number(user2.id));

	await apiHelpers.headlessChangeTracking.deleteCTCollection(ctCollection.id);
});
