/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.plugins;

import java.time.Duration;
import software.amazon.smithy.java.client.core.AutoClientPlugin;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.client.core.ClientConfig;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.http.HttpMessageExchange;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.http.api.HeaderName;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.retries.api.RetrySafety;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds retry information to HTTP errors based on retry-after headers, 429 and 503 status codes, the presence of
 * idempotency tokens, if a request is idempotent or readonly, and whether an error is retryable.
 *
 * <p>This plugin is applied automatically when using an HTTP protocol via {@link HttpMessageExchange}.
 */
@SmithyInternalApi
public final class ApplyHttpRetryInfoPlugin implements AutoClientPlugin {
    private static final HeaderName X_AMZ_RETRY_AFTER = HeaderName.of("x-amz-retry-after");

    @Override
    public void configureClient(ClientConfig.Builder config) {
        // We can conditionally add the interceptor here because client transport can't change after construction.
        if (config.isUsingMessageExchange(HttpMessageExchange.INSTANCE)) {
            config.addInterceptor(Interceptor.INSTANCE);
        }
    }

    private static final class Interceptor implements ClientInterceptor {
        private static final ClientInterceptor INSTANCE = new Interceptor();

        @Override
        public <O extends SerializableStruct> O modifyBeforeAttemptCompletion(
                OutputHook<?, O, ?, ?> hook,
                RuntimeException error
        ) {
            if (error instanceof CallException ce
                    && ce.isRetrySafe() == RetrySafety.MAYBE
                    && hook.response() instanceof HttpResponse res) {
                applyRetryInfo(res, ce, hook.context());
            }
            return hook.forward(error);
        }
    }

    static void applyRetryInfo(HttpResponse response, CallException exception, Context context) {
        // (1) Check with the protocol if the server explicitly wants a retry.
        if (!applyRetryAfterHeader(response, exception)) {
            if (!applyThrottlingStatusCodes(response, exception)) {
                // (2) If no retry was detected so far, is it safe to retry because of a 5XX error + idempotency token?
                if (exception.isRetrySafe() == RetrySafety.MAYBE) {
                    var idempotencyTokenUsed = context.get(CallContext.IDEMPOTENCY_TOKEN) != null;
                    if (response.statusCode() >= 500 && idempotencyTokenUsed) {
                        exception.isRetrySafe(RetrySafety.YES);
                    } else {
                        exception.isRetrySafe(RetrySafety.NO);
                    }
                }
            }
        }
    }

    // Treat 429 and 503 errors as retryable throttling errors.
    private static boolean applyThrottlingStatusCodes(HttpResponse response, CallException exception) {
        if (response.statusCode() == 429 || response.statusCode() == 503) {
            exception.isRetrySafe(RetrySafety.YES);
            exception.isThrottle(true);
            return true;
        }
        return false;
    }

    // Per SEP: use x-amz-retry-after (integer milliseconds). Ignore standard Retry-After header.
    private static boolean applyRetryAfterHeader(HttpResponse response, CallException exception) {
        var xAmzRetryAfter = response.headers().firstValue(X_AMZ_RETRY_AFTER);
        if (xAmzRetryAfter != null) {
            try {
                var millis = Long.parseLong(xAmzRetryAfter);
                exception.isRetrySafe(RetrySafety.YES);
                exception.retryAfter(Duration.ofMillis(millis));
                return true;
            } catch (NumberFormatException e) {
                // Invalid value — ignore, fall back to exponential backoff
            }
        }
        return false;
    }
}
