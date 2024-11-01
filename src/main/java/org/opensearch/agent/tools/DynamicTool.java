/*
 *
 *  * Copyright OpenSearch Contributors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.opensearch.agent.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.xcontent.DeprecationHandler;
import org.opensearch.core.xcontent.MediaType;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.ml.common.spi.tools.Tool;
import org.opensearch.ml.common.spi.tools.ToolAnnotation;
import org.opensearch.rest.RestRequestCreator;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestResponse;
import org.opensearch.rest.ToolExecutor;

import java.util.HashMap;
import java.util.Map;

@ToolAnnotation(DynamicTool.TYPE)
public class DynamicTool implements Tool {

    private static final Logger log = LogManager.getLogger(DynamicTool.class);
    private final ToolExecutor toolExecutor;
    private final NamedXContentRegistry namedXContentRegistry;

    public static final String TYPE = "DynamicTool";

    public DynamicTool(ToolExecutor toolExecutor, NamedXContentRegistry namedXContentRegistry) {
        this.toolExecutor = toolExecutor;
        this.namedXContentRegistry = namedXContentRegistry;
    }

    @Override
    public String getType() {
        return DynamicTool.class.getSimpleName();
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public String getName() {
        return DynamicTool.class.getSimpleName();
    }

    @Override
    public void setName(String s) {

    }

    @Override
    public String getDescription() {
        return String.join(
            " ",
            "This tool gets index information from the OpenSearch cluster.",
            "It takes 2 optional arguments named `index` which is a comma-delimited list of one or more indices to get information from (default is an empty list meaning all indices),",
            "and `local` which means whether to return information from the local node only instead of the cluster manager node (default is false).",
            "The tool returns the indices information, including `health`, `status`, `index`, `uuid`, `pri`, `rep`, `docs.count`, `docs.deleted`, `store.size`, `pri.store. size `, `pri.store.size`, `pri.store`."
        );
    }

    @Override
    public void setDescription(String s) {

    }

    @Override
    public boolean validate(Map<String, String> map) {
        return true;
    }

    @Override
    public <T> void run(Map<String, String> parameters, ActionListener<T> listener) {
        RestRequest.Method method = RestRequest.Method.GET;
        String uri = "/.plugins-ml-config/_search";
        String rawContent = "{\"query\": {\"match_all\": {}}}";
        try {
            XContentBuilder builder = XContentBuilder.builder(XContentType.JSON.xContent());
            XContentParser parser = MediaType.fromMediaType("application/json").xContent()
                .createParser(namedXContentRegistry, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, rawContent);
            builder.copyCurrentStructure(parser);
            BytesReference content = BytesReference.bytes(builder);
            RestRequest request = RestRequestCreator.createRestRequest(namedXContentRegistry, method, uri, content, new HashMap<>());
            ActionListener<RestResponse> actionListener = ActionListener.wrap(r -> {
                listener.onResponse((T) r.content().utf8ToString());
            },e -> {
                log.error("Failed during tool execution");
                listener.onFailure(e);
            });
            toolExecutor.execute(request, actionListener);
        } catch (Exception ex) {
            listener.onFailure(ex);
        }
    }


    public static class Factory implements Tool.Factory<DynamicTool> {
        private ToolExecutor toolExecutor;
        private NamedXContentRegistry namedXContentRegistry;

        private static DynamicTool.Factory INSTANCE;

        public static DynamicTool.Factory getInstance() {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            synchronized (DynamicTool.class) {
                if (INSTANCE != null) {
                    return INSTANCE;
                }
                INSTANCE = new DynamicTool.Factory();
                return INSTANCE;
            }
        }

        public void init(ToolExecutor toolExecutor, NamedXContentRegistry namedXContentRegistry) {
            this.toolExecutor = toolExecutor;
            this.namedXContentRegistry = namedXContentRegistry;
        }

        @Override
        public DynamicTool create(Map<String, Object> map) {
            return new DynamicTool(
                toolExecutor,
                namedXContentRegistry
            );
        }

        @Override
        public String getDefaultDescription() {
            return String.join(
                    " ",
                    "This tool gets index information from the OpenSearch cluster.",
                    "It takes 2 optional arguments named `index` which is a comma-delimited list of one or more indices to get information from (default is an empty list meaning all indices),",
                    "and `local` which means whether to return information from the local node only instead of the cluster manager node (default is false).",
                    "The tool returns the indices information, including `health`, `status`, `index`, `uuid`, `pri`, `rep`, `docs.count`, `docs.deleted`, `store.size`, `pri.store. size `, `pri.store.size`, `pri.store`."
                );
        }

        @Override
        public String getDefaultType() {
            return DynamicTool.class.getSimpleName();
        }

        @Override
        public String getDefaultVersion() {
            return null;
        }

    }
}
