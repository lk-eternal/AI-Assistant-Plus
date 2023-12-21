package lk.eternal.ai.dto.resp;

import java.util.List;

public class GeminiResp {

    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    private Error error;


    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public PromptFeedback getPromptFeedback() {
        return promptFeedback;
    }

    public void setPromptFeedback(PromptFeedback promptFeedback) {
        this.promptFeedback = promptFeedback;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;
        private List<SafetyRating> safetyRatings;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    public static class Content {
        private List<Part> parts;
        private String role;

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class SafetyRating {
        private String category;
        private String probability;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProbability() {
            return probability;
        }

        public void setProbability(String probability) {
            this.probability = probability;
        }
    }

    public static class PromptFeedback {
        private List<SafetyRating> safetyRatings;

        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    public static class CitationMetadata {
        private List<CitationSource> citationSources;

        public List<CitationSource> getCitationSources() {
            return citationSources;
        }

        public void setCitationSources(List<CitationSource> citationSources) {
            this.citationSources = citationSources;
        }
    }

    public static class CitationSource {
        private int startIndex;
        private int endIndex;
        private String uri;
        private String license;

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getLicense() {
            return license;
        }

        public void setLicense(String license) {
            this.license = license;
        }
    }

    public static class Response {
        private List<Candidate> candidates;
        private PromptFeedback promptFeedback;

        public List<Candidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Candidate> candidates) {
            this.candidates = candidates;
        }

        public PromptFeedback getPromptFeedback() {
            return promptFeedback;
        }

        public void setPromptFeedback(PromptFeedback promptFeedback) {
            this.promptFeedback = promptFeedback;
        }
    }

    public static class Error {

        private int code;
        private String message;
        private String status;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
