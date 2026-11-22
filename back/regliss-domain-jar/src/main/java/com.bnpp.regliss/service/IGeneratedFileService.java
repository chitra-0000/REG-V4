package com.bnpp.regliss.service;

import java.util.*;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.FileExportFormat;

public interface IGeneratedFileService {

    List<GeneratedFile> createGeneratedFileStubsAfterCertification(Version version);

    List<GeneratedFile> persistOfacGeneratedFilesStubs();

    List<GeneratedFile> getLatestGeneratedFilesForVersion(Long versionId);

    AddModSupExportFile createAddModSupFileStub(Version version);

    AddModSupAuditTrailFile createAddModSupAuditTrailFileStub(Version version);

    List<Long> persistConcatenatedListGeneratedFilesStubsInTx(Long id, List<FileExportFormat> formats);
}
