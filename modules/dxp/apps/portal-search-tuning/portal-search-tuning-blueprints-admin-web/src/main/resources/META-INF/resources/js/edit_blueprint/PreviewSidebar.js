/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import {Align} from '@clayui/drop-down';
import ClayEmptyState from '@clayui/empty-state';
import ClayIcon from '@clayui/icon';
import ClayLink from '@clayui/link';
import ClayList from '@clayui/list';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import ClayManagementToolbar from '@clayui/management-toolbar';
import {ClayPaginationWithBasicItems} from '@clayui/pagination';
import ClayPaginationBar from '@clayui/pagination-bar';
import getCN from 'classnames';
import PropTypes from 'prop-types';
import React, {useState} from 'react';

import CodeMirrorEditor from '../shared/CodeMirrorEditor';
import PreviewModal from '../shared/PreviewModal';
import SearchInput from '../shared/SearchInput';
import useDidUpdateEffect from '../utils/useDidUpdateEffect';
import {openSuccessToast, sub} from '../utils/utils';
import ErrorListItem from './ErrorListItem';
import ResultListItem from './ResultListItem';

const DELTAS = [10, 20, 30, 50];
const RAW_RESPONSE_FILE_NAME = 'raw_response.json';

function PreviewSidebar({
	loading,
	onFetchResults,
	onFocusElement,
	onToggle,
	results,
	visible,
}) {
	const [value, setValue] = useState('');
	const [activePage, setActivePage] = useState(1);
	const [activeDelta, setActiveDelta] = useState(10);

	const _handleFetch = () => {
		onFetchResults(value, activeDelta, activePage);
	};

	useDidUpdateEffect(() => {
		_handleFetch();
	}, [activeDelta, activePage]);

	const _handleDeltaChange = (delta) => () => {
		setActiveDelta(delta);
		setActivePage(1);
	};

	const _onCopyToClipboard = () => {
		navigator.clipboard.writeText(JSON.stringify(results, null, 2));

		openSuccessToast({
			message: Liferay.Language.get('copied-to-clipboard'),
		});
	};

	const _renderErrors = () => (
		<ClayList className="preview-error-list">
			{results.errors.map((error, index) => (
				<ErrorListItem
					item={error}
					key={index}
					onFocusElement={onFocusElement}
				/>
			))}
		</ClayList>
	);

	const _renderHits = () => (
		<div className="preview-results-list sidebar-body">
			<ClayList>
				{results.hits.map((result) => (
					<ResultListItem item={result} key={result.id} />
				))}
			</ClayList>

			<ClayPaginationBar>
				<ClayPaginationBar.DropDown
					alignmentPosition={Align.TopLeft}
					items={DELTAS.map((delta) => ({
						label: delta,
						onClick: _handleDeltaChange(delta),
					}))}
					trigger={
						<ClayButton displayType="unstyled">
							{sub(Liferay.Language.get('x-entries'), [
								activeDelta,
							])}

							<ClayIcon symbol="caret-double-l" />
						</ClayButton>
					}
				/>

				<ClayPaginationBar.Results>
					{sub(Liferay.Language.get('showing-x-to-x-of-x-entries'), [
						(activePage - 1) * activeDelta + 1,
						activePage * activeDelta < results.meta.totalHits
							? activePage * activeDelta
							: results.meta.totalHits,
						results.meta.totalHits,
					])}
				</ClayPaginationBar.Results>

				<ClayPaginationWithBasicItems
					activePage={activePage}
					ellipsisBuffer={1}
					onPageChange={setActivePage}
					totalPages={Math.ceil(results.meta.totalHits / activeDelta)}
				/>
			</ClayPaginationBar>
		</div>
	);

	const _renderResultsManagementBar = () => (
		<ClayManagementToolbar>
			<ClayManagementToolbar.ItemList>
				<ClayManagementToolbar.Item>
					<span className="text-truncate-inline total-hits-label">
						<span className="text-truncate">
							{sub(Liferay.Language.get('x-results'), [
								results.meta.totalHits.toLocaleString(),
							])}
						</span>
					</span>
				</ClayManagementToolbar.Item>

				<ClayManagementToolbar.Item>
					<ClayButton
						aria-label={Liferay.Language.get('refresh')}
						disabled={loading}
						displayType="secondary"
						onClick={_handleFetch}
						small
					>
						{Liferay.Language.get('refresh')}
					</ClayButton>
				</ClayManagementToolbar.Item>
			</ClayManagementToolbar.ItemList>

			<ClayManagementToolbar.ItemList>
				<ClayManagementToolbar.Item>
					<PreviewModal
						body={
							<>
								<ClayButton.Group spaced>
									<ClayButton
										displayType="secondary"
										onClick={_onCopyToClipboard}
										small
									>
										<span className="inline-item inline-item-before">
											<ClayIcon symbol="copy" />
										</span>

										{Liferay.Language.get(
											'copy-to-clipboard'
										)}
									</ClayButton>

									<ClayLink
										displayType="secondary"
										download={RAW_RESPONSE_FILE_NAME}
										href={URL.createObjectURL(
											new Blob(
												[
													JSON.stringify(
														results,
														null,
														2
													),
												],
												{
													type: 'application/json',
												}
											)
										)}
										onClick={() => {
											openSuccessToast({
												message: Liferay.Language.get(
													'downloaded-json'
												),
											});
										}}
										outline
									>
										<span className="inline-item inline-item-before">
											<ClayIcon symbol="download" />
										</span>

										{Liferay.Language.get('download-json')}
									</ClayLink>
								</ClayButton.Group>

								<CodeMirrorEditor
									folded
									readOnly
									value={JSON.stringify(results, null, 2)}
								/>
							</>
						}
						size="lg"
						title={Liferay.Language.get('raw-response')}
					>
						<ClayButton
							borderless
							className="raw-response"
							disabled={loading}
							displayType="secondary"
							small
						>
							{Liferay.Language.get('view-raw-response')}
						</ClayButton>
					</PreviewModal>
				</ClayManagementToolbar.Item>
			</ClayManagementToolbar.ItemList>
		</ClayManagementToolbar>
	);

	return (
		<div
			className={getCN('preview-sidebar', 'sidebar', 'sidebar-light', {
				open: visible,
			})}
		>
			<div className="sidebar-header">
				<h4 className="component-title">
					<span className="text-truncate-inline">
						<span className="text-truncate">
							{Liferay.Language.get('preview')}
						</span>
					</span>
				</h4>

				<ClayButton
					aria-label={Liferay.Language.get('dropdown')}
					displayType="unstyled"
					onClick={() => onToggle(false)}
					small
				>
					<ClayIcon symbol="times" />
				</ClayButton>
			</div>

			<nav
				aria-label="preview-searchbar"
				className="component-tbar sidebar-search tbar"
			>
				<div className="container-fluid">
					<SearchInput onChange={setValue} onEnter={_handleFetch} />
				</div>
			</nav>

			{results.warnings &&
				!!results.warnings.length &&
				results.warnings.map((warning, index) => (
					<ClayAlert
						displayType="warning"
						key={index}
						title={Liferay.Language.get('warning')}
						variant="stripe"
					>
						{warning.msg}
					</ClayAlert>
				))}

			{results.meta &&
				(!results.errors || !results.errors.length) &&
				_renderResultsManagementBar()}

			{!loading ? (
				results.errors && results.errors.length ? (
					_renderErrors()
				) : results.hits && results.hits.length ? (
					_renderHits()
				) : results.meta ? (
					<div className="empty-list-message">
						<ClayEmptyState description="" />
					</div>
				) : (
					<div className="search-message">
						{Liferay.Language.get(
							'perform-a-search-to-preview-your-blueprints-search-results'
						)}
					</div>
				)
			) : (
				<ClayLoadingIndicator />
			)}
		</div>
	);
}

PreviewSidebar.propTypes = {
	loading: PropTypes.bool,
	onFetchResults: PropTypes.func,
	onFocusElement: PropTypes.func,
	onToggle: PropTypes.func,
	results: PropTypes.object,
	visible: PropTypes.bool,
};

export default React.memo(PreviewSidebar);
