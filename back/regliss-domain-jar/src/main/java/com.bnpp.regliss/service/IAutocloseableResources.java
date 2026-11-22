package com.bnpp.regliss.service;

// TODO: [GAP - imports not captured in screenshots: import region was collapsed/folded in editor]

public interface IAutocloseableResources {

    File createTempFileForThisThread();

    File createTempFileForThisThread(String prefix, String suffix);

    InputStream openInputStreamForThisThread(File file) throws IOException;
}
