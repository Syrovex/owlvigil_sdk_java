package com.owlvigil.management;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class WorkspaceList {
    public List<Workspace> items = Collections.emptyList();
    @JsonProperty("page_info") public PageInfo pageInfo = new PageInfo();
    public List<Workspace> items() { return items == null ? Collections.emptyList() : Collections.unmodifiableList(items); }
    public PageInfo pageInfo() { return pageInfo; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Workspace {
        public long id;
        @JsonProperty("workspace_id") public long workspaceId;
        public String name;
        public String slug;
        public String status;
        public String role;
        public long id() { return id; }
        public long workspaceId() { return workspaceId; }
        public String name() { return name; }
        public String slug() { return slug; }
        public String status() { return status; }
        public String role() { return role; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class PageInfo {
        @JsonProperty("next_cursor") public String nextCursor;
        @JsonProperty("has_more") public boolean hasMore;
        public String nextCursor() { return nextCursor == null ? "" : nextCursor; }
        public boolean hasMore() { return hasMore; }
    }
}
