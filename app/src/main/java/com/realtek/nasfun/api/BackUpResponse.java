
package com.realtek.nasfun.api;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.realtek.nasfun.api.BackUpResponse.BackUpGetResponse.Response;
import com.realtek.nasfun.api.BackUpResponse.BackUpGetResponse.Response.Progress;

/**
 * POJO for getting response about back up progress info:
 * @author phyllis
 *
 */
public class BackUpResponse {
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BackUpGetResponse {
        public BackUpGetResponse(Response response) {
            this.response = response;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonDeserialize(using = BackUpGetResponseDeserializer.class)
         static class Response {
            public Response(String api_version, String resource, String action,  int status_code, String status_message, ArrayList<Progress> data) {
                this.api_version = api_version;
                this.resource = resource;
                this.status_code = status_code;
                this.action = action;
                this.status_message = status_message;
                this.data = data;
            }

            @JsonProperty("api version")
            private String api_version;
            private String resource;
            private String action;
            @JsonProperty("status code")
            private int status_code;
            @JsonProperty("status message")
            private String status_message;
            private ArrayList<Progress> data;

            @JsonIgnoreProperties(ignoreUnknown = true)
            static class Progress {
                public Progress(int progress) {
                    this.progress = progress;
                }

                private int progress;

                public int getProgress() {
                    return progress;
                }

                public void setProgress(int progress) {
                    this.progress = progress;
                }

                @Override
                public String toString() {
                    return "Progress [progress=" + progress + "]+";
                }
            }

            public String getApi_version() {
                return api_version;
            }

            public void setApi_version(String api_version) {
                this.api_version = api_version;
            }

            public String getResource() {

                return resource;
            }

            public void setResource(String resource) {

                this.resource = resource;
            }

            public String getAction() {

                return action;
            }

            public void setAction(String action) {
                this.action = action;
            }

            public int getStatus_code() {
                return status_code;
            }

            public void setStatus_code(int status_code) {
                this.status_code = status_code;
            }

            public String getStatus_message() {
                return status_message;
            }

            public void setStatus_message(String status_message) {
                this.status_message = status_message;
            }

            public ArrayList<Progress> getData() {
                return data;
            }

            public void setData(ArrayList<Progress> data) {
                this.data = data;
            }

            @Override
            public String toString() {
                return "Response [api_version=" + api_version + ", resource=" + resource
                        + ", action=" + action + ", status_code=" + Integer.toString(status_code)
                        + ", status_message=" + status_message
                        + ", data=" + data + "]";
            }
        }

        private Response response;

        public Response getResponse() {
            return response;
        }

        public void setResponse(Response response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return "BackUpGetResponse [response=" + response + "]";
        }
    }

    static class BackUpGetResponseDeserializer extends JsonDeserializer<BackUpGetResponse> {
        @Override
        public BackUpGetResponse deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode rootNode = jp.getCodec().readTree(jp);
            JsonNode responseNode = rootNode.get("response");
            String api_version  =  responseNode.get("api version").asText();
            String resource =  responseNode.get("resource").asText();
            String action =  responseNode.get("action").asText();
            int status_code = (Integer) ((IntNode) responseNode.get("status code")).numberValue();
            String status_message =  responseNode.get("status message").asText();
            JsonNode dataNode =  responseNode.get("data");

            // only get progress
            // ignore other values about error.
            if(dataNode != null && dataNode.get(0) != null && dataNode.get(0).get("progress")!=null) {
                ArrayList<Progress> data = new ArrayList<Progress>();
                int progressValue = (Integer)((IntNode) dataNode.get(0).get("progress")).numberValue();
                Progress progress = new Progress(progressValue);
                data.add(progress);
                return new BackUpGetResponse(new Response(api_version, resource, action, status_code, status_message, data));
            }

            return  new BackUpGetResponse(new Response(api_version, resource, action, status_code, status_message, null));

        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BackUpSetResponse {
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Response {
            @JsonProperty("api version")
            private String api_version;
            private String resource;
            private String action;
            @JsonProperty("status code")
            private int status_code;
            @JsonProperty("status message")
            private String status_message;

            public String getApi_version() {
                return api_version;
            }
            public void setApi_version(String api_version) {
                this.api_version = api_version;
            }
            public String getResource() {
                return resource;
            }
            public void setResource(String resource) {
                this.resource = resource;
            }
            public String getAction() {return action;}
            public void setAction(String action) {
                this.action = action;
            }
            public int getStatus_code() {
                return status_code;
            }
            public void setStatus_code(int status_code) {
                this.status_code = status_code;
            }
            public String getStatus_message() {
                return status_message;
            }
            public void setStatus_message(String status_message) {
                this.status_message = status_message;
            }

            @Override
            public String toString() {
                return "Response [api_version=" + api_version + ", resource=" + resource
                        + ", action=" + action + ", status_code=" + Integer.toString(status_code)
                        + ", status_message=" + status_message + "]";
            }
        }

        private Response response;

        public Response getResponse() {
            return response;
        }
        public void setResponse(Response response) {
            this.response = response;
        }

        @Override
        public String toString() {
            return "BackUpSetResponse [response=" + response + "]";
        }
    }
}