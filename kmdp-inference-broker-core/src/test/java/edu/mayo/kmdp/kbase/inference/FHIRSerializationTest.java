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
package edu.mayo.kmdp.kbase.inference;

import static edu.mayo.kmdp.util.JSonUtil.asMapOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.mayo.kmdp.util.JSonUtil;
import edu.mayo.kmdp.util.Util;
import edu.mayo.kmdp.util.fhir.fhir3.FHIR3JacksonModule;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.Type;
import org.junit.jupiter.api.Test;

public class FHIRSerializationTest {


	@Test
	public void testJsonSerial() {
		String str = "Your plain old FHIR String";
		Map<String,Type> m = new HashMap<>();
		m.put( "aaa", new Quantity(  ).setValue( 11 ).setUnit( "mm" ) );
		m.put( "bbb", new StringType().setValue( str ));

		Optional<String> xo = JSonUtil.writeJson( m, new FHIR3JacksonModule(), JSonUtil.defaultProperties() )
		                             .flatMap( Util::asString );

		assertTrue( xo.isPresent() );
		String x = xo.orElse("");

		assertTrue( x.contains( "11" ) );
		assertTrue( x.contains( "mm" ) );
		assertTrue( x.contains( str ) );

		Optional<java.util.Map<String,Type>> recon1 = JSonUtil.parseJson( x,
				new FHIR3JacksonModule(),
				asMapOf( String.class, Type.class ));
		assertTrue( recon1.isPresent() );

		java.util.Map<String,Type> map = recon1.orElse(Collections.emptyMap());
		assertTrue( map.get( "aaa" ) instanceof  Quantity );
		assertTrue( map.get( "bbb" ) instanceof StringType );
	}


}
