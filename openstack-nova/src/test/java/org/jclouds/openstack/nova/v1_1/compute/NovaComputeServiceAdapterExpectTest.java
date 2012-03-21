/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.openstack.nova.v1_1.compute;

import static org.testng.Assert.assertNotNull;

import java.net.URI;
import java.util.Map;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.ComputeServiceAdapter.NodeAndInitialCredentials;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.openstack.nova.v1_1.compute.domain.ServerInZone;
import org.jclouds.openstack.nova.v1_1.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v1_1.internal.BaseNovaComputeServiceContextExpectTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.inject.Injector;

/**
 * Tests the compute service abstraction of the nova client.
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "NovaComputeServiceAdapterExpectTest")
public class NovaComputeServiceAdapterExpectTest extends BaseNovaComputeServiceContextExpectTest<Injector> {

   public void testCreateNodeWithGroupEncodedIntoNameWhenSecurityGroupsArePresent() throws Exception {

      HttpRequest createServer = HttpRequest
         .builder()
         .method("POST")
         .endpoint(URI.create("https://compute.north.host/v1.1/3456/servers"))
         .headers(
               ImmutableMultimap.<String, String> builder().put("Accept", "application/json")
                     .put("X-Auth-Token", authToken).build())
         .payload(payloadFromStringWithContentType(
                  "{\"server\":{\"name\":\"test-e92\",\"imageRef\":\"1241\",\"flavorRef\":\"100\",\"security_groups\":[{\"name\":\"group2\"},{\"name\":\"group1\"}]}}","application/json"))
         .build();

  
      HttpResponse createServerResponse = HttpResponse.builder().statusCode(202).message("HTTP/1.1 202 Accepted")
         .payload(payloadFromResourceWithContentType("/new_server.json","application/json; charset=UTF-8")).build();

         
      Map<HttpRequest, HttpResponse> requestResponseMap = ImmutableMap.<HttpRequest, HttpResponse> builder()
               .put(keystoneAuthWithAccessKeyAndSecretKey, responseWithKeystoneAccess)
               .put(extensionsOfNovaRequest, extensionsOfNovaResponse)
               .put(listImagesDetail, listImagesDetailResponse)
               .put(listFlavorsDetail, listFlavorsDetailResponse)
               .put(createServer, createServerResponse).build();

      Injector forSecurityGroups = requestsSendResponses(requestResponseMap);

      Template template = forSecurityGroups.getInstance(TemplateBuilder.class).build();
      template.getOptions().as(NovaTemplateOptions.class).securityGroupNames("group1", "group2");
      
      NovaComputeServiceAdapter adapter = forSecurityGroups.getInstance(NovaComputeServiceAdapter.class);

      NodeAndInitialCredentials<ServerInZone> server = adapter.createNodeWithGroupEncodedIntoName("test", "test-e92",
               template);
      assertNotNull(server);
   }

   @Override
   public Injector apply(ComputeServiceContext input) {
      return input.utils().injector();
   }
}