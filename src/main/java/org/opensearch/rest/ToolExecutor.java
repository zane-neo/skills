/*
 *
 *  * Copyright OpenSearch Contributors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.opensearch.rest;

import org.opensearch.client.node.NodeClient;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.xcontent.NamedXContentRegistry;

import java.util.HashMap;
import java.util.Optional;

public class ToolExecutor {

    private final RestController restController;
    private final NodeClient client;

    public ToolExecutor(RestController restController, NodeClient nodeClient) {
        this.restController = restController;
        this.client = nodeClient;
    }

    public void execute(RestRequest request, ActionListener<RestResponse> listener) throws Exception {
        String rawPath = request.rawPath();
        String uri = request.uri();
        RestRequest.Method requestMethod = request.method();

        Optional<RestHandler> restHandler = restController.dispatchHandler(uri, rawPath, requestMethod, new HashMap<>());
        // Implement the logic to execute the tool

        RestChannel dummyChannel = new AbstractRestChannel(request, true) {
            @Override
            public void sendResponse(RestResponse response) {
                if (response.status().getStatus() >= 400) {
                    listener.onFailure(new Exception("Dummy rest call failed with status: " + response.status().getStatus()));
                } else {
                    listener.onResponse(response);
                }
            }
        };

        restHandler.get().handleRequest(request, dummyChannel, client);
    }

}
