/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.web.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.datagear.util.IOUtil;
import org.datagear.web.config.support.CustomErrorPageRegistrar;
import org.datagear.web.config.support.EnumCookieThemeResolver;
import org.datagear.web.controller.MainController;
import org.datagear.web.freemarker.CustomFreeMarkerView;
import org.datagear.web.freemarker.WriteJsonTemplateDirectiveModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.ui.context.support.ResourceBundleThemeSource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web配置。
 * 
 * @author datagear@163.com
 *
 */
@Configuration
@ComponentScan(basePackageClasses = MainController.class)
public class WebMvcConfigurerConfig implements WebMvcConfigurer
{
	private CoreConfig coreConfig;

	@Autowired
	public WebMvcConfigurerConfig(CoreConfig coreConfig)
	{
		super();
		this.coreConfig = coreConfig;
	}

	public CoreConfig getCoreConfig()
	{
		return coreConfig;
	}

	public void setCoreConfig(CoreConfig coreConfig)
	{
		this.coreConfig = coreConfig;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry)
	{
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/org/datagear/web/static/");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry)
	{
		ThemeChangeInterceptor interceptor = new ThemeChangeInterceptor();
		registry.addInterceptor(interceptor);
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters)
	{
		int oldIndex = -1;

		for (int i = 0; i < converters.size(); i++)
		{
			HttpMessageConverter<?> converter = converters.get(i);

			if (converter instanceof MappingJackson2HttpMessageConverter)
			{
				oldIndex = i;
				break;
			}
		}

		ObjectMapper objectMapper = this.coreConfig.objectMapperBuilder().build();
		MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter(objectMapper);

		if (oldIndex > -1)
			converters.add(oldIndex, messageConverter);
		else
			converters.add(messageConverter);
	}

	@Override
	public void configureViewResolvers(ViewResolverRegistry registry)
	{
		FreeMarkerViewResolver viewResolver = new FreeMarkerViewResolver();
		viewResolver.setViewClass(CustomFreeMarkerView.class);
		viewResolver.setContentType("text/html;charset=" + IOUtil.CHARSET_UTF_8);
		viewResolver.setExposeRequestAttributes(true);
		viewResolver.setAllowRequestOverride(true);
		viewResolver.setCache(true);
		viewResolver.setPrefix("");
		viewResolver.setSuffix(".ftl");

		registry.viewResolver(viewResolver);
	}

	@Bean
	public FreeMarkerConfigurer freeMarkerConfigurer()
	{
		FreeMarkerConfigurer bean = new FreeMarkerConfigurer();

		Properties settings = new Properties();
		settings.setProperty("datetime_format", "yyyy-MM-dd HH:mm:ss");
		settings.setProperty("date_format", "yyyy-MM-dd");
		settings.setProperty("number_format", "#.##");

		Map<String, Object> variables = new HashMap<>();
		variables.put("writeJson", new WriteJsonTemplateDirectiveModel(this.coreConfig.objectMapperBuilder()));

		bean.setTemplateLoaderPath("classpath:org/datagear/web/templates/");
		bean.setDefaultEncoding(IOUtil.CHARSET_UTF_8);
		bean.setFreemarkerSettings(settings);
		bean.setFreemarkerVariables(variables);

		return bean;
	}

	@Bean("themeSource")
	public ResourceBundleThemeSource themeSource()
	{
		ResourceBundleThemeSource bean = new ResourceBundleThemeSource();
		bean.setBasenamePrefix("org.datagear.web.theme.");

		return bean;
	}

	@Bean("themeResolver")
	public EnumCookieThemeResolver themeResolver()
	{
		EnumCookieThemeResolver bean = new EnumCookieThemeResolver();
		return bean;
	}

	@Bean("localeResolver")
	public AcceptHeaderLocaleResolver localeResolver()
	{
		AcceptHeaderLocaleResolver bean = new AcceptHeaderLocaleResolver();
		return bean;
	}

	@Bean("multipartResolver")
	public MultipartResolver multipartResolver()
	{
		CommonsMultipartResolver bean = new CommonsMultipartResolver();
		return bean;
	}

	@Bean
	public ErrorPageRegistrar errorPageRegistrar()
	{
		CustomErrorPageRegistrar bean = new CustomErrorPageRegistrar();
		return bean;
	}
}
