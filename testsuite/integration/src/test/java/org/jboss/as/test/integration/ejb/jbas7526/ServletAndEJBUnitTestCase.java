/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.jbas7526;

import java.net.HttpURLConnection;

import org.junit.Assert;
import org.junit.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.formatter.Formatter;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.util.file.JarArchiveBrowser;
import org.junit.runner.RunWith;

/**
 * Deploy a war with an EJB in it.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class ServletAndEJBUnitTestCase
{
    private static final Logger log = Logger.getLogger(ServletAndEJBUnitTestCase.class);
    
	// JBAS-8540
   private final String baseURL = "http://" + System.getProperty("jbosstest.server.host.url","localhost") + ":" + Integer.getInteger("web.port", 8080) + "/jbas7526/calculator";;
   
   @Deployment
   public static Archive<?> deploy() {
       final WebArchive war = ShrinkWrap.create(WebArchive.class, "jbas7526.war");
       war.addClass(CalculatorServlet.class);
       war.addClass(ServletAndEJBUnitTestCase.class);
       final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbas7526.jar");
       jar.addClass(CalculatorBean.class);
       jar.addClass(CalculatorLocal.class);
       war.addAsLibraries(jar);
       log.debug(war.toString(Formatters.VERBOSE));
       return war;
   }
   
   private String accessURL(String url) throws Exception
   {
      HttpClient httpClient =  new DefaultHttpClient();
      final HttpGet request = new HttpGet(baseURL + url);
      log.info("RequestURI: " + request.getURI());
      final HttpResponse response = httpClient.execute(request);
      int statusCode = response.getStatusLine().getStatusCode();
      log.info("responseCode="+statusCode+", response="+response);
      final HttpEntity entity = response.getEntity();
      final String responseMessage = EntityUtils.toString(entity);
      // HttpMethodBase request = new GetMethod(baseURL + url);
      // log.debug("RequestURI: " + request.getURI());
      // int responseCode = httpConn.executeMethod(request);
      // String response = request.getStatusText();
      // log.debug("responseCode="+responseCode+", response="+response);
      // String content = request.getResponseBodyAsString();
      Assert.assertEquals(HttpURLConnection.HTTP_OK, statusCode);
      return responseMessage;
   }
   
   @Test
   @RunAsClient
   public void test1() throws Exception
   {
      String result = accessURL("?a=1&b=2");
      Assert.assertEquals("3\r\n", result);
   }   
}
