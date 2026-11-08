package com.bnpp.regliss.facade.dto.list;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public class ConcatenatedListDto implements Comparable<ConcatenatedListDto> {

    public Long id;
    public String label;
    public LocalDateTime publicationDate;
    public boolean deleted;
    public boolean active;
    public boolean genericFormat;
    public boolean reglissFormat;
    public boolean vigilanceFormat;
    public boolean safewatchFormat;
    public boolean fircosoftFormat;
    public boolean fullyEditable;
    public boolean canDownloadImport;
    public boolean canDownloadExport;
    public Set<ReglissListDto> lists=new HashSet<>();

    @Override
    public int compareTo(ConcatenatedListDto o) {
        if (this.fullyEditable && !o.fullyEditable) {
            return 1;
        } else if (!this.fullyEditable && o.fullyEditable) {
            return -1;
        } else if (!this.fullyEditable && !o.fullyEditable && this.label.equals("GlobalPlusOfac")) {
            return -1;
        } else if (!this.fullyEditable && !o.fullyEditable && o.label.equals("GlobalPlusOfac")) {
            return 1;
        } else {
            return 0;
        }
    }
}

