/**
 * Copyright gradecak.com

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gradecak.alfresco.mvc.webscript;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WrappingWebScriptResponse;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class DispatcherWebscript extends AbstractWebScript
		implements ApplicationListener<ContextRefreshedEvent>, ServletContextAware, ApplicationContextAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherWebscript.class);

	protected DispatcherServlet s;
	private String contextConfigLocation;
	private Class<?> contextClass;
	private ApplicationContext applicationContext;
	private ServletContext servletContext;

	private final String servletName;

	public DispatcherWebscript() {
		this.servletName = "Alfresco @MVC Dispatcher Webscript";
	}

	public DispatcherWebscript(final String servletName) {
		Assert.hasText(servletName,
				"[Assertion failed] - this String servletName must have text; it must not be null, empty, or blank");
		this.servletName = "Alfresco @MVC Dispatcher Webscript: " + servletName;
	}

	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {

		final WebScriptServletRequest origReq = (WebScriptServletRequest) req;

		WebScriptServletResponse wsr = null;
		if (res instanceof WrappingWebScriptResponse) {
			wsr = (WebScriptServletResponse) ((WrappingWebScriptResponse) res).getNext();
		} else {
			wsr = (WebScriptServletResponse) res;
		}

		final HttpServletResponse sr = wsr.getHttpServletResponse();
		res.setHeader("Cache-Control", "no-cache");

		WebscriptRequestWrapper wrapper = new WebscriptRequestWrapper(origReq);
		try {
			s.service(wrapper, sr);

		} catch (Throwable e) {
			throw new IOException(e);
		}
	}

	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext refreshContext = event.getApplicationContext();
		if (refreshContext != null && refreshContext.equals(applicationContext)) {

			s = new DispatcherServlet() {

				private static final long serialVersionUID = -7492692694742840997L;

				@Override
				protected WebApplicationContext initWebApplicationContext() {
					WebApplicationContext wac = createWebApplicationContext(applicationContext);
					if (wac == null) {
						wac = super.initWebApplicationContext();
					}
					return wac;
				}

			};

			if (contextClass != null) {
				s.setContextClass(contextClass);
			}
			s.setContextConfigLocation(contextConfigLocation);
			configureDispatcherServlet(s);

			try {
				s.init(new DelegatingServletConfig(servletName));
			} catch (ServletException e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected void configureDispatcherServlet(DispatcherServlet dispatcherServlet) {
	}

	public String getContextConfigLocation() {
		return contextConfigLocation;
	}

	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * Internal implementation of the {@link ServletConfig} interface, to be passed
	 * to the servlet adapter.
	 */
	public class DelegatingServletConfig implements ServletConfig {

		final private String name;

		public DelegatingServletConfig(final String name) {
			Assert.hasText(name,
					"[Assertion failed] - this String name must have text; it must not be null, empty, or blank");
			this.name = name;
		}

		public String getServletName() {
			return name;
		}

		public ServletContext getServletContext() {
			return DispatcherWebscript.this.servletContext;
		}

		public String getInitParameter(String paramName) {
			return null;
		}

		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(new HashSet<String>());
		}
	}

	public class WebscriptRequestWrapper extends HttpServletRequestWrapper {

		private WebScriptServletRequest origReq;

		public WebscriptRequestWrapper(WebScriptServletRequest request) {
			super(request.getHttpServletRequest());
			this.origReq = request;
		}

		@Override
		public String getRequestURI() {
			String uri = super.getRequestURI();
			Pattern pattern = Pattern
					.compile("(^" + origReq.getServiceContextPath() + "/)(.*)(/" + origReq.getExtensionPath() + ")");
			Matcher matcher = pattern.matcher(uri);

			final int extensionPathRegexpGroupIndex = 3;
			if (matcher.find()) {
				try {
					return matcher.group(extensionPathRegexpGroupIndex);
				} catch (Exception e) {
					// let an empty string be returned
					LOGGER.warn("no such group (3) in regexp while URI evaluation", e);
				}
			}

			return "";
		}

		public String getContextPath() {
			return origReq.getContextPath();
		}

		public String getServletPath() {
			return "";
		}

		public WebScriptServletRequest getWebScriptServletRequest() {
			return origReq;
		}
	}

}
