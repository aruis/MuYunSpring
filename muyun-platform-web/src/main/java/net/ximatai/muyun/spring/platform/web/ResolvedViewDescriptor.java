package net.ximatai.muyun.spring.platform.web;

import java.util.List;

public record ResolvedViewDescriptor(String viewCode,
                                     ModuleViewKind viewKind,
                                     ModuleUiClientType clientType,
                                     String title,
                                     List<ResolvedViewFieldDescriptor> fields) {
    public ResolvedViewDescriptor {
        if (viewCode == null || viewCode.isBlank()) {
            throw new IllegalArgumentException("view code must not be blank");
        }
        viewCode = viewCode.trim();
        if (viewKind == null) {
            throw new IllegalArgumentException("view kind must not be null");
        }
        clientType = clientType == null ? ModuleUiClientType.WEB : clientType;
        title = title == null || title.isBlank() ? null : title.trim();
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
