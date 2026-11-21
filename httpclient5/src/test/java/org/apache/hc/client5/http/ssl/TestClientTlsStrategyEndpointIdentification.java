/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.client5.http.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.reactor.ssl.SSLBufferMode;
import org.apache.hc.core5.reactor.ssl.SSLSessionInitializer;
import org.apache.hc.core5.reactor.ssl.SSLSessionVerifier;
import org.apache.hc.core5.reactor.ssl.TransportSecurityLayer;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class TestClientTlsStrategyEndpointIdentification {

    private static SSLEngine initializeEngine(final HostnameVerificationPolicy policy) throws Exception {
        final SSLContext sslContext = SSLContexts.createDefault();
        final DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext, policy, null);
        final TransportSecurityLayer tlsSession = Mockito.mock(TransportSecurityLayer.class);
        final HttpHost target = new HttpHost("https", "somehost", 443);

        tlsStrategy.upgrade(tlsSession, target, null, Timeout.ofSeconds(5), null);

        final ArgumentCaptor<SSLSessionInitializer> initializerCaptor = ArgumentCaptor.forClass(SSLSessionInitializer.class);
        Mockito.verify(tlsSession).startTls(
                ArgumentMatchers.same(sslContext),
                ArgumentMatchers.<NamedEndpoint>any(),
                ArgumentMatchers.<SSLBufferMode>any(),
                initializerCaptor.capture(),
                ArgumentMatchers.<SSLSessionVerifier>any(),
                ArgumentMatchers.<Timeout>any(),
                ArgumentMatchers.any());

        final SSLEngine sslEngine = sslContext.createSSLEngine(target.getHostName(), target.getPort());
        sslEngine.setUseClientMode(true);
        initializerCaptor.getValue().initialize(target, sslEngine);
        return sslEngine;
    }

    @Test
    void testBuiltinPolicyEnablesEndpointIdentificationOnEngine() throws Exception {
        final SSLEngine sslEngine = initializeEngine(HostnameVerificationPolicy.BUILTIN);
        Assertions.assertEquals(URIScheme.HTTPS.id, sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm());
    }

    @Test
    void testBothPolicyEnablesEndpointIdentificationOnEngine() throws Exception {
        final SSLEngine sslEngine = initializeEngine(HostnameVerificationPolicy.BOTH);
        Assertions.assertEquals(URIScheme.HTTPS.id, sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm());
    }

}
