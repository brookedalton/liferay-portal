/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.service.impl;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.VirtualHost;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.kernel.service.persistence.LayoutSetPersistence;

/**
 * @author Brooke Dalton
 */
public class VirtualHostModelListener extends BaseModelListener<VirtualHost> {

	@Override
	public void onAfterCreate(VirtualHost virtualHost) {
		Company company = CompanyLocalServiceUtil.fetchCompanyById(
			virtualHost.getCompanyId());

		if (company == null) {
			return;
		}

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					virtualHost.getCtCollectionId())) {

			LayoutSet layoutSet = LayoutSetLocalServiceUtil.fetchLayoutSet(
				virtualHost.getHostname());

			_layoutSetPersistence.update(layoutSet);
		}
	}

	@Override
	public void onAfterRemove(VirtualHost virtualHost)
		throws ModelListenerException {

		Company company = CompanyLocalServiceUtil.fetchCompanyById(
			virtualHost.getCompanyId());

		if (company == null) {
			return;
		}

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					virtualHost.getCtCollectionId())) {

			LayoutSet layoutSet = LayoutSetLocalServiceUtil.fetchLayoutSet(
				virtualHost.getHostname());

			_layoutSetPersistence.update(layoutSet);
		}
	}

	@BeanReference(type = LayoutSet.class)
	private LayoutSetPersistence _layoutSetPersistence;

}