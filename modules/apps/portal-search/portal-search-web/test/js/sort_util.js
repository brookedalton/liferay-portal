/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

describe('liferay-search-sort-util', () => {
	beforeEach(done => {
		require('../../src/main/resources/META-INF/resources/js/sort_util');

		AUI().use(['liferay-search-sort-util'], () => done());
	});

	describe('removeURLParameters()', () => {
		it('removes the parameter whose name is the given key', () => {
			const parameters = ['sort=title', 'q=test'];

			const newParameters = Liferay.Search.SortUtil.removeURLParameters(
				'sort',
				parameters
			);

			expect(newParameters).toEqual(['q=test']);
		});

		it('does not remove parameters not matching the given key', () => {
			const parameters = ['sort=modified%2B', 'q=test'];

			const newParameters = Liferay.Search.SortUtil.removeURLParameters(
				'sort=modified-',
				parameters
			);

			expect(newParameters).toEqual(['sort=modified%2B', 'q=test']);
		});
	});
});
