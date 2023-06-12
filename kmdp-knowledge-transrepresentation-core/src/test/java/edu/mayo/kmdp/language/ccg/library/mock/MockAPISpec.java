/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.language.ccg.library.mock;


import static edu.mayo.kmdp.registry.Registry.BASE_UUID_URN_URI;
import static org.omg.spec.api4kp._20200801.id.IdentifierConstants.VERSION_ZERO;
import static org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder.newSurrogate;

import java.net.URI;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.omg.spec.api4kp._20200801.id.ResourceIdentifier;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.omg.spec.api4kp._20200801.surrogate.KnowledgeAsset;
import org.omg.spec.api4kp._20200801.surrogate.SurrogateBuilder;

public class MockAPISpec {

	public static final ResourceIdentifier assetId =
			SurrogateBuilder.assetId( BASE_UUID_URN_URI, "c67be5b6-0804-46fa-a325-a65bc0cd323d",
	                                                "20.0.0" );
	public static final ResourceIdentifier artifactId =
			SurrogateBuilder.artifactId( BASE_UUID_URN_URI, "4d6a88e3-e0f0-455d-8e7f-347bbfcbf309", VERSION_ZERO);

	String modelName = "fhir-api.v20";

	public KnowledgeCarrier buildArtifact() {
		return AbstractCarrier.of("apiTest".getBytes())
				.withArtifactId(artifactId);
	}

	public KnowledgeAsset buildSurrogate( ) {

		KnowledgeAsset surrogate = newSurrogate(assetId)
				.withName( "Mock API","Mock Enterprise API" )
				.withServiceType()
				.withOpenAPIExpression()
				.withCarriers( artifactId,
				              URI.create( "https://foo.bar" ) )
				.get();

		return surrogate;
	}

	public ResourceIdentifier associateArtifactTo() {
		return null;
	}

}

