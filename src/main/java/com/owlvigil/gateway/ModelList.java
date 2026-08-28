package com.owlvigil.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ModelList {
    public String object;
    public List<Model> data = Collections.emptyList();
    public String object() { return object; }
    public List<Model> data() { return data == null ? Collections.emptyList() : Collections.unmodifiableList(data); }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Model {
        public String id;
        public String object;
        public long created;
        @JsonProperty("owned_by") public String ownedBy;
        public String id() { return id; }
        public String object() { return object; }
        public long created() { return created; }
        public String ownedBy() { return ownedBy; }
    }
}
