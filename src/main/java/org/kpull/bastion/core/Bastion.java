package org.kpull.bastion.core;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.jglue.fluentjson.JsonArrayBuilder;
import org.jglue.fluentjson.JsonBuilderFactory;
import org.jglue.fluentjson.JsonObjectBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * @author <a href="mailto:mail@kylepullicino.com">Kyle</a>
 */
public class Bastion {

    private static Gson gson = new Gson();

    private String name = "";
    private ApiEnvironment environment = new ApiEnvironment();
    private List<ApiCall> apiCalls = new LinkedList<>();
    private Bastion() {
    }

    public static Bastion start() {
        return new Bastion();
    }

    public Bastion name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }

    public ApiEnvironmentBuilder environment() {
        return new ApiEnvironmentBuilder();
    }

    public ApiCallBuilder call(String name) {
        return new ApiCallBuilder(name);
    }

    public ApiSuite build() {
        return new ApiSuite(name, environment, apiCalls);
    }

    public class ApiEnvironmentBuilder {
        private Map<String, String> entries = new HashMap<>();

        private ApiEnvironmentBuilder() {
        }

        public ApiEnvironmentBuilder entry(String key, Object value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            entries.put(key, value.toString());
            return this;
        }

        public Bastion done() {
            environment.putAll(entries);
            return Bastion.this;
        }
    }

    public class ApiCallBuilder {
        private String name = "";
        private String description = "";
        private ApiRequest request = new ApiRequest("", "", Collections.emptyList(), "", "", Collections.emptyList());
        private ApiResponse response = new ApiResponse(Collections.emptyList(), 0, "", "");
        private Class<?> responseModel = null;
        private Assertions<?> assertions = null;
        private Callback postCallExecution = Callback.NO_OPERATION_CALLBACK;

        private ApiCallBuilder(String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        public ApiCallBuilder description(String description) {
            Objects.requireNonNull(description);
            this.description = description;
            return this;
        }

        public PostCallScriptBuilder afterwardsExecute() {
            return new PostCallScriptBuilder();
        }

        public <M> ResponseModelBuilder<M> bind(Class<M> responseModel) {
            Objects.requireNonNull(responseModel);
            this.responseModel = responseModel;
            return new ResponseModelBuilder<>();
        }

        public ApiRequestBuilder request(String method, String url) {
            return new ApiRequestBuilder(method, url);
        }

        public Bastion done() {
            apiCalls.add(new ApiCall(name, description, request, responseModel, assertions, postCallExecution));
            return Bastion.this;
        }

        public class PostCallScriptBuilder {

            public ApiCallBuilder nothing() {
                postCallExecution = Callback.NO_OPERATION_CALLBACK;
                return ApiCallBuilder.this;
            }

            public ApiCallBuilder groovy(String groovyScript) {
                Objects.requireNonNull(groovyScript);
                postCallExecution = new GroovyCallback(groovyScript);
                return ApiCallBuilder.this;
            }

            public ApiCallBuilder groovyFromFile(File groovyFile) {
                try {
                    Objects.requireNonNull(groovyFile);
                    postCallExecution = new GroovyCallback(FileUtils.readFileToString(groovyFile));
                    return ApiCallBuilder.this;
                } catch (IOException e) {
                    throw new IllegalStateException(format("Cannot open file: %s", groovyFile), e);
                }
            }

            public ApiCallBuilder callback(Callback callback) {
                Objects.requireNonNull(callback);
                ApiCallBuilder.this.postCallExecution = callback;
                return ApiCallBuilder.this;
            }
        }

        public class ResponseModelBuilder<M> {

            public ApiCallBuilder assertions(Assertions<M> assertions) {
                Objects.requireNonNull(assertions);
                ApiCallBuilder.this.assertions = assertions;
                return ApiCallBuilder.this;
            }

            public ApiCallBuilder noAssertions() {
                return ApiCallBuilder.this;
            }

        }

        public class ApiRequestBuilder {
            private String method = "";
            private String url = "";
            private List<ApiHeader> headers = new LinkedList<>();
            private List<ApiQueryParam> queryParams = new LinkedList<>();
            private String type = "";
            private String body = "";

            private ApiRequestBuilder(String method, String url) {
                Objects.requireNonNull(method);
                Objects.requireNonNull(url);
                this.method = method;
                this.url = url;
            }

            public ApiRequestBuilder type(String type) {
                Objects.requireNonNull(type);
                this.type = type;
                return this;
            }

            public ApiRequestBuilder body(String body) {
                Objects.requireNonNull(body);
                this.body = body;
                return this;
            }

            public ApiRequestBuilder bodyFromModel(Object model) {
                Objects.requireNonNull(model);
                this.body = gson.toJson(model);
                return this;
            }

            public ApiRequestBuilder bodyFromJsonObject(Function<JsonObjectBuilder<?, ?>, String> jsonBuilder) {
                Objects.requireNonNull(jsonBuilder);
                this.body = jsonBuilder.apply(JsonBuilderFactory.buildObject());
                return this;
            }

            public ApiRequestBuilder bodyFromJsonArray(Function<JsonArrayBuilder, String> jsonBuilder) {
                Objects.requireNonNull(jsonBuilder);
                this.body = jsonBuilder.apply(JsonBuilderFactory.buildArray());
                return this;
            }

            public ApiRequestBuilder bodyFromFile(File body) {
                try {
                    Objects.requireNonNull(body);
                    this.body = FileUtils.readFileToString(body);
                    return this;
                } catch (IOException e) {
                    throw new IllegalStateException(format("Cannot open file: %s", body), e);
                }
            }

            public ApiRequestBuilder header(String name, String value) {
                Objects.requireNonNull(name);
                Objects.requireNonNull(value);
                headers.add(new ApiHeader(name, value));
                return this;
            }

            public ApiRequestBuilder queryParam(String name, String value) {
                Objects.requireNonNull(name);
                Objects.requireNonNull(value);
                queryParams.add(new ApiQueryParam(name, value));
                return this;
            }

            public ApiCallBuilder done() {
                request = new ApiRequest(method, url, headers, type, body, queryParams);
                return ApiCallBuilder.this;
            }
        }

    }
}
