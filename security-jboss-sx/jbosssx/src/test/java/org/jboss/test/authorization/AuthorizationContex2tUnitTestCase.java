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
package org.jboss.test.authorization;


import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jboss.security.auth.login.XMLLoginConfigImpl;
import org.jboss.security.authorization.AuthorizationContext;
import org.jboss.security.authorization.AuthorizationException;
import org.jboss.security.authorization.Resource;
import org.jboss.security.authorization.ResourceType;
import org.jboss.security.config.ApplicationPolicyRegistration;
import org.jboss.security.config.parser.StaxBasedConfigParser;
import org.jboss.security.plugins.authorization.JBossAuthorizationContext;


public class AuthorizationContex2tUnitTestCase extends TestCase
{ 
   public AuthorizationContex2tUnitTestCase(String name)
   {
      super(name); 
   }


   public static Test suite() throws Exception
   {
      TestSuite suite = new TestSuite();
      suite.addTest(new TestSuite(AuthorizationContex2tUnitTestCase.class));
      // Create an initializer for the test suite
      return new TestSetup(suite)
      {
         protected void setUp() throws Exception
         {
            super.setUp();
            ClassLoader tcl = Thread.currentThread().getContextClassLoader();
            URL url = tcl.getResource("authorization/config/authorization-policy.xml");
            if(url == null)
               throw new IllegalStateException("config url is null");
            Configuration.setConfiguration(XMLLoginConfigImpl.getInstance());
            loadXMLConfig(url);
         }
         protected void tearDown() throws Exception
         {
            super.tearDown();
         }
      };
   } 


   /**
    * Test the AuthorizationModule required behavior
    */
   public void testRequiredOptionBehavior() throws Exception
   {   
      int result = getResult("required-permit-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("required-deny-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result);
   }

   /**
    * Test the AuthorizationModule requisite behavior
    */
   public void testRequisiteOptionBehavior() throws Exception
   {   
      int result = getResult("requisite-permit-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("requisite-deny-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result);
   }


   /**
    * Test the AuthorizationModule sufficient behavior
    */
   public void testSufficientOptionBehavior() throws Exception
   {   
      int result = getResult("sufficient-permit-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("sufficient-deny-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result);
   }


   /**
    * Test the AuthorizationModule optional behavior
    */
   public void testOptionalOptionBehavior() throws Exception
   {   
      int result = getResult("optional-permit-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("optional-deny-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result);
   }

   /**
    * Test the AuthorizationModules combination behavior
    */
   public void testCombinationBehavior() throws Exception
   {   
      int result = getResult("required-deny-sufficient-permit-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result); 
      result = getResult("required-permit-sufficient-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result); 
      result = getResult("required-permit-required-deny-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result);
      result = getResult("required-permit-required-permit-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("required-permit-required-permit-sufficient-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("required-permit-required-permit-requisite-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("required-permit-required-permit-optional-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("required-permit-required-deny-requisite-permit-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result); 
      result = getResult("requisite-permit-requisite-permit-sufficient-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);

      result = getResult("sufficient-permit-required-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("sufficient-permit-sufficient-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);
      result = getResult("optional-deny-sufficient-permit-required-deny-policy");
      assertTrue("PERMIT?", AuthorizationContext.PERMIT == result);

      result = getResult("sufficient-deny-optional-deny-policy");
      assertTrue("DENY?", AuthorizationContext.DENY == result);
   }

   private int getResult(String policyName) throws Exception
   {
      int result;

      Configuration config = Configuration.getConfiguration();
      if(!(config instanceof ApplicationPolicyRegistration))
         throw new IllegalStateException("JAAS Configuration does not support application policy registration");
      ApplicationPolicyRegistration appPolicyRegistration = (ApplicationPolicyRegistration) config;

      JBossAuthorizationContext aContext = new JBossAuthorizationContext(policyName, 
            new Subject(), 
            new TestCallbackHandler()); 
      aContext.setApplicationPolicy(appPolicyRegistration.getApplicationPolicy(policyName)); 
      try
      {
         result =  aContext.authorize(new Resource()
         { 
            HashMap<String,Object> contextMap = new HashMap<String,Object>();
            
            public ResourceType getLayer()
            {
               return ResourceType.WEB;
            }

            public Map<String,Object> getMap()
            {
               return contextMap;
            }

            public void add(String key, Object value)
            {
               contextMap.put(key, value);
            }
         });
      }
      catch(AuthorizationException e)
      {
         result = AuthorizationContext.DENY;
      }
      return result; 
   }

   /**
    * Use JBossXB to parse the security config file
    * @param loginConfigURL
    * @throws Exception
    */
   private static void loadXMLConfig(URL loginConfigURL)
   throws Exception 
   {
      if(loginConfigURL == null)
         throw new IllegalArgumentException("loginConfigURL is null");
      new StaxBasedConfigParser().parse2(loginConfigURL.openStream());
   } 

   /**
    * Dummy CallbackHandler
    */
   private static class TestCallbackHandler implements CallbackHandler
   { 
      public void handle(Callback[] arg0) 
      throws IOException, UnsupportedCallbackException
      {
      } 
   } 
}