/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../fixtures/apiHelpersTest';
import {changeTrackingPagesTest} from '../../fixtures/changeTrackingPagesTest';
import {featureFlagsTest} from '../../fixtures/featureFlagsTest';
import {loginTest} from '../../fixtures/loginTest';
import {workflowPagesTest} from '../../fixtures/workflowPagesTest';
import getRandomString from '../../utils/getRandomString';
import {journalPagesTest} from '../journal-web/fixtures/journalPagesTest';

export const test = mergeTests(
	apiHelpersTest,
	featureFlagsTest({
		'LPD-10703': true,
	}),
	journalPagesTest,
	changeTrackingPagesTest,
	workflowPagesTest,
	loginTest()
);

test('LPD-19748 Add workflow info to the View Change screen', async ({
	apiHelpers,
	changeTrackingPage,
	journalEditArticlePage,
	page,
	workflowPage,
}) => {
	await workflowPage.goto();

	await workflowPage.changeWorkflow('Web Content Article', 'Single Approver');

	await changeTrackingPage.enablePublications();

	const publicationName = getRandomString();

	const ctCollection =
		await apiHelpers.headlessChangeTracking.createCTCollection(
			publicationName
		);

	await apiHelpers.headlessChangeTracking.checkoutCTCollection(
		ctCollection.id
	);

	const journalName = getRandomString();

	await journalEditArticlePage.goto();
	await journalEditArticlePage.submitArticleForWorkflow(journalName);

	await changeTrackingPage.goToReviewChanges(publicationName);

	await changeTrackingPage.reviewChange(journalName);

	await test.step('Check for workflow status and tab', async () => {
		await expect(page.getByText(`Workflow status: Pending`)).toBeVisible();

		await changeTrackingPage.viewDisplayTab('Workflow');
	});

	await test.step('Assert workflow data is displayed in tab', async () => {
		const displayData = [
			'Status',
			'Assigned to',
			'Task Name',
			'Create Date',
			'Due Date',
		];

		await changeTrackingPage.selectTab('Workflow');

		for (const data of displayData) {
			await page.getByText(data, {exact: true}).isVisible();
		}
	});

	await test.step('Workflow status is displayed when workflow is disabled', async () => {
		await workflowPage.goto();

		await workflowPage.changeWorkflow(
			'Web Content Article',
			'No Workflow',
			{
				disable: true,
			}
		);

		await changeTrackingPage.goToReviewChanges(publicationName);

		await changeTrackingPage.reviewChange(journalName);

		await expect(page.getByText(`Workflow status: Pending`)).toBeVisible();
	});

	await test.step('Workflow tab is not displayed when workflow is disabled', async () => {
		await changeTrackingPage.viewDisplayTab('Workflow', {isHidden: true});
	});

	await apiHelpers.headlessChangeTracking.deleteCTCollection(ctCollection.id);

	await changeTrackingPage.disablePublications();

	await workflowPage.goto();

	await workflowPage.changeWorkflow('Web Content Article', 'No Workflow', {
		disable: true,
	});
});
