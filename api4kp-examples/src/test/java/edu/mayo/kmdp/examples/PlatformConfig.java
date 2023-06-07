package edu.mayo.kmdp.examples;

import edu.mayo.kmdp.knowledgebase.KnowledgeBaseProvider;
import edu.mayo.kmdp.knowledgebase.introspectors.dmn.DMNMetadataIntrospector;
import edu.mayo.kmdp.language.LanguageDeSerializer;
import edu.mayo.kmdp.language.parsers.dmn.v1_1.DMN11Parser;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(
    basePackageClasses = {
        KnowledgeBaseProvider.class,
        LanguageDeSerializer.class})
public class PlatformConfig {

}
