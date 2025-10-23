/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';
import path from 'path';

import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {changeTrackingPagesTest} from '../../../fixtures/changeTrackingPagesTest';
import {documentLibraryPagesTest} from '../../../fixtures/documentLibraryPages.fixtures';
import getRandomString from '../../../utils/getRandomString';
import {waitForAlert} from '../../../utils/waitForAlert';
import {blogsPagesTest} from '../../blogs-web/main/fixtures/blogsPagesTest';

export const test = mergeTests(
	apiHelpersTest,
	blogsPagesTest,
	changeTrackingPagesTest,
	documentLibraryPagesTest
);

test.beforeEach(
	async ({
		apiHelpers,
		ctCollection,
		documentLibraryPage,
		documentLibraryEditFilePage,
	}) => {
	await apiHelpers.headlessChangeTracking.checkoutCTCollection(
		ctCollection.body.id
	);

	await documentLibraryPage.goto();
	await documentLibraryPage.goToCreateNewFile();

	await documentLibraryEditFilePage.goToNewFileDifferentType(
			'Files Upload'
		);

	await documentLibraryEditFilePage.publishMultipleFiles("Basic Document", [
		path.join(__dirname, '/dependencies/Document_1.mp4'),
	]);

});

test('Blog With Embedded Video is Visible in Publication', async ({
	blogsPage,
	changeTrackingPage,
	ctCollection,
	page,
}) => {
	await blogsPage.goto();

	await blogsPage.goToCreateBlogEntry();

	const blogTitle = getRandomString();

	await page.getByPlaceholder('Title *').fill(blogTitle);

	await page.locator(
			'#_com_liferay_blogs_web_portlet_BlogsAdminPortlet_contentEditor.cke_editable'
	).click();

	await page.getByRole('button', {name: 'Add'}).click();

	await page.getByRole('button', {name: 'Insert Video'}).click();

	const iframe = page.frameLocator('iframe[title="Select Item"]');

	await iframe.getByRole('link', {name: 'Documents and Media'}).click();

	const imageCard = iframe.getByText("Document_1");

	await imageCard.waitFor({state: 'visible'});

	await imageCard.click();

	await page.getByRole('button', {name: 'Publish'}).click();

	await waitForAlert(page);

	await changeTrackingPage.goToReviewChanges(ctCollection.body.name);

	await changeTrackingPage.reviewChange(blogTitle);

	const response = await page.waitForResponse(response => 
		response.url().includes(`*/previewCTCollectionId=${ctCollection.body.id}&videoPreview=1&type=mp4&p_l_mode=preview`) && response.request().resourceType() === 'video/mp4'
	);

	await expect(response.status()).toBe(200);
});