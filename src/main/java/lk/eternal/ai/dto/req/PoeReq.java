package lk.eternal.ai.dto.req;


import java.util.List;

public class PoeReq {

    private String version;
    private String type;
    private String conversationId;
    private String userId;
    private String messageId;
    private List<Query> query;
    private boolean skipSystemPrompt;
    private Object logitBias;
    private String metadata;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public List<Query> getQuery() {
        return query;
    }

    public void setQuery(List<Query> query) {
        this.query = query;
    }

    public boolean isSkipSystemPrompt() {
        return skipSystemPrompt;
    }

    public void setSkipSystemPrompt(boolean skipSystemPrompt) {
        this.skipSystemPrompt = skipSystemPrompt;
    }

    public Object getLogitBias() {
        return logitBias;
    }

    public void setLogitBias(Object logitBias) {
        this.logitBias = logitBias;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public static class Query {
        private String role;
        private String content;
        private String contentType;
        private long timestamp;
        private String messageId;
        private List<Object> feedback;
        private List<Object> attachments;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public List<Object> getFeedback() {
            return feedback;
        }

        public void setFeedback(List<Object> feedback) {
            this.feedback = feedback;
        }

        public List<Object> getAttachments() {
            return attachments;
        }

        public void setAttachments(List<Object> attachments) {
            this.attachments = attachments;
        }
    }
}
