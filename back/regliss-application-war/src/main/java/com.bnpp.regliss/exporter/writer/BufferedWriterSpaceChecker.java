package com.bnpp.regliss.exporter.writer;

import org.apache.commons.io.output.ProxyWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

class BufferedWriterSpaceChecker extends BufferedWriter {
    public BufferedWriterSpaceChecker(Writer out, long freeSpaceBuffer, File tmpFile) {
        super(new ProxyWriter(out){
            @Override
            public void afterWrite(int n) throws IOException{
                long diskFreeSpace = 0;
                if(tmpFile.getParentFile() != null) {
                    diskFreeSpace = tmpFile.getParentFile().getFreeSpace();
                }
                long bufferSizeMB = 10;
                if(diskFreeSpace < freeSpaceBuffer*1024*1024 + bufferSizeMB*1024*1024){
                    throw new IOException("Too few space on disk!!!");
                }
            }
        }, 10*1024*1024);
    }
}
