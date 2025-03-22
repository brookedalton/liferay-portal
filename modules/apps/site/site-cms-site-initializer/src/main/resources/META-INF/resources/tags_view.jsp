<%--
/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
TagsViewDisplayContext tagsViewDisplayContext = (TagsViewDisplayContext)request.getAttribute(TagsViewDisplayContext.class.getName());
%>

<div class="cms-section">
	<div id="<%= CMSSiteInitializerFDSNames.CATEGORIZATION_SECTION %>">
		<react:component
			module="{TagsView} from site-cms-site-initializer"
			data="<%= tagsViewDisplayContext.getReactData() %>"
		/>
	</div>

	<frontend-data-set:headless-display
		apiURL=""
		bulkActionDropdownItems="<%= tagsViewDisplayContext.getBulkActionDropdownItems() %>"
		creationMenu="<%= tagsViewDisplayContext.getCreationMenu() %>"
		emptyState="<%= tagsViewDisplayContext.getEmptyState() %>"
		formName="fm"
		id="tagsView"
		itemsPerPage="<%= 10 %>"
		propsTransformer="{TagsFDSPropsTransformer} from site-cms-site-initializer"
		selectedItemsKey="id"
		selectionType="multiple"
		showManagementBar="false"
		showSearch="false"
		style="fluid"
	/>
</div>