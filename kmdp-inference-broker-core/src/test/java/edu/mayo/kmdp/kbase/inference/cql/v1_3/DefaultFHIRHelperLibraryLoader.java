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

import edu.mayo.kmdp.util.FileUtil;
import java.io.InputStream;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.omg.spec.api4kp._20200801.AbstractCarrier;
import org.opencds.cqf.cql.execution.LibraryLoader;

public class DefaultFHIRHelperLibraryLoader implements LibraryLoader {

	private CQL2ELMTranslatorHelper translator;

	DefaultFHIRHelperLibraryLoader(CQL2ELMTranslatorHelper translator) {
		this.translator = translator;
	}

	@Override
	public Library load(VersionedIdentifier libraryIdentifier) {
		String libName =
				"/org/hl7/fhir/" + libraryIdentifier.getId() + "-" + libraryIdentifier.getVersion()
						+ ".cql";
		InputStream is = CQLEvaluator.class.getResourceAsStream(libName);
		return FileUtil.readBytes(is)
				.map(AbstractCarrier::of)
				.flatMap(translator::cqlToExecutableLibrary)
				.orElseThrow(
						() -> new IllegalStateException("Unable to load library " + libraryIdentifier));
	}
}
