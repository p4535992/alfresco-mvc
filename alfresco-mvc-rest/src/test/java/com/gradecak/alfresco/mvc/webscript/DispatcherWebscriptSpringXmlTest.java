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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.gradecak.alfresco.mvc.webscript.mock.MockWebscriptBuilder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "/web-context-test.xml" })
@TestInstance(Lifecycle.PER_CLASS)
public class DispatcherWebscriptSpringXmlTest extends AbstractAlfrescoMvcTest {

	@Spy
	@Autowired
	private DispatcherWebscript webScript;

	@BeforeAll
	public void beforeAll() throws Exception {
		MockitoAnnotations.initMocks(this);

		webScript.setServletContext(new MockServletContext());
		webScript.setContextConfigLocation("test-webscriptdispatcher-context.xml");

		mockWebscript = MockWebscriptBuilder.singleWebscript(webScript);
	}

	@BeforeEach
	public void before() throws Exception {
		mockWebscript.newRequest();
	}

}
