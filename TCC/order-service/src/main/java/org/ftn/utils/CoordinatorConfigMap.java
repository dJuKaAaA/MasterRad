package org.ftn.utils;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "coordinator")
public interface CoordinatorConfigMap {
    String username();
    String password();
}
