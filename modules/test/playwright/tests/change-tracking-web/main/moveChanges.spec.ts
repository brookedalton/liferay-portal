/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {expect, mergeTests} from '@playwright/test';
import {createReadStream} from 'fs';

import {accountSettingsPagesTest} from '../../../fixtures/accountSettingsPagesTest';
import {apiHelpersTest} from '../../../fixtures/apiHelpersTest';
import {changeTrackingPagesTest} from '../../../fixtures/changeTrackingPagesTest';
import {dataApiHelpersTest} from '../../../fixtures/dataApiHelpersTest';
import {featureFlagsTest} from '../../../fixtures/featureFlagsTest';
import {isolatedSiteTest} from '../../../fixtures/isolatedSiteTest';
import {pageEditorPagesTest} from '../../../fixtures/pageEditorPagesTest';
import {pagesAdminPagesTest} from '../../../fixtures/pagesAdminPagesTest';
import {workflowPagesTest} from '../../../fixtures/workflowPagesTest';
import fillAndClickOutside from '../../../utils/fillAndClickOutside';
import {clickAndExpectToBeVisible} from '../../../utils/clickAndExpectToBeVisible';
import getRandomString from '../../../utils/getRandomString';
import {performLoginViaApi, performLogout} from '../../../utils/performLogin';
import {journalPagesTest} from '../../journal-web/main/fixtures/journalPagesTest';
import getDataStructureDefinition from '../../journal-web/main/utils/getDataStructureDefinition';
import {waitForAlert} from '../../../utils/waitForAlert';
import {getWebContentStructureId} from '../../../utils/structured-content/getBasicWebContentStructureId';

export const test = mergeTests(
    accountSettingsPagesTest,
    apiHelpersTest,
    changeTrackingPagesTest,
    dataApiHelpersTest,
    isolatedSiteTest,
    journalPagesTest,
    pagesAdminPagesTest,
    pageEditorPagesTest,
);

test('Cannot move change without parent change', {tag: '@LPD-52950'}, async ({
    apiHelpers,
    changeTrackingPage,
    ctCollection,
    journalEditArticlePage,
    page,
    site
}) => {
    const ctCollection2 =
        await apiHelpers.headlessChangeTracking.createCTCollection(
            getRandomString()
        );

    await changeTrackingPage.workOnPublication(ctCollection);
 
    const structureName = 'Structure 1';
   
    const dataDefinition = getDataStructureDefinition({
        defaultLanguageId: 'en_US',
        fields: [
            {name: 'Text1234'},
            {
                localizable: false,
                name: 'TextNonLocalizable',
                required: false,
            },
        ],
        name: structureName,
    });
   
    await apiHelpers.dataEngine.createStructure(site.id, dataDefinition);
   
    await journalEditArticlePage.goto({
        siteUrl: site.friendlyUrlPath,
        structureName,
    });
   
    const journalArticleTitle = getRandomString();
   
    await journalEditArticlePage.fillTitle(journalArticleTitle);
   
    await journalEditArticlePage.publishArticle();
   
    await waitForAlert(page, `Success:${journalArticleTitle} was created successfully.`);
   
    await changeTrackingPage.goToReviewChanges(ctCollection.body.name);
   
    await changeTrackingPage.viewChanges({
        click: true,
        title: journalArticleTitle,
        type: 'Web Content Article',
    });

    await changeTrackingPage.goToMoveChanges(ctCollection2.body.name);

    await changeTrackingPage.moveChanges({moveFailed: true});

    await apiHelpers.headlessChangeTracking.deleteCTCollection(ctCollection.body.id);
});