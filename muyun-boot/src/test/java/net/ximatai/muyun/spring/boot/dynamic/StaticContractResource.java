package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.Map;

@ApplicationScoped
@Path("/sales.contract")
public class StaticContractResource {
    @POST
    @Path("/query")
    public Map<String, String> query() {
        return Map.of("source", "static");
    }
}
