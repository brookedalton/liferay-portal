<%--
/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
TagsSectionDisplayContext tagsSectionDisplayContext = (TagsSectionDisplayContext)request.getAttribute(TagsSectionDisplayContext.class.getName());
%>

<div class="cms-section">
	<frontend-data-set:headless-display
		apiURL="<%= tagsSectionDisplayContext.getAPIURL() %>"
		bulkActionDropdownItems="<%= tagsSectionDisplayContext.getBulkActionDropdownItems() %>"
		creationMenu="<%= tagsSectionDisplayContext.getCreationMenu() %>"
		emptyState="<%= tagsSectionDisplayContext.getEmptyState() %>"
		fdsActionDropdownItems="<%= tagsSectionDisplayContext.getFDSActionDropdownItems() %>"
		formName="fm"
		id="<%= CMSSiteInitializerFDSNames.TAGS_SECTION %>"
		itemsPerPage="<%= 10 %>"
		propsTransformer="{TagsFDSPropsTransformer} from site-cms-site-initializer"
		selectedItemsKey="id"
		selectionType="multiple"
		style="fluid"
	/>
</div>