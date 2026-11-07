package com.bnpp.regliss.config.thread.autocloseable;

import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CloseableThreadResources {
    @Getter
    private List<Closeable> toClose = new ArrayList<>();
    @Getter
    private Set<File> filesToDelete = new HashSet<>();
    public void closeLater(Closeable closeable) { toClose.add(closeable); }
    public void deleteLater(File file) { filesToDelete.add(file); }
}

