// SPDX-License-Identifier: Apache-2.0
package io.github.stavshamir.springwolf.asyncapi.sqs;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.github.stavshamir.springwolf.asyncapi.controller.PublishingPayloadCreator;
import io.github.stavshamir.springwolf.asyncapi.controller.SpringwolfSqsController;
import io.github.stavshamir.springwolf.producer.SpringwolfSqsProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.github.stavshamir.springwolf.configuration.properties.SpringwolfSqsConfigConstants.SPRINGWOLF_SQS_CONFIG_PREFIX;
import static io.github.stavshamir.springwolf.configuration.properties.SpringwolfSqsConfigConstants.SPRINGWOLF_SQS_PLUGIN_PUBLISHING_ENABLED;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = SPRINGWOLF_SQS_CONFIG_PREFIX,
        name = SPRINGWOLF_SQS_PLUGIN_PUBLISHING_ENABLED,
        havingValue = "true")
public class SpringwolfSqsProducerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringwolfSqsProducer springwolfSqsProducer(List<SqsTemplate> sqsTemplates) {
        return new SpringwolfSqsProducer(sqsTemplates);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringwolfSqsController springwolfSqsController(
            PublishingPayloadCreator publishingPayloadCreator, SpringwolfSqsProducer springwolfSqsProducer) {
        return new SpringwolfSqsController(publishingPayloadCreator, springwolfSqsProducer);
    }
}
