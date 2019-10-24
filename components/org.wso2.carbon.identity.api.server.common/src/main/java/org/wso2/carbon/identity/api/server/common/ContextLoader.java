/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.api.server.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.api.server.common.error.APIError;
import org.wso2.carbon.identity.api.server.common.error.ErrorResponse;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.wso2.carbon.identity.api.server.common.Constants.SERVER_API_PATH_COMPONENT;
import static org.wso2.carbon.identity.api.server.common.Constants.TENANT_CONTEXT_PATH_COMPONENT;
import static org.wso2.carbon.identity.api.server.common.Constants.TENANT_NAME_FROM_CONTEXT;

/**
 * Load information from context.
 */
public class ContextLoader {

    private static final Log LOG = LogFactory.getLog(ContextLoader.class);

    private static final String ERROR_CODE = "SE-50001";
    private static final String ERROR_MESSAGE = "Unexpected Processing Error.";
    private static final String ERROR_DESCRIPTION = "Server encountered an error while building the resource location.";

    /**
     * Retrieves loaded tenant domain from carbon context.
     *
     * @return tenant domain of the request is being served.
     */
    public static String getTenantDomainFromContext() {

        String tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        if (IdentityUtil.threadLocalProperties.get().get(TENANT_NAME_FROM_CONTEXT) != null) {
            tenantDomain = (String) IdentityUtil.threadLocalProperties.get().get(TENANT_NAME_FROM_CONTEXT);
        }
        return tenantDomain;
    }

    /**
     * Retrieves authenticated username from carbon context.
     *
     * @return username of the authenticated user.
     */
    public static String getUsernameFromContext() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
    }

    /**
     * Build URI prepending the user API context with the proxy context path to the endpoint.
     * Ex: /t/<tenant-domain>/api/users/<endpoint>
     *
     * @param endpoint relative endpoint path.
     * @return Fully qualified URI.
     */
    public static URI buildURIForBody(String endpoint) {

        String tenantQualifiedRelativePath =
                String.format(TENANT_CONTEXT_PATH_COMPONENT, getTenantDomainFromContext()) + SERVER_API_PATH_COMPONENT;
        String url = IdentityUtil.getEndpointURIPath(tenantQualifiedRelativePath + endpoint, true, true);
        return URI.create(url);
    }

    /**
     * Build the complete URI prepending the user API context without the proxy context path, to the endpoint.
     * Ex: https://localhost:9443/t/<tenant-domain>/api/users/<endpoint>
     *
     * @param endpoint relative endpoint path.
     * @return Fully qualified and complete URI.
     */
    public static URI buildURIForHeader(String endpoint) {

        String tenantQualifiedRelativePath =
                String.format(TENANT_CONTEXT_PATH_COMPONENT, getTenantDomainFromContext()) + SERVER_API_PATH_COMPONENT;
        String url = IdentityUtil.getEndpointURIPath(tenantQualifiedRelativePath + endpoint, false, true);

        URI loc = URI.create(url);
        if (!loc.isAbsolute()) {
            Message currentMessage = PhaseInterceptorChain.getCurrentMessage();
            if (currentMessage != null) {
                UriInfo ui = new UriInfoImpl(currentMessage.getExchange().getInMessage(), null);
                try {
                    return new URI(ui.getBaseUri().getScheme(), ui.getBaseUri().getAuthority(), url, null, null);
                } catch (URISyntaxException e) {
                    LOG.error(ERROR_DESCRIPTION + ": " + e);
                    ErrorResponse errorResponse = new ErrorResponse.Builder()
                            .withCode(ERROR_CODE)
                            .withMessage(ERROR_MESSAGE)
                            .withDescription(ERROR_DESCRIPTION)
                            .build();
                    throw new APIError(Response.Status.INTERNAL_SERVER_ERROR, errorResponse);
                }
            }
        }
        return loc;
    }
}
