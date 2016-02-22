package com.realtek.nasfun.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

/**
 * POJO for sending request about back up progress info:
 * @author phyllis
 *
 */
public class BackUpRequest{

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BackUpGetRequest{
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Request{
            private String action = "get";

            public String getAction() {
                return action;
            }
            public void setAction(String action) {
                this.action = action;
            }

            @Override
            public String toString() {
                return "Request [action=" + action + "]";
            }
        }

        public Request getRequest() {
            return request;
        }
        public void setRequest(Request request) {
            this.request = request;
        }

        private Request request;

        @Override
        public String toString() {
            return "BackUpGetRequest [request=" + request + "]";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class BackUpSetRequest{
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Request{
            @JsonIgnoreProperties(ignoreUnknown = true)
            static class Toggle{
                private String enable;

                public String getEnable() {
                    return enable;
                }
                public void setEnable(String enable) {
                    this.enable = enable;
                }

                @Override
                public String toString() {
                    return "Toggle [enable=" + enable + "]+";
                }
            }

            private String action = "set";
            private ArrayList<Toggle> data;

            public String getAction() {
                return action;
            }
            public void setAction(String action) {
                this.action = action;
            }
            public ArrayList<Toggle> getData() {
                return data;
            }
            public void setData(ArrayList<Toggle> data) {
                this.data = data;
            }

             @Override
            public String toString() {
                return "Request [action=" + action
                        +", data="+data+ "]";
            }
        }

        private Request request;

        public Request getRequest() {
            return request;
        }
        public void setRequest(Request request) {
            this.request = request;
        }

        @Override
        public String toString() {
            return "BackUpSetRequest [request=" + request + "]";
        }
    }

}