package lk.eternal.ai.dto.resp;

import java.util.List;

public class SearchResponse {
    private String kind;
    private Url url;
    private Queries queries;
    private Context context;
    private SearchInformation searchInformation;
    private List<Item> items;


    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public Queries getQueries() {
        return queries;
    }

    public void setQueries(Queries queries) {
        this.queries = queries;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public SearchInformation getSearchInformation() {
        return searchInformation;
    }

    public void setSearchInformation(SearchInformation searchInformation) {
        this.searchInformation = searchInformation;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Url {
        private String type;
        private String template;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }
    }

    public static class Queries {
        private List<Request> request;
        private List<Request> nextPage;

        public List<Request> getRequest() {
            return request;
        }

        public void setRequest(List<Request> request) {
            this.request = request;
        }

        public List<Request> getNextPage() {
            return nextPage;
        }

        public void setNextPage(List<Request> nextPage) {
            this.nextPage = nextPage;
        }
    }

    public static class Request {
        private String title;
        private String totalResults;
        private String searchTerms;
        private int count;
        private int startIndex;
        private String inputEncoding;
        private String outputEncoding;
        private String safe;
        private String cx;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTotalResults() {
            return totalResults;
        }

        public void setTotalResults(String totalResults) {
            this.totalResults = totalResults;
        }

        public String getSearchTerms() {
            return searchTerms;
        }

        public void setSearchTerms(String searchTerms) {
            this.searchTerms = searchTerms;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public String getInputEncoding() {
            return inputEncoding;
        }

        public void setInputEncoding(String inputEncoding) {
            this.inputEncoding = inputEncoding;
        }

        public String getOutputEncoding() {
            return outputEncoding;
        }

        public void setOutputEncoding(String outputEncoding) {
            this.outputEncoding = outputEncoding;
        }

        public String getSafe() {
            return safe;
        }

        public void setSafe(String safe) {
            this.safe = safe;
        }

        public String getCx() {
            return cx;
        }

        public void setCx(String cx) {
            this.cx = cx;
        }
    }

    public static class Context {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class SearchInformation {
        private double searchTime;
        private String formattedSearchTime;
        private String totalResults;
        private String formattedTotalResults;

        public double getSearchTime() {
            return searchTime;
        }

        public void setSearchTime(double searchTime) {
            this.searchTime = searchTime;
        }

        public String getFormattedSearchTime() {
            return formattedSearchTime;
        }

        public void setFormattedSearchTime(String formattedSearchTime) {
            this.formattedSearchTime = formattedSearchTime;
        }

        public String getTotalResults() {
            return totalResults;
        }

        public void setTotalResults(String totalResults) {
            this.totalResults = totalResults;
        }

        public String getFormattedTotalResults() {
            return formattedTotalResults;
        }

        public void setFormattedTotalResults(String formattedTotalResults) {
            this.formattedTotalResults = formattedTotalResults;
        }
    }

    public static class Item {
        private String kind;
        private String title;
        private String htmlTitle;
        private String link;
        private String displayLink;
        private String snippet;
        private String htmlSnippet;
        private String cacheId;
        private String formattedUrl;
        private String htmlFormattedUrl;
        private PageMap pagemap;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getHtmlTitle() {
            return htmlTitle;
        }

        public void setHtmlTitle(String htmlTitle) {
            this.htmlTitle = htmlTitle;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getDisplayLink() {
            return displayLink;
        }

        public void setDisplayLink(String displayLink) {
            this.displayLink = displayLink;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public String getHtmlSnippet() {
            return htmlSnippet;
        }

        public void setHtmlSnippet(String htmlSnippet) {
            this.htmlSnippet = htmlSnippet;
        }

        public String getCacheId() {
            return cacheId;
        }

        public void setCacheId(String cacheId) {
            this.cacheId = cacheId;
        }

        public String getFormattedUrl() {
            return formattedUrl;
        }

        public void setFormattedUrl(String formattedUrl) {
            this.formattedUrl = formattedUrl;
        }

        public String getHtmlFormattedUrl() {
            return htmlFormattedUrl;
        }

        public void setHtmlFormattedUrl(String htmlFormattedUrl) {
            this.htmlFormattedUrl = htmlFormattedUrl;
        }

        public PageMap getPagemap() {
            return pagemap;
        }

        public void setPagemap(PageMap pagemap) {
            this.pagemap = pagemap;
        }
    }

    public static class PageMap {
        private List<MetaTag> metatags;

        public List<MetaTag> getMetatags() {
            return metatags;
        }

        public void setMetatags(List<MetaTag> metatags) {
            this.metatags = metatags;
        }
    }

    public static class MetaTag {
        private String referrer;
        private String ogImage;
        private String themeColor;
        private int ogImageWidth;
        private String ogType;
        private String viewport;
        private String ogTitle;
        private int ogImageHeight;
        private String formatDetection;

        public String getReferrer() {
            return referrer;
        }

        public void setReferrer(String referrer) {
            this.referrer = referrer;
        }

        public String getOgImage() {
            return ogImage;
        }

        public void setOgImage(String ogImage) {
            this.ogImage = ogImage;
        }

        public String getThemeColor() {
            return themeColor;
        }

        public void setThemeColor(String themeColor) {
            this.themeColor = themeColor;
        }

        public int getOgImageWidth() {
            return ogImageWidth;
        }

        public void setOgImageWidth(int ogImageWidth) {
            this.ogImageWidth = ogImageWidth;
        }

        public String getOgType() {
            return ogType;
        }

        public void setOgType(String ogType) {
            this.ogType = ogType;
        }

        public String getViewport() {
            return viewport;
        }

        public void setViewport(String viewport) {
            this.viewport = viewport;
        }

        public String getOgTitle() {
            return ogTitle;
        }

        public void setOgTitle(String ogTitle) {
            this.ogTitle = ogTitle;
        }

        public int getOgImageHeight() {
            return ogImageHeight;
        }

        public void setOgImageHeight(int ogImageHeight) {
            this.ogImageHeight = ogImageHeight;
        }

        public String getFormatDetection() {
            return formatDetection;
        }

        public void setFormatDetection(String formatDetection) {
            this.formatDetection = formatDetection;
        }
    }
}