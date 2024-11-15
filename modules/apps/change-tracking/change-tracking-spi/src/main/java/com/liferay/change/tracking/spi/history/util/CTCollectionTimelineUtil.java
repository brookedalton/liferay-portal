/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.change.tracking.spi.history.util;

import com.liferay.change.tracking.spi.constants.CTTimelineKeys;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Cheryl Tang
 */
public class CTCollectionTimelineUtil {

	/**
	 * Set the CT Timeline Key for only the class name. Used for aggregate timeline views.
	 * @param httpServletRequest httpServletRequest the servlet request used to associate the
	 *    <code>CTTimelineKeys</code>
	 * @param clazz the class whose name will be associated with the request
	 */
	public void setClassName(
		HttpServletRequest httpServletRequest, Class<?> clazz) {

		httpServletRequest.setAttribute(
			CTTimelineKeys.CLASS_NAME, clazz.getName());
	}

	/**
	 * Sets the CT Timeline Keys for both class name and class PK. Used for views of a specific entity.
	 * @param httpServletRequest httpServletRequest the servlet request used to associate the
	 *    <code>CTTimelineKeys</code>
	 * @param clazz the class whose name will be associated with the request
	 * @param classPK the PK of the entity of type <code>clazz</code> that will be associated with the request
	 */
	public void setCTTimelineKeys(
		HttpServletRequest httpServletRequest, Class<?> clazz, long classPK) {

		setClassName(httpServletRequest, clazz);

		httpServletRequest.setAttribute(CTTimelineKeys.CLASS_PK, classPK);
	}

}