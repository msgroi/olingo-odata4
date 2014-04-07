/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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
package org.apache.olingo.client.core.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.olingo.client.api.v3.ODataClient;
import org.apache.olingo.client.api.edm.xml.EntityContainer;
import org.apache.olingo.client.api.edm.xml.EntityType;
import org.apache.olingo.client.api.edm.xml.Schema;
import org.apache.olingo.client.api.edm.xml.XMLMetadata;
import org.apache.olingo.client.api.edm.xml.v3.FunctionImport;
import org.apache.olingo.client.api.http.HttpMethod;
import org.apache.olingo.client.core.AbstractTest;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmActionImport;
import org.apache.olingo.commons.api.edm.EdmActionImportInfo;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmFunctionImport;
import org.apache.olingo.commons.api.edm.EdmFunctionImportInfo;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.junit.Test;

public class MetadataTest extends AbstractTest {

  @Override
  protected ODataClient getClient() {
    return v3Client;
  }

  @Test
  public void parse() {
    final XMLMetadata metadata = getClient().getDeserializer().
            toMetadata(getClass().getResourceAsStream("metadata.xml"));
    assertNotNull(metadata);

    final EntityType order = metadata.getSchemas().get(0).getEntityType("Order");
    assertNotNull(order);
    assertEquals("Order", order.getName());

    @SuppressWarnings("unchecked")
    final List<FunctionImport> functionImports = (List<FunctionImport>) metadata.getSchemas().get(0).
            getDefaultEntityContainer().getFunctionImports();
    int legacyGetters = 0;
    int legacyPosters = 0;
    int actions = 0;
    int functions = 0;
    for (FunctionImport functionImport : functionImports) {
      if (HttpMethod.GET.name().equals(functionImport.getHttpMethod())) {
        legacyGetters++;
      } else if (HttpMethod.POST.name().equals(functionImport.getHttpMethod())) {
        legacyPosters++;
      } else if (functionImport.getHttpMethod() == null) {
        if (functionImport.isSideEffecting()) {
          actions++;
        } else {
          functions++;
        }
      }
    }
    assertEquals(6, legacyGetters);
    assertEquals(1, legacyPosters);
    assertEquals(5, actions);
    assertEquals(0, functions);
  }

  @Test
  public void multipleSchemas() {
    final XMLMetadata metadata = getClient().getDeserializer().
            toMetadata(getClass().getResourceAsStream("northwind-metadata.xml"));
    assertNotNull(metadata);

    final Schema first = metadata.getSchema("NorthwindModel");
    assertNotNull(first);

    final Schema second = metadata.getSchema("ODataWebV3.Northwind.Model");
    assertNotNull(second);

    final EntityContainer entityContainer = second.getDefaultEntityContainer();
    assertNotNull(entityContainer);
    assertEquals("NorthwindEntities", entityContainer.getName());
  }

  @Test
  public void complexAndEntityType() {
    final Edm metadata = getClient().getReader().
            readMetadata(getClass().getResourceAsStream("metadata.xml"));
    assertNotNull(metadata);

    final EdmEntityContainer container = metadata.getEntityContainer(
            new FullQualifiedName("Microsoft.Test.OData.Services.AstoriaDefaultService", "DefaultContainer"));
    assertNotNull(container);

    final EdmComplexType complex = metadata.getComplexType(
            new FullQualifiedName("Microsoft.Test.OData.Services.AstoriaDefaultService", "ContactDetails"));
    assertNotNull(complex);
    assertFalse(complex.getPropertyNames().isEmpty());
    assertTrue(complex.getProperty("EmailBag").isCollection());

    final EdmEntityType entity = metadata.getEntityType(
            new FullQualifiedName("Microsoft.Test.OData.Services.AstoriaDefaultService", "ProductReview"));
    assertNotNull(entity);
    assertFalse(entity.getPropertyNames().isEmpty());
    assertEquals(EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int32),
            entity.getProperty("ProductId").getType());

    assertFalse(entity.getKeyPropertyRefs().isEmpty());
    assertNotNull("ProductId", entity.getKeyPropertyRef("ProductId").getKeyPropertyName());
  }

  @Test
  public void functionImport() {
    final Edm metadata = getClient().getReader().
            readMetadata(getClass().getResourceAsStream("metadata.xml"));
    assertNotNull(metadata);

    final Set<String> actionImports = new HashSet<String>();
    for (EdmActionImportInfo info : metadata.getServiceMetadata().getActionImportInfos()) {
      actionImports.add(info.getActionImportName());
    }
    final Set<String> expectedAI = new HashSet<String>(Arrays.asList(new String[] {
      "ResetDataSource",
      "IncreaseSalaries",
      "Sack",
      "GetComputer",
      "ChangeProductDimensions",
      "ResetComputerDetailsSpecifications"}));
    assertEquals(expectedAI, actionImports);
    final Set<String> functionImports = new HashSet<String>();
    for (EdmFunctionImportInfo info : metadata.getServiceMetadata().getFunctionImportInfos()) {
      functionImports.add(info.getFunctionImportName());
    }
    final Set<String> expectedFI = new HashSet<String>(Arrays.asList(new String[] {
      "GetPrimitiveString",
      "GetSpecificCustomer",
      "GetCustomerCount",
      "GetArgumentPlusOne",
      "EntityProjectionReturnsCollectionOfComplexTypes",
      "InStreamErrorGetCustomer"}));
    assertEquals(expectedFI, functionImports);

    final EdmEntityContainer container = metadata.getEntityContainer(
            new FullQualifiedName("Microsoft.Test.OData.Services.AstoriaDefaultService", "DefaultContainer"));
    assertNotNull(container);

    final EdmFunctionImport getArgumentPlusOne = container.getFunctionImport("GetArgumentPlusOne");
    assertNotNull(getArgumentPlusOne);
    assertEquals(EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int32),
            getArgumentPlusOne.getUnboundFunction(null).getReturnType().getType());

    final EdmActionImport resetDataSource = container.getActionImport("ResetDataSource");
    assertNotNull(resetDataSource);
    assertTrue(resetDataSource.getUnboundAction().getParameterNames().isEmpty());
    assertNull(resetDataSource.getUnboundAction().getReturnType());

    final EdmEntityType computer = metadata.getEntityType(new FullQualifiedName(container.getNamespace(), "Computer"));
    assertNotNull(computer);

    final EdmFunction getComputer = metadata.getBoundFunction(
            new FullQualifiedName(container.getNamespace(), "GetComputer"),
            new FullQualifiedName(container.getNamespace(), computer.getName()),
            Boolean.FALSE, Arrays.asList("computer"));
    assertNotNull(getComputer);
    assertEquals(computer, getComputer.getParameter("computer").getType());
    assertEquals(computer, getComputer.getReturnType().getType());

    final EdmAction resetDataSource2 = metadata.getUnboundAction(
            new FullQualifiedName(container.getNamespace(), "ResetDataSource"));
    assertNotNull(resetDataSource2);
  }

  @Test
  public void navigation() {
    final Edm metadata = getClient().getReader().
            readMetadata(getClass().getResourceAsStream("metadata.xml"));
    assertNotNull(metadata);

    final EdmEntityContainer container = metadata.getEntityContainer(
            new FullQualifiedName("Microsoft.Test.OData.Services.AstoriaDefaultService", "DefaultContainer"));
    assertNotNull(container);

    final EdmEntitySet customer = container.getEntitySet("Customer");
    assertNotNull(customer);

    final EdmBindingTarget order = customer.getRelatedBindingTarget("Orders");
    assertNotNull(order);
    assertTrue(order instanceof EdmEntitySet);

    final EdmBindingTarget customerBindingTarget = ((EdmEntitySet) order).getRelatedBindingTarget("Customer");
    assertEquals(customer.getEntityType().getName(), customerBindingTarget.getEntityType().getName());
  }
}
