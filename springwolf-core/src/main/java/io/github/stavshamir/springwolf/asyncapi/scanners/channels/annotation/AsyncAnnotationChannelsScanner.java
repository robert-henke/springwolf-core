// SPDX-License-Identifier: Apache-2.0
package io.github.stavshamir.springwolf.asyncapi.scanners.channels.annotation;

import com.asyncapi.v2._6_0.model.channel.ChannelItem;
import com.asyncapi.v2._6_0.model.channel.operation.Operation;
import com.asyncapi.v2._6_0.model.server.Server;
import com.asyncapi.v2.binding.message.MessageBinding;
import com.asyncapi.v2.binding.operation.OperationBinding;
import io.github.stavshamir.springwolf.asyncapi.scanners.bindings.MessageBindingProcessor;
import io.github.stavshamir.springwolf.asyncapi.scanners.bindings.OperationBindingProcessor;
import io.github.stavshamir.springwolf.asyncapi.scanners.channels.ChannelMerger;
import io.github.stavshamir.springwolf.asyncapi.scanners.channels.ChannelsScanner;
import io.github.stavshamir.springwolf.asyncapi.scanners.channels.operationdata.annotation.AsyncOperation;
import io.github.stavshamir.springwolf.asyncapi.scanners.channels.payload.PayloadClassExtractor;
import io.github.stavshamir.springwolf.asyncapi.scanners.classes.ClassScanner;
import io.github.stavshamir.springwolf.asyncapi.types.OperationData;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.message.Message;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.message.PayloadReference;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.message.header.AsyncHeaders;
import io.github.stavshamir.springwolf.asyncapi.types.channel.operation.message.header.HeaderReference;
import io.github.stavshamir.springwolf.configuration.AsyncApiDocketService;
import io.github.stavshamir.springwolf.schemas.SchemasService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
public class AsyncAnnotationChannelsScanner<A extends Annotation>
        implements ChannelsScanner, EmbeddedValueResolverAware {

    private final AsyncAnnotationProvider<A> asyncAnnotationProvider;
    private final ClassScanner classScanner;
    private final SchemasService schemasService;
    private final AsyncApiDocketService asyncApiDocketService;
    private final PayloadClassExtractor payloadClassExtractor;
    private final List<OperationBindingProcessor> operationBindingProcessors;
    private final List<MessageBindingProcessor> messageBindingProcessors;
    private StringValueResolver resolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Map<String, ChannelItem> scan() {
        List<Map.Entry<String, ChannelItem>> channels = classScanner.scan().stream()
                .flatMap(this::getAnnotatedMethods)
                .map(this::buildChannelItem)
                .filter(this::isInvalidChannelItem)
                .collect(toList());

        return ChannelMerger.merge(channels);
    }

    private Stream<MethodAndAnnotation<A>> getAnnotatedMethods(Class<?> type) {
        Class<A> annotationClass = this.asyncAnnotationProvider.getAnnotation();
        log.debug("Scanning class \"{}\" for @\"{}\" annotated methods", type.getName(), annotationClass.getName());

        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> !method.isBridge())
                .filter(method -> AnnotationUtil.findAnnotation(annotationClass, method) != null)
                .peek(method -> log.debug("Mapping method \"{}\" to channels", method.getName()))
                .flatMap(method -> AnnotationUtil.findAnnotations(annotationClass, method).stream()
                        .map(annotation -> new MethodAndAnnotation<>(method, annotation)));
    }

    private boolean isInvalidChannelItem(Map.Entry<String, ChannelItem> entry) {
        Operation publish = entry.getValue().getPublish();
        boolean publishBindingExists = publish != null && publish.getBindings() != null;

        Operation subscribe = entry.getValue().getSubscribe();
        boolean subscribeBindingExists = subscribe != null && subscribe.getBindings() != null;

        boolean allNonNull = entry.getKey() != null && (publishBindingExists || subscribeBindingExists);

        if (!allNonNull) {
            log.warn(
                    "Some data fields are null - method (channel={}) will not be documented: {}",
                    entry.getKey(),
                    entry.getValue());
        }

        return allNonNull;
    }

    private Map.Entry<String, ChannelItem> buildChannelItem(MethodAndAnnotation<A> methodAndAnnotation) {
        ChannelItem.ChannelItemBuilder channelBuilder = ChannelItem.builder();

        AsyncOperation operationAnnotation =
                this.asyncAnnotationProvider.getAsyncOperation(methodAndAnnotation.annotation());
        String channelName = resolver.resolveStringValue(operationAnnotation.channelName());

        Operation operation = buildOperation(operationAnnotation, methodAndAnnotation.method(), channelName);
        switch (this.asyncAnnotationProvider.getOperationType()) {
            case PUBLISH -> channelBuilder.publish(operation);
            case SUBSCRIBE -> channelBuilder.subscribe(operation);
        }
        ;

        List<String> servers = AsyncAnnotationScannerUtil.getServers(operationAnnotation, resolver);
        if (servers != null && !servers.isEmpty()) {
            validateServers(servers, operation.getOperationId());
            channelBuilder.servers(servers);
        }

        ChannelItem channelItem = channelBuilder.build();
        return Map.entry(channelName, channelItem);
    }

    private Operation buildOperation(AsyncOperation asyncOperation, Method method, String channelName) {
        String description = this.resolver.resolveStringValue(asyncOperation.description());
        if (!StringUtils.hasText(description)) {
            description = "Auto-generated description";
        }

        String operationId = channelName + "_" + this.asyncAnnotationProvider.getOperationType().operationName;

        Map<String, OperationBinding> operationBinding =
                AsyncAnnotationScannerUtil.processOperationBindingFromAnnotation(method, operationBindingProcessors);
        Map<String, Object> opBinding = operationBinding != null ? new HashMap<>(operationBinding) : null;

        return Operation.builder()
                .description(description)
                .operationId(operationId)
                .message(buildMessage(asyncOperation, method))
                .bindings(opBinding)
                .build();
    }

    private Message buildMessage(AsyncOperation operationData, Method method) {
        Class<?> payloadType = operationData.payloadType() != Object.class
                ? operationData.payloadType()
                : payloadClassExtractor.extractFrom(method);

        String modelName = this.schemasService.register(payloadType);
        AsyncHeaders asyncHeaders = AsyncAnnotationScannerUtil.getAsyncHeaders(operationData, resolver);
        String headerModelName = this.schemasService.register(asyncHeaders);

        var schema = payloadType.getAnnotation(Schema.class);
        String description = schema != null ? schema.description() : null;

        Map<String, MessageBinding> messageBinding =
                AsyncAnnotationScannerUtil.processMessageBindingFromAnnotation(method, messageBindingProcessors);

        var builder = Message.builder()
                .name(payloadType.getName())
                .title(payloadType.getSimpleName())
                .description(description)
                .payload(PayloadReference.fromModelName(modelName))
                .headers(HeaderReference.fromModelName(headerModelName))
                .bindings(messageBinding);

        // Retrieve the Message information obtained from the @AsyncMessage annotation. These values have higher
        // priority so if we find them, we need to override the default values.
        AsyncAnnotationScannerUtil.processAsyncMessageAnnotation(builder, operationData.message(), this.resolver);

        return builder.build();
    }

    /**
     * validates the given list of server names (for a specific operation) with the servers defined in the 'servers' part of
     * the current AsyncApi.
     *
     * @param serversFromOperation the server names defined for the current operation
     * @param operationId          operationId of the current operation - used for exception messages
     * @throws IllegalArgumentException if server from operation is not present in AsyncApi's servers definition.
     */
    void validateServers(List<String> serversFromOperation, String operationId) {
        if (!serversFromOperation.isEmpty()) {
            Map<String, Server> asyncApiServers =
                    this.asyncApiDocketService.getAsyncApiDocket().getServers();
            if (asyncApiServers == null || asyncApiServers.isEmpty()) {
                throw new IllegalArgumentException(String.format(
                        "Operation '%s' defines server refs (%s) but there are no servers defined in this AsyncAPI.",
                        operationId, serversFromOperation));
            }
            for (String server : serversFromOperation) {
                if (!asyncApiServers.containsKey(server)) {
                    throw new IllegalArgumentException(String.format(
                            "Operation '%s' defines unknown server ref '%s'. This AsyncApi defines these server(s): %s",
                            operationId, server, asyncApiServers.keySet()));
                }
            }
        }
    }

    public interface AsyncAnnotationProvider<A> {
        Class<A> getAnnotation();

        AsyncOperation getAsyncOperation(A annotation);

        OperationData.OperationType getOperationType();
    }

    private record MethodAndAnnotation<A>(Method method, A annotation) {}
}
