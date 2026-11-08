package com.bnpp.regliss.helper;

// [MISSING FROM SCREENSHOTS: lines 3-14 of the original file were not visible in the photos]

@Slf4j
@Component
public class TempFileUtils {

    @Value("${temp.file.folder}")
    private File baseTempFolder;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Getter
    private File nodeBaseTempFolder;

    @PostConstruct
    private void init() {
        nodeBaseTempFolder = new File(baseTempFolder, FilenameUtils.getName(nodeDetailsSupplier.getNodeId()));
    }

    private void createTempFolderForCurrentNode(){
        try {
            if(!nodeBaseTempFolder.exists() && !nodeBaseTempFolder.mkdirs()) {
                throw new ReglissException("Folder " + nodeBaseTempFolder.getAbsolutePath() + " does not exist and could nt be created");
            }
        } catch (ReglissException exception) {
            log.error((exception.getMessage()));
        }
    }

    private boolean emptyAndDeleteTempFolderForCurrentNode(){
        return FileSystemUtils.deleteRecursively(nodeBaseTempFolder);
    }

    public void recreateTempFolderForCurrentNode(){
        if(emptyAndDeleteTempFolderForCurrentNode() || !nodeBaseTempFolder.exists()) {
            createTempFolderForCurrentNode();
        } else {
            log.error("Error while deleting folder at path: " + nodeBaseTempFolder.getAbsolutePath());
        }
    }

}

