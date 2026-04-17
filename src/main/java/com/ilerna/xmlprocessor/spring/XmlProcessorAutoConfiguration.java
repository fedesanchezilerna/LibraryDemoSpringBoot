package com.ilerna.xmlprocessor.spring;

import com.ilerna.xmlprocessor.XmlProcessorConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class XmlProcessorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XmlProcessorConfig xmlProcessorConfig() {
        return new XmlProcessorConfig();
    }
}
