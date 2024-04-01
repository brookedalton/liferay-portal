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

	const ctCollection =
		await apiHelpers.headlessChangeTracking.createCTCollection(
			'Publication Name'
		);

	await apiHelpers.headlessChangeTracking.checkoutCTCollection(
		ctCollection.id
	);

	await journalEditArticlePage.goto();
	await journalEditArticlePage.submitArticleForWorkflow(
		'Basic Web Content Article'
	);

	await changeTrackingPage.goToReviewChanges('Publication Name');

	await changeTrackingPage.reviewChange('Basic Web Content Article');

	await expect(page.getByText(`Workflow status: Pending`)).toBeVisible();

	await changeTrackingPage.viewDisplayTab('Workflow');
});

test('LPD-19748 Workflow data is displayed in tab', async ({
	changeTrackingPage,
	page,
}) => {
	const displayData = [
		'Status',
		'Assigned to',
		'Task Name',
		'Create Date',
		'Due Date',
	];

	await changeTrackingPage.goToReviewChanges('Publication Name');

	await changeTrackingPage.reviewChange('Basic Web Content Article');

	await changeTrackingPage.selectTab('Workflow');

	for (const data of displayData) {
		await page.getByText(data, {exact: true}).isVisible();
	}
});

test('LPD-19748 Workflow status is displayed but not tab when workflow is not enabled', async ({
	changeTrackingPage,
	page,
	workflowPage,
}) => {
	await workflowPage.goto();

	await workflowPage.changeWorkflow('Web Content Article', 'No Workflow', {
		disable: true,
	});

	await changeTrackingPage.goToReviewChanges('Publication Name');

	await changeTrackingPage.reviewChange('Basic Web Content Article');

	await expect(page.getByText(`Workflow status: Pending`)).toBeVisible();

	await changeTrackingPage.viewDisplayTab('Workflow', {isHidden: true});
});
