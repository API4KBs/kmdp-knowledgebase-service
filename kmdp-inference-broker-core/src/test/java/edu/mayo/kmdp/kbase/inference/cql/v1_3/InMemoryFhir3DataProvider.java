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
package edu.mayo.kmdp.kbase.inference.cql.v1_3;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opencds.cqf.cql.data.fhir.BaseDataProviderStu3;
import org.opencds.cqf.cql.runtime.Code;
import org.opencds.cqf.cql.runtime.Interval;

public class InMemoryFhir3DataProvider extends BaseDataProviderStu3 {

	private static FhirContext context = FhirContext.forDstu3();

	private Map<String, Set<Object>> pool = new HashMap<>();

	public InMemoryFhir3DataProvider(Map<String,Object> source) {
		super();
		this.setPackageName("org.hl7.fhir.dstu3.model");
		this.setFhirContext(context);
		source.entrySet().forEach(entry -> {
			Object value = entry.getValue();
			String key = value.getClass().getSimpleName();

			Set<Object> collection = pool.computeIfAbsent(key, k -> new HashSet<>());

			collection.add(value);
		});
	}

	@Override
	public Iterable<Object> retrieve( String context,
	                                  Object contextValue,
	                                  String dataType,
	                                  String templateId,
	                                  String codePath,
	                                  Iterable<Code> codes,
	                                  String valueSet,
	                                  String datePath,
	                                  String dateLowPath,
	                                  String dateHighPath,
	                                  Interval dateRange ) {
		Object val = pool.containsKey( dataType ) ? pool.get( dataType ) : lookup( codes );
		return val instanceof Iterable ? (Iterable<Object>) val : Collections.singletonList(val);
	}

	private Object lookup( Iterable<Code> codes ) {
		List<Object> vals = new ArrayList<>();
		if ( codes == null ) {
			return null;
		}
		codes.forEach( code -> {
			String key = code.getSystem() + code.getCode();
			if ( pool.containsKey( key ) ) {
				vals.add( pool.get( key ) );
			}
		});
		return vals;
	}

}
