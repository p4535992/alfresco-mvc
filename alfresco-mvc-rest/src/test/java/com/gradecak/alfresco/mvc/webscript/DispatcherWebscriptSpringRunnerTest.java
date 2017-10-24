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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;
import com.gradecak.alfresco.mvc.webscript.mock.MockWebscript;
import com.gradecak.alfresco.mvc.webscript.mock.MockWebscriptBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/web-context-test.xml" })
public class DispatcherWebscriptSpringRunnerTest {

  @Spy
  @Autowired
  private DispatcherWebscript webScript;

  private MockWebscript mockWebscript;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);

    mockWebscript = MockWebscriptBuilder.singleWebscript(webScript);
  }

  @Test
  public void requestGet_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withParameters(ImmutableMap.of("id", "testId")).withControllerMapping("test/get").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"data\":\"testId\",\"total\":1,\"success\":true}", res.getContentAsString());
  }
  
  @Test
  public void requestGet_withHeaders_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withHeaders(ImmutableMap.of("header-key", (Object)"header-value")).withControllerMapping("test/getHeaders").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"data\":{\"Content-Type\":\"application/json\",\"header-key\":\"header-value\"},\"total\":1,\"success\":true}", res.getContentAsString());
  }
  
  @Test
  public void requestGet_withCookies_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withCookies(new Cookie("cookie-key","cookie-value")).withControllerMapping("test/getCookies").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"data\":[{\"name\":\"cookie-key\",\"value\":\"cookie-value\",\"comment\":null,\"domain\":null,\"maxAge\":-1,\"path\":null,\"secure\":false,\"version\":0,\"httpOnly\":false}],\"total\":1,\"success\":true}", res.getContentAsString());
  }

  @Test
  public void requestPost_withParams_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withParameters(ImmutableMap.of("id", "testId")).withControllerMapping("test/post").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"data\":\"testId\",\"total\":1,\"success\":true}", res.getContentAsString());
  }

  @Test
  public void requestPost_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withControllerMapping("test/post2").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"success\":true}", res.getContentAsString());
  }

  @Test
  public void requestPost_withBody_response400() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withControllerMapping("test/body").execute();
    Assert.assertTrue(res.getStatus() == 400);
  }

  @Test
  public void requestPost_withBody_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withBody(ImmutableMap.of("id", "testId")).withControllerMapping("test/body").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"data\":{\"id\":\"testId\"},\"total\":1,\"success\":true}", res.getContentAsString());
  }

  @Test
  public void requestGet_wrongControllerMapping_response404() throws Exception {
    MockHttpServletResponse res = mockWebscript.withParameters(ImmutableMap.of("id", "testId")).withControllerMapping("test/wrong").execute();
    Assert.assertTrue(res.getStatus() == 404);
  }

  @Test
  public void requestPost_toGetMethod() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withParameters(ImmutableMap.of("id", "testId")).withControllerMapping("test/get").execute();
    Assert.assertTrue(res.getStatus() == 405);
    Assert.assertEquals("Request method 'POST' not supported", res.getErrorMessage());
  }

  @Test
  public void requestDelete_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withMethod(HttpMethod.DELETE).withControllerMapping("test/delete").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"success\":true}", res.getContentAsString());
  }

  @Test
  public void requestPut_responseOk() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withMethod(HttpMethod.PUT).withControllerMapping("test/delete").execute();
    Assert.assertTrue(res.getStatus() == 200);
    Assert.assertEquals("{\"success\":true}", res.getContentAsString());
  }

  @Test
  public void requestHead_response405() throws Exception {
    MockHttpServletResponse res = mockWebscript.withPostRequest().withMethod(HttpMethod.HEAD).withControllerMapping("test/delete").execute();
    Assert.assertTrue(res.getStatus() == 405);
  }

  @Test
  public void requestGet_responseException() throws Exception {
    MockHttpServletResponse res = mockWebscript.withParameters(ImmutableMap.of("id", "testId")).withControllerMapping("test/exception").execute();
    Assert.assertTrue(res.getStatus() == HttpServletResponse.SC_BAD_REQUEST);
    Assert.assertEquals("{\"success\":false,\"event\":\"exception\",\"exception\":\"org.springframework.web.util.NestedServletException\","
        + "\"message\":\"Request processing failed; nested exception is java.lang.RuntimeException: test exception\","
        + "\"cause\":\"java.lang.RuntimeException\",\"causeMessage\":\"test exception\"}", res.getContentAsString());
  }

  // TODO add file upload test
}