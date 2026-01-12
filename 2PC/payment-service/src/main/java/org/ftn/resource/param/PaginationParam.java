package org.ftn.resource.param;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import org.jboss.resteasy.reactive.RestQuery;

public class PaginationParam {
    @NotNull(message = "Page omitted")
    @Min(value = 0, message = "Page must be at least 0")
    @DefaultValue("0")
    @RestQuery
    private Integer page;

    @NotNull(message = "Size omitted")
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size of page can be up to 100")
    @DefaultValue("10")
    @RestQuery
    private Integer size;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
