/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.plugins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.retries.api.RetrySafety;

public class ApplyHttpRetryInfoPluginTest {

    @Test
    public void appliesXAmzRetryAfterHeader() {
        var response = HttpResponse.create()
                .setStatusCode(500)
                .setHeaders(HttpHeaders.of(Map.of("x-amz-retry-after", List.of("1500"))))
                .toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        assertThat(e.isRetrySafe(), is(RetrySafety.YES));
        assertThat(e.retryAfter(), equalTo(Duration.ofMillis(1500)));
    }

    @Test
    public void ignoresInvalidXAmzRetryAfterHeader() {
        var response = HttpResponse.create()
                .setStatusCode(500)
                .setHeaders(HttpHeaders.of(Map.of("x-amz-retry-after", List.of("invalid"))))
                .toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();
        context.put(CallContext.IDEMPOTENCY_TOKEN, "foo");

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        // Falls through to normal 5xx + idempotency handling
        assertThat(e.isRetrySafe(), is(RetrySafety.YES));
        assertThat(e.retryAfter(), nullValue());
    }

    @Test
    public void ignoresStandardRetryAfterHeader() {
        // SEP: SDKs MUST ignore the standard HTTP Retry-After header
        var response = HttpResponse.create()
                .setStatusCode(500)
                .setHeaders(HttpHeaders.of(Map.of("retry-after", List.of("10"))))
                .toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        // Standard Retry-After is ignored, falls through to non-retryable (no idempotency token)
        assertThat(e.isRetrySafe(), is(RetrySafety.NO));
        assertThat(e.retryAfter(), nullValue());
    }

    @Test
    public void appliesThrottlingStatusCode503() {
        var response = HttpResponse.create().setStatusCode(503).toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        assertThat(e.isRetrySafe(), is(RetrySafety.YES));
        assertThat(e.isThrottle(), is(true));
        assertThat(e.retryAfter(), nullValue());
    }

    @Test
    public void appliesThrottlingStatusCode429() {
        var response = HttpResponse.create().setStatusCode(429).toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        assertThat(e.isRetrySafe(), is(RetrySafety.YES));
        assertThat(e.isThrottle(), is(true));
        assertThat(e.retryAfter(), nullValue());
    }

    @Test
    public void retriesSafe5xx() {
        var response = HttpResponse.create().setStatusCode(500).toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();
        context.put(CallContext.IDEMPOTENCY_TOKEN, "foo");

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        assertThat(e.isRetrySafe(), is(RetrySafety.YES));
        assertThat(e.isThrottle(), is(false));
        assertThat(e.retryAfter(), nullValue());
    }

    @Test
    public void doesNotRetryUnsafe5xx() {
        var response = HttpResponse.create().setStatusCode(500).toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        assertThat(e.isRetrySafe(), is(RetrySafety.NO));
        assertThat(e.isThrottle(), is(false));
        assertThat(e.retryAfter(), nullValue());
    }

    @Test
    public void doesNotRetryNormal4xx() {
        var response = HttpResponse.create().setStatusCode(400).toUnmodifiable();
        var e = new CallException("err");
        var context = Context.create();

        ApplyHttpRetryInfoPlugin.applyRetryInfo(response, e, context);

        assertThat(e.isRetrySafe(), is(RetrySafety.NO));
        assertThat(e.isThrottle(), is(false));
        assertThat(e.retryAfter(), nullValue());
    }
}
