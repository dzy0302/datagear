/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.analysis.ChartDataSet;
import org.datagear.analysis.ChartPlugin;
import org.datagear.analysis.ChartPluginManager;
import org.datagear.analysis.DataSetResult;
import org.datagear.analysis.TemplateDashboardWidgetResManager;
import org.datagear.analysis.support.html.HtmlChartPlugin;
import org.datagear.analysis.support.html.HtmlDashboard;
import org.datagear.analysis.support.html.HtmlRenderAttributes;
import org.datagear.analysis.support.html.HtmlRenderContext;
import org.datagear.analysis.support.html.HtmlRenderContext.WebContext;
import org.datagear.analysis.support.html.HtmlTplDashboardWidget;
import org.datagear.analysis.support.html.HtmlTplDashboardWidgetHtmlRenderer;
import org.datagear.analysis.support.html.HtmlTplDashboardWidgetRenderer.AddPrefixHtmlTitleHandler;
import org.datagear.management.domain.HtmlChartWidgetEntity;
import org.datagear.management.domain.SqlDataSetEntity;
import org.datagear.management.domain.User;
import org.datagear.management.service.HtmlChartWidgetEntityService;
import org.datagear.persistence.PagingData;
import org.datagear.persistence.PagingQuery;
import org.datagear.util.IDUtil;
import org.datagear.util.IOUtil;
import org.datagear.web.OperationMessage;
import org.datagear.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

/**
 * 图表控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/analysis/chart")
public class ChartController extends AbstractChartPluginAwareController
{
	@Autowired
	private HtmlChartWidgetEntityService htmlChartWidgetEntityService;

	@Autowired
	private ChartPluginManager chartPluginManager;

	@Autowired
	@Qualifier("chartShowHtmlTplDashboardWidgetHtmlRenderer")
	private HtmlTplDashboardWidgetHtmlRenderer<HtmlRenderContext> chartShowHtmlTplDashboardWidgetHtmlRenderer;

	public ChartController()
	{
		super();
	}

	public ChartController(HtmlChartWidgetEntityService htmlChartWidgetEntityService,
			ChartPluginManager chartPluginManager,
			HtmlTplDashboardWidgetHtmlRenderer<HtmlRenderContext> chartShowHtmlTplDashboardWidgetHtmlRenderer)
	{
		super();
		this.htmlChartWidgetEntityService = htmlChartWidgetEntityService;
		this.chartPluginManager = chartPluginManager;
		this.chartShowHtmlTplDashboardWidgetHtmlRenderer = chartShowHtmlTplDashboardWidgetHtmlRenderer;
	}

	public HtmlChartWidgetEntityService getHtmlChartWidgetEntityService()
	{
		return htmlChartWidgetEntityService;
	}

	public void setHtmlChartWidgetEntityService(HtmlChartWidgetEntityService htmlChartWidgetEntityService)
	{
		this.htmlChartWidgetEntityService = htmlChartWidgetEntityService;
	}

	public ChartPluginManager getChartPluginManager()
	{
		return chartPluginManager;
	}

	public void setChartPluginManager(ChartPluginManager chartPluginManager)
	{
		this.chartPluginManager = chartPluginManager;
	}

	public HtmlTplDashboardWidgetHtmlRenderer<HtmlRenderContext> getChartShowHtmlTplDashboardWidgetHtmlRenderer()
	{
		return chartShowHtmlTplDashboardWidgetHtmlRenderer;
	}

	public void setChartShowHtmlTplDashboardWidgetHtmlRenderer(
			HtmlTplDashboardWidgetHtmlRenderer<HtmlRenderContext> chartShowHtmlTplDashboardWidgetHtmlRenderer)
	{
		this.chartShowHtmlTplDashboardWidgetHtmlRenderer = chartShowHtmlTplDashboardWidgetHtmlRenderer;
	}

	@RequestMapping("/add")
	public String add(HttpServletRequest request, org.springframework.ui.Model model)
	{
		HtmlChartWidgetEntity chart = new HtmlChartWidgetEntity();

		List<HtmlChartPluginVO> pluginVOs = findHtmlChartPluginVOs(request, null);

		if (pluginVOs.size() > 0)
		{
			String defaultChartPluginId = pluginVOs.get(0).getId();
			chart.setHtmlChartPlugin((HtmlChartPlugin<HtmlRenderContext>) this.chartPluginManager
					.<HtmlRenderContext> get(defaultChartPluginId));
		}

		model.addAttribute("chart", chart);
		model.addAttribute("pluginVOs", toWriteJsonTemplateModel(pluginVOs));
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.addChart");
		model.addAttribute(KEY_FORM_ACTION, "save");

		return "/analysis/chart/chart_form";
	}

	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id)
	{
		User user = WebUtils.getUser(request, response);

		HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getByIdForEdit(user, id);

		if (chart == null)
			throw new RecordNotFoundException();

		List<HtmlChartPluginVO> pluginVOs = findHtmlChartPluginVOs(request, null);

		model.addAttribute("chart", chart);
		model.addAttribute("chartDataSets", toWriteJsonTemplateModel(chart.getChartDataSets()));
		model.addAttribute("pluginVOs", toWriteJsonTemplateModel(pluginVOs));
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.editChart");
		model.addAttribute(KEY_FORM_ACTION, "save");

		return "/analysis/chart/chart_form";
	}

	@RequestMapping(value = "/save", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> save(HttpServletRequest request, HttpServletResponse response,
			HtmlChartWidgetEntity entity)
	{
		User user = WebUtils.getUser(request, response);

		if (isEmpty(entity.getId()))
		{
			entity.setId(IDUtil.uuid());
			entity.setCreateUser(user);
			inflateHtmlChartWidgetEntity(entity, request);

			checkSaveEntity(entity);

			this.htmlChartWidgetEntityService.add(user, entity);
		}
		else
		{
			inflateHtmlChartWidgetEntity(entity, request);
			checkSaveEntity(entity);
			this.htmlChartWidgetEntityService.update(user, entity);
		}

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id", entity.getId());

		ResponseEntity<OperationMessage> responseEntity = buildOperationMessageSaveSuccessResponseEntity(request);
		responseEntity.getBody().setData(data);

		return responseEntity;
	}

	@RequestMapping("/view")
	public String view(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@RequestParam("id") String id)
	{
		User user = WebUtils.getUser(request, response);

		HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getById(user, id);

		if (chart == null)
			throw new RecordNotFoundException();

		List<HtmlChartPluginVO> pluginVOs = findHtmlChartPluginVOs(request, null);

		model.addAttribute("chart", chart);
		model.addAttribute("chartDataSets", toWriteJsonTemplateModel(chart.getChartDataSets()));
		model.addAttribute("pluginVOs", toWriteJsonTemplateModel(pluginVOs));
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.viewChart");
		model.addAttribute(KEY_READONLY, true);

		return "/analysis/chart/chart_form";
	}

	@RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("id") String[] ids)
	{
		User user = WebUtils.getUser(request, response);

		for (int i = 0; i < ids.length; i++)
		{
			String id = ids[i];
			this.htmlChartWidgetEntityService.deleteById(user, id);
		}

		return buildOperationMessageDeleteSuccessResponseEntity(request);
	}

	@RequestMapping("/pagingQuery")
	public String pagingQuery(HttpServletRequest request, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.manageChart");

		return "/analysis/chart/chart_grid";
	}

	@RequestMapping(value = "/select")
	public String select(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model)
	{
		model.addAttribute(KEY_TITLE_MESSAGE_KEY, "chart.selectChart");
		model.addAttribute(KEY_SELECTONLY, true);
		setIsMultipleSelectAttribute(request, model);

		return "/analysis/chart/chart_grid";
	}

	@RequestMapping(value = "/pagingQueryData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public PagingData<HtmlChartWidgetEntity> pagingQueryData(HttpServletRequest request, HttpServletResponse response,
			final org.springframework.ui.Model springModel) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		PagingQuery pagingQuery = getPagingQuery(request);

		PagingData<HtmlChartWidgetEntity> pagingData = this.htmlChartWidgetEntityService.pagingQuery(user, pagingQuery);
		setChartPluginNames(request, pagingData.getItems());

		return pagingData;
	}

	/**
	 * 展示图表。
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping({ "/show/{id}/", "/show/{id}/index" })
	public void show(HttpServletRequest request, HttpServletResponse response, org.springframework.ui.Model model,
			@PathVariable("id") String id) throws Exception
	{
		User user = WebUtils.getUser(request, response);

		HtmlChartWidgetEntity chart = this.htmlChartWidgetEntityService.getById(user, id);

		if (chart == null)
			throw new RecordNotFoundException();

		String htmlTitle = chart.getName();
		HtmlTplDashboardWidget<HtmlRenderContext> dashboardWidget = new HtmlTplDashboardWidget<HtmlRenderContext>(id,
				this.chartShowHtmlTplDashboardWidgetHtmlRenderer.simpleTemplateContent("UTF-8", htmlTitle,
						"  position:absolute;\n  left:1em;\n  right:1em;\n  top:1em;\n  bottom:1em;\n  margin:0 0;\n",
						new String[] { id }),
				this.chartShowHtmlTplDashboardWidgetHtmlRenderer);

		String responseEncoding = dashboardWidget.getTemplateEncoding();
		response.setCharacterEncoding(responseEncoding);

		Writer out = response.getWriter();

		HtmlRenderContext renderContext = createHtmlRenderContext(request, createWebContext(request), out);
		AddPrefixHtmlTitleHandler htmlTitleHandler = new AddPrefixHtmlTitleHandler(
				getMessage(request, "chart.show.htmlTitlePrefix", getMessage(request, "app.name")));
		HtmlRenderAttributes.setHtmlTitleHandler(renderContext, htmlTitleHandler);

		HtmlDashboard dashboard = dashboardWidget.render(renderContext);

		SessionHtmlDashboardManager dashboardManager = getSessionHtmlDashboardManagerNotNull(request);
		dashboardManager.put(dashboard);
	}

	/**
	 * 加载展示图表的资源。
	 * 
	 * @param request
	 * @param response
	 * @param webRequest
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping("/show/{id}/**/*")
	public void showResource(HttpServletRequest request, HttpServletResponse response, WebRequest webRequest,
			org.springframework.ui.Model model, @PathVariable("id") String id) throws Exception
	{
		String pathInfo = request.getPathInfo();
		String resPath = pathInfo.substring(pathInfo.indexOf(id) + id.length() + 1);

		TemplateDashboardWidgetResManager resManager = this.chartShowHtmlTplDashboardWidgetHtmlRenderer
				.getTemplateDashboardWidgetResManager();

		long lastModified = resManager.lastModifiedResource(id, resPath);
		if (webRequest.checkNotModified(lastModified))
			return;

		InputStream in = resManager.getResourceInputStream(id, resPath);
		OutputStream out = response.getOutputStream();

		IOUtil.write(in, out);
	}

	/**
	 * 展示数据。
	 * 
	 * @param request
	 * @param response
	 * @param model
	 * @param id
	 * @throws Exception
	 */
	@RequestMapping(value = "/showData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, DataSetResult[]> showData(HttpServletRequest request, HttpServletResponse response,
			org.springframework.ui.Model model) throws Exception
	{
		WebContext webContext = createWebContext(request);
		return getDashboardData(request, response, model, webContext);
	}

	protected WebContext createWebContext(HttpServletRequest request)
	{
		String contextPath = getWebContextPath(request).get(request);
		return new WebContext(contextPath, contextPath + "/analysis/chart/showData");
	}

	protected void setChartPluginNames(HttpServletRequest request, List<HtmlChartWidgetEntity> entities)
	{
		if (entities == null)
			return;

		Locale locale = WebUtils.getLocale(request);

		for (HtmlChartWidgetEntity entity : entities)
			entity.updateChartPluginName(locale);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void inflateHtmlChartWidgetEntity(HtmlChartWidgetEntity entity, HttpServletRequest request)
	{
		HtmlChartPlugin<HtmlRenderContext> htmlChartPlugin = entity.getHtmlChartPlugin();

		if (htmlChartPlugin != null)
		{
			htmlChartPlugin = (HtmlChartPlugin<HtmlRenderContext>) (ChartPlugin) this.chartPluginManager
					.get(htmlChartPlugin.getId());

			entity.setHtmlChartPlugin(htmlChartPlugin);
		}

		inflateChartDataSets(entity, request);
	}

	protected void inflateChartDataSets(HtmlChartWidgetEntity entity, HttpServletRequest request)
	{
		String[] chartDataSetIndexes = request.getParameterValues("chartDataSetIndex");

		if (isEmpty(chartDataSetIndexes))
			return;

		List<ChartDataSet> chartDataSets = new ArrayList<ChartDataSet>();

		for (String chartDataSetIndex : chartDataSetIndexes)
		{
			String dataSetId = request.getParameter("chartDataSet_" + chartDataSetIndex + "_dataSetId");

			if (isEmpty(dataSetId))
				continue;

			SqlDataSetEntity sqlDataSet = new SqlDataSetEntity();
			sqlDataSet.setId(dataSetId);

			Map<String, Set<String>> propertySigns = new HashMap<String, Set<String>>();

			String[] propertySignIndexes = request
					.getParameterValues("chartDataSet_" + chartDataSetIndex + "_propertySignIndex");

			if (!isEmpty(propertySignIndexes))
			{
				for (String propertySignIndex : propertySignIndexes)
				{
					String propertyName = request.getParameter(
							"chartDataSet_" + chartDataSetIndex + "_propertySign_" + propertySignIndex + "_name");

					if (isEmpty(propertyName))
						continue;

					String[] signs = request.getParameterValues(
							"chartDataSet_" + chartDataSetIndex + "_propertySign_" + propertySignIndex + "_value");

					if (!isEmpty(signs))
					{
						Set<String> signSet = new HashSet<String>();
						for (String sign : signs)
							signSet.add(sign);

						propertySigns.put(propertyName, signSet);
					}
				}
			}

			ChartDataSet chartDataSet = new ChartDataSet();
			chartDataSet.setDataSet(sqlDataSet);
			chartDataSet.setPropertySigns(propertySigns);

			chartDataSets.add(chartDataSet);
		}

		entity.setChartDataSets(chartDataSets.toArray(new ChartDataSet[chartDataSets.size()]));
	}

	protected void checkSaveEntity(HtmlChartWidgetEntity chart)
	{
		if (isBlank(chart.getName()))
			throw new IllegalInputException();

		if (isEmpty(chart.getChartPlugin()))
			throw new IllegalInputException();

		if (isEmpty(chart.getChartPlugin()))
			throw new IllegalInputException();
	}
}
