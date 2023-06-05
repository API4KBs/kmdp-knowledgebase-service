/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.mayo.kmdp.kbase.inference.dmn;

import java.io.ByteArrayInputStream;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNRuntime;
import org.omg.spec.api4kp._20200801.services.KnowledgeBase;
import org.omg.spec.api4kp._20200801.services.KnowledgeCarrier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieDMNHelper {

	private static final Logger logger = LoggerFactory.getLogger(KieDMNHelper.class);

	protected KieDMNHelper() {
	  // hidden constructor
  }

  public static DMNRuntime initRuntime(KnowledgeBase knowledgeBase) {
    KieServices kieServices = KieServices.Factory.get();
    KnowledgeCarrier carrier = knowledgeBase.getManifestation();

    KieFileSystem kfs = kieServices.newKieFileSystem()
        .write(kieServices.getResources()
            .newInputStreamResource(
                new ByteArrayInputStream(carrier.asBinary()
                    .orElseThrow(UnsupportedOperationException::new)))
            .setTargetPath(
                "/" + carrier.getAssetId().getTag() + "/versions/" + carrier.getAssetId().getVersionTag())
            .setResourceType(ResourceType.DMN));

    KieModule km = kieServices.newKieBuilder(kfs).buildAll().getKieModule();
    KieContainer kieContainer = kieServices.newKieContainer(km.getReleaseId());

    kieContainer.verify().getMessages(Message.Level.ERROR)
        .forEach(msg -> logger.error(msg.getText()));

    return kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
  }
}
