/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.seo.studio.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.seo.studio.model.CrawlHit;

import java.net.URI;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Brooke Dalton
 */
@Component
public class SEOStudioService extends BaseService {

	public static URI toCrawlURI(String hostname) {
		if ((hostname == null) || hostname.isBlank()) {
			throw new IllegalArgumentException("Hostname is required");
		}

		String hostnameString = StringUtil.toLowerCase(hostname.trim());

		if (!hostnameString.startsWith("http://") &&
			!hostnameString.startsWith("https://")) {

			hostnameString = "https://" + hostnameString;
		}

		URI uri = URI.create(hostnameString);

		if (uri.getHost() == null) {
			throw new IllegalArgumentException(
				"Hostname \"" + hostname + "\" has no host component");
		}

		return uri;
	}

	public static String toDomainURL(URI uri) {
		String host = StringUtil.toLowerCase(uri.getHost());
		String scheme = StringUtil.toLowerCase(uri.getScheme());

		if (uri.getPort() == -1) {
			return scheme + "://" + host;
		}

		return StringBundler.concat(scheme, "://", host, ":", uri.getPort());
	}

	public static String toIndexName(long seoStudioDomainId) {
		return "seo_studio_" + seoStudioDomainId;
	}

	public String createInsightType(JSONObject jsonObject) {
		return post(
			_authorization(), jsonObject.toString(),
			URI.create("/o/seo-studio/insight-types"));
	}

	public String createPagesBatch(JSONArray jsonArray) {
		return post(
			_authorization(), jsonArray.toString(),
			URI.create(_PAGES + "/batch"));
	}

	public String createScanInsightsBatch(JSONArray jsonArray) {
		return post(
			_authorization(), jsonArray.toString(),
			URI.create("/o/seo-studio/scan-insights/batch"));
	}

	public String fetchActiveScans() {
		return get(
			_authorization(),
			URI.create(
				StringBundler.concat(
					"/o/seo-studio/scans?filter=",
					URLEncoder.encode(
						"state in ('queued','running')",
						StandardCharsets.UTF_8),
					"&pageSize=100")));
	}

	public List<CrawlHit> fetchCrawlHits(long seoStudioDomainId) {
		List<CrawlHit> crawlHits = new ArrayList<>();

		String lastURL = null;

		while (true) {
			JSONObject hitsJSONObject = new JSONObject(
				_fetchCrawlHits(lastURL, 2000, seoStudioDomainId));

			JSONArray hitsJSONArray = hitsJSONObject.optJSONArray("items");

			if ((hitsJSONArray == null) || (hitsJSONArray.length() == 0)) {
				break;
			}

			String previousLastURL = lastURL;

			for (Object hitObject : hitsJSONArray) {
				CrawlHit crawlHit = new CrawlHit((JSONObject)hitObject);

				crawlHits.add(crawlHit);

				lastURL = crawlHit.getURL();
			}

			if (Objects.equals(previousLastURL, lastURL)) {
				break;
			}
		}

		return crawlHits;
	}

	public String fetchDomain(long seoStudioDomainId) {
		return get(
			_authorization(), URI.create(_DOMAINS + "/" + seoStudioDomainId));
	}

	public String fetchPage(int page, int pageSize, long seoStudioScanId) {
		String filterString = StringBundler.concat(
			"r_seoStudioScanToSEOStudioPages_seoStudioScanId eq '",
			seoStudioScanId, "'");

		return get(
			_authorization(),
			URI.create(
				StringBundler.concat(
					_PAGES, "?filter=",
					URLEncoder.encode(filterString, StandardCharsets.UTF_8),
					"&page=", page, "&pageSize=", pageSize)));
	}

	public String updateDomain(JSONObject jsonObject, long seoStudioDomainId) {
		return patch(
			_authorization(), jsonObject.toString(),
			URI.create(_DOMAINS + "/" + seoStudioDomainId));
	}

	public String updateScan(JSONObject jsonObject, long seoStudioScanId) {
		return patch(
			_authorization(), jsonObject.toString(),
			URI.create("/o/seo-studio/scans/" + seoStudioScanId));
	}

	public String updateScan(
		String errorMessage, long seoStudioScanId, String state) {

		JSONObject jsonObject = new JSONObject();

		if (errorMessage != null) {
			jsonObject.put("errorMessage", errorMessage);
		}

		jsonObject.put("state", state);

		return updateScan(jsonObject, seoStudioScanId);
	}

	private String _authorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-seostudio-etc-crawler-oahs");
	}

	private String _fetchCrawlHits(
		String lastURL, int pageSize, long seoStudioDomainId) {

		String crawlHitsURL = StringBundler.concat(
			"/o/seo-studio/v1.0/seo-studio-domains/", seoStudioDomainId,
			"/crawl-hits?pageSize=", pageSize);

		if ((lastURL != null) && !lastURL.isBlank()) {
			crawlHitsURL = StringBundler.concat(
				crawlHitsURL, "&lastURL=",
				URLEncoder.encode(lastURL, StandardCharsets.UTF_8));
		}

		return get(_authorization(), URI.create(crawlHitsURL));
	}

	private static final String _DOMAINS = "/o/seo-studio/domains";

	private static final String _PAGES = "/o/seo-studio/pages";

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

}