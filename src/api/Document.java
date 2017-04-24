package api;

import java.util.List;

import utils.MD5;

public class Document {

    private String url;
    private List<String> keywords;

    public Document() {
    }

    public Document(String url, List<String> keywords) {
        this.url = url;
        this.keywords = keywords;
    }

    public String id() {
        return MD5.hash(url);
    }

    public String getUrl() {
        return url;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String toString() {
        return String.format("%s : %s", url, keywords);
    }
}
