/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.terms;

import static edu.mayo.kmdp.terms.TermsTestUtil.prepopulate;

import edu.mayo.kmdp.terms.TermsTestUtil.MockRepo;
import java.net.URI;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetCatalogApi;
import org.omg.spec.api4kp._20200801.api.repository.asset.v4.KnowledgeAssetRepositoryApi;
import org.omg.spec.api4kp._20200801.api.terminology.v4.server.TermsApiInternal;
import org.omg.spec.api4kp._20200801.services.KPComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ComponentScan( basePackageClasses = TermsBrokerImpl.class )
public class TermsTestConfig {

  @Bean
  @Primary
  @KPComponent(implementation = "fhir")
  public TermsApiInternal fhirTerms() {
    MockRepo mock = new MockRepo();
    prepopulate(
        URI.create("https://ontology.mayo.edu/taxonomies/ClinicalTasks#"
            + "7fee57de-ad70-384b-ba8c-c03be5e2d80f"),
        "20201101",
        "/mock.skos.rdf",
        mock, mock);
    return new TermsFHIRFacade(
        KnowledgeAssetCatalogApi.newInstance(mock),
        KnowledgeAssetRepositoryApi.newInstance(mock));
  }

}
