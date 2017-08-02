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

package com.liferay.journal.search.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.counter.kernel.service.CounterLocalServiceUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.test.util.JournalArticleBuilder;
import com.liferay.journal.test.util.JournalArticleContent;
import com.liferay.journal.test.util.JournalArticleTitle;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.Summary;
import com.liferay.portal.kernel.search.highlight.HighlightUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.TimeZoneUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.test.ServiceTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.test.LayoutTestUtil;

import java.util.Locale;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author André de Oliveira
 * @author Bryan Engler
 */
@RunWith(Arquillian.class)
@Sync
public class JournalArticleSummaryTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_user = UserTestUtil.addUser();

		_journalArticleBuilder = new JournalArticleBuilder();

		_journalArticleBuilder.setGroupId(_group.getGroupId());

		ServiceTestUtil.setUser(TestPropsValues.getUser());

		CompanyThreadLocal.setCompanyId(TestPropsValues.getCompanyId());

		_indexer = IndexerRegistryUtil.getIndexer(JournalArticle.class);
	}

	@Test
	public void testGetSummary() throws Exception {
		String title = "test title";
		String content = "test content";

		assertGetSummary(title, content, null, null, null, null, null, null);
	}

	@Test
	public void testGetSummaryHighlighted() throws Exception {
		String title = "test title";
		String content = "test content";

		StringBundler sb = new StringBundler(4);

		sb.append(HighlightUtil.HIGHLIGHT_TAG_OPEN);
		sb.append("test");
		sb.append(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		sb.append(" title");

		String highlightedTitle = sb.toString();

		sb = new StringBundler(4);

		sb.append(HighlightUtil.HIGHLIGHT_TAG_OPEN);
		sb.append("test");
		sb.append(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		sb.append(" content");

		String highlightedContent = sb.toString();

		assertGetSummary(
			title, content, highlightedTitle, highlightedContent, null, null,
			null, null);
	}

	@Test
	public void testGetSummaryHighlightedStaleDoc() throws Exception {
		String title = "test title";
		String content = "test content";

		StringBundler sb = new StringBundler(4);

		sb.append(HighlightUtil.HIGHLIGHT_TAG_OPEN);
		sb.append("test");
		sb.append(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		sb.append(" title");

		String highlightedTitle = sb.toString();

		sb = new StringBundler(4);

		sb.append(HighlightUtil.HIGHLIGHT_TAG_OPEN);
		sb.append("test");
		sb.append(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		sb.append(" content");

		String highlightedContent = sb.toString();

		sb = new StringBundler(4);

		sb.append(HighlightUtil.HIGHLIGHT_TAG_OPEN);
		sb.append("test");
		sb.append(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		sb.append(" stale title");

		String staleHighlightedTitle = sb.toString();

		sb = new StringBundler(4);

		sb.append(HighlightUtil.HIGHLIGHT_TAG_OPEN);
		sb.append("test");
		sb.append(HighlightUtil.HIGHLIGHT_TAG_CLOSE);
		sb.append(" stale content");

		String staleHighlightedContent = sb.toString();

		assertGetSummary(
			title, content, highlightedTitle, highlightedContent, null, null,
			staleHighlightedTitle, staleHighlightedContent);
	}

	@Test
	public void testGetSummaryStaleDoc() throws Exception {
		String title = "test title";
		String content = "test content";

		String staleTitle = "stale title";
		String staleContent = "stale content";

		assertGetSummary(
			title, content, null, null, staleTitle, staleContent, null, null);
	}

	protected JournalArticle addArticle() {
		try {
			return _journalArticleBuilder.addArticle();
		}
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void addTextFields(
		Document document, String staleTitle, String staleContent,
		String highlightedTitle, String highlightedContent,
		String staleHighlightedTitle, String staleHighlightedContent) {

		if (Validator.isNull(staleHighlightedTitle)) {
			document.addText(
				getSnippetFieldName(Field.TITLE), highlightedTitle);
		}
		else {
			document.addText(
				getSnippetFieldName(Field.TITLE), staleHighlightedTitle);
		}

		if (Validator.isNotNull(staleTitle)) {
			document.addText(
				Field.TITLE + StringPool.UNDERLINE +
					LocaleUtil.toLanguageId(Locale.US),
				staleTitle);
		}

		if (Validator.isNotNull(staleContent)) {
			document.addText(
				Field.CONTENT + StringPool.UNDERLINE +
					LocaleUtil.toLanguageId(Locale.US),
				staleContent);
		}

		if (Validator.isNull(staleHighlightedContent)) {
			document.addText(
				getSnippetFieldName(Field.CONTENT), highlightedContent);
		}
		else {
			document.addText(
				getSnippetFieldName(Field.CONTENT), staleHighlightedContent);
		}
	}

	protected void assertGetSummary(
			String title, String content, String highlightedTitle,
			String highlightedContent, String staleTitle, String staleContent,
			String staleHighlightedTitle, String staleHighlightedContent)
		throws Exception {

		setTitle(
			new JournalArticleTitle() {
				{
					put(LocaleUtil.US, title);
				}
			});

		setContent(
			new JournalArticleContent() {
				{
					name = "content";
					defaultLocale = LocaleUtil.US;

					put(LocaleUtil.US, content);
				}
			});

		PortletRequest portletRequest =
			new org.springframework.mock.web.portlet.MockRenderRequest();

		HttpServletRequest servletRequest =
			new org.springframework.mock.web.MockHttpServletRequest();

		servletRequest.setAttribute(
			JavaConstants.JAVAX_PORTLET_REQUEST, portletRequest);

		PortletResponse portletResponse =
			new org.springframework.mock.web.portlet.MockPortletResponse();

		HttpServletResponse servletResponse =
			new org.springframework.mock.web.MockHttpServletResponse();

		ThemeDisplay themeDisplay = getThemeDisplay(
			servletRequest, servletResponse);

		portletRequest.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);

		JournalArticle journalArticle = addArticle();

		Document document = _indexer.getDocument(journalArticle);

		addTextFields(
			document, staleTitle, staleContent, highlightedTitle,
			highlightedContent, staleHighlightedTitle, staleHighlightedContent);

		Summary summary = _indexer.getSummary(
			document, null, portletRequest, portletResponse);

		String summaryTitle = summary.getTitle();
		String summaryContent = summary.getContent();

		String expectedTitle = getExpectedTitle(
			title, staleTitle, highlightedTitle, staleHighlightedTitle);

		String expectedContent = content;

		if (Validator.isNotNull(highlightedContent)) {
			expectedContent = highlightedContent;
		}

		Assert.assertEquals(expectedTitle, summaryTitle);
		Assert.assertEquals(expectedContent, summaryContent);
	}

	protected String getExpectedTitle(
		String title, String staleTitle, String highlightedTitle,
		String staleHighlightedTitle) {

		if (Validator.isNotNull(staleTitle)) {
			title = staleTitle;
		}

		if (Validator.isNotNull(highlightedTitle)) {
			title = highlightedTitle;
		}

		if (Validator.isNotNull(staleHighlightedTitle)) {
			title = staleHighlightedTitle;
		}

		return title;
	}

	protected String getSnippetFieldName(String field) {
		StringBundler sb = new StringBundler(5);

		sb.append(Field.SNIPPET);
		sb.append(StringPool.UNDERLINE);
		sb.append(field);
		sb.append(StringPool.UNDERLINE);
		sb.append(LocaleUtil.toLanguageId(Locale.US));

		return sb.toString();
	}

	protected ThemeDisplay getThemeDisplay(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse)
		throws Exception {

		ThemeDisplay themeDisplay = new ThemeDisplay();

		themeDisplay.setCompany(
			CompanyLocalServiceUtil.getCompany(_group.getCompanyId()));

		themeDisplay.setUser(_user);
		themeDisplay.setRealUser(_user);

		Layout layout = LayoutTestUtil.addLayout(_group);

		themeDisplay.setLayout(layout);

		themeDisplay.setTimeZone(TimeZoneUtil.getDefault());
		themeDisplay.setRequest(servletRequest);
		themeDisplay.setResponse(servletResponse);

		LayoutSet layoutSet = LayoutSetLocalServiceUtil.createLayoutSet(
			CounterLocalServiceUtil.increment());

		themeDisplay.setLayoutSet(layoutSet);

		return themeDisplay;
	}

	protected void setContent(JournalArticleContent journalArticleContent) {
		_journalArticleBuilder.setContent(journalArticleContent);
	}

	protected void setTitle(JournalArticleTitle journalArticleTitle) {
		_journalArticleBuilder.setTitle(journalArticleTitle);
	}

	@DeleteAfterTestRun
	private Group _group;

	private Indexer<JournalArticle> _indexer;
	private JournalArticleBuilder _journalArticleBuilder;

	@DeleteAfterTestRun
	private User _user;

}