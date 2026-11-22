package com.bnpp.regliss.service;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.repository.ConcatenatedListRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.bnpp.regliss.entity.referential.FileExportFormat.DJ_AME;
import static com.bnpp.regliss.entity.referential.FileExportFormat.DJ_WATCHLIST;

@Service
public class SubscriberService {

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private ConcatenatedListRepository concatenatedListRepository;

    public List<Subscription> getAllSubscriptionCombinations() {
        List<Subscription> allCombinations = new ArrayList<>();

        allCombinations.addAll(generateSubscriptionsForLists());
        allCombinations.addAll(generateSubscriptionsForOfac());
        return allCombinations;
    }

    public List<Subscription> getAllSubscriptionsForListAndConcatenatedList() {
        List<Subscription> allCombinations = new ArrayList<>();

        allCombinations.addAll(generateSubscriptionsForLists());
        allCombinations.addAll(generateSubscriptionsForConcatenatedList());
        return allCombinations;
    }

    private List<Subscription> generateSubscriptionsForLists() {
        List<Subscription> allCombinations = new ArrayList<>();
        for (ReglissList list : listRepo.findByDeletedFalse()) {
            for (GeneratedFileFormat format : generateAllCombinationsForList(list)) {
                Subscription subscription = new Subscription(format, list);
                allCombinations.add(subscription);
            }
        }
        return allCombinations;
    }

    private List<Subscription> generateSubscriptionsForOfac() {
        List<Subscription> allCombinations = new ArrayList<>();
        ReglissList ofacSubscriptionList = null;
        for (GeneratedFileFormat format : generateOfacCombinations(Arrays.asList(FileExportFormat.values()))) {
            Subscription subscription = new Subscription(format, ofacSubscriptionList);
            allCombinations.add(subscription);
        }
        return allCombinations;
    }

    private List<Subscription> generateSubscriptionsForConcatenatedList(){
        List<Subscription> allCombinations = new ArrayList<>();
        for (ConcatenatedList concatenatedList : concatenatedListRepository.findAllNotDeletedAndActiveConcatenatedLists()) {
            for (GeneratedFileFormat format : generateConcatenatedListCombinations(concatenatedList, concatenatedList.getConcatenatedListExportFormats())) {
                Subscription subscription = new Subscription(format, concatenatedList);
                allCombinations.add(subscription);
            }
        }
        return allCombinations;
    }

    public List<GeneratedFileFormat> generateAllCombinationsForList(ReglissList list) {
        return generateCombinationsForFormats(list.getEnabledFileFormats(), list.getActiveFiltersWithGenerteExportFiles());
    }

    public List<GeneratedFileFormat> generateCombinationsForFormats(List<FileExportFormat> formats,
            Set<DJExportFilter> filters) {
        List<GeneratedFileFormat> formatCombinations = new ArrayList<>();
        for (FileExportFormat format : formats) {
            if (format == DJ_WATCHLIST || format == DJ_AME) {
                for (DJExportFilter filter : filters) {
                    Set<GeneratedFileFormat> files=filter.getExtensions().stream().map(e->new GeneratedFileFormat(filter,e)).collect(Collectors.toSet());
                    if(filter.getExtensions().contains(FileExportExtension.zip)){
                    }
                    files.add(new GeneratedFileFormat(filter ,FileExportExtension.xml));
                    formatCombinations.addAll(files);

                }

            } else {
                for (FileExportMode fileExportMode : format.getExportModes()) {
                    formatCombinations.addAll(generateCombinations(format, fileExportMode));
                }
            }
        }
        return formatCombinations;
    }


    public List<GeneratedFileFormat> generateCombinationsForRegularFormats(List<String> stringFormats){
        List<GeneratedFileFormat> formatCombinations = new ArrayList<>();
        List<FileExportFormat> formats=new ArrayList<>();
        for(String stringFormat:stringFormats){
            formats.add(FileExportFormat.valueOf(stringFormat));
        }
        for(FileExportFormat format: formats){
            for(FileExportMode fileExportMode : format.getExportModes()){
                formatCombinations.addAll(generateCombinations(format,fileExportMode));
            }
        }

        return formatCombinations;
    }

    public List<GeneratedFileFormat> generateCombinationsForWLFilters(List<String> filtersNames, Set<DJExportFilter> filters){
        List<GeneratedFileFormat> formatCombinations = new ArrayList<>();
        Set<DJExportFilter> selectedFilters=new HashSet<>();
        for(DJExportFilter filter:filters){
            for(String filterName: filtersNames){
                if(filter.getFilterName().equals(filterName)){
                    selectedFilters.add(filter);
                }
            }
        }

        for(DJExportFilter selectedFilter:selectedFilters){
            Set<GeneratedFileFormat> files=selectedFilter.getExtensions().stream().map(e->new GeneratedFileFormat(selectedFilter,e)).collect(Collectors.toSet());
            if(selectedFilter.getExtensions().contains(FileExportExtension.zip)){
            }
            files.add(new GeneratedFileFormat(selectedFilter,FileExportExtension.xml));
            formatCombinations.addAll(files);
        }

        return formatCombinations;
    }

    public List<GeneratedFileFormat> generateOfacCombinations(List<FileExportFormat> formats) {
        List<GeneratedFileFormat> ofacCombinations = new ArrayList<>();
        for (FileExportFormat format : formats) {
            if (format.isSupportsGlobal()) {
                ofacCombinations.addAll(generateCombinations(format, FileExportMode.FULL));
            }
        }
        return ofacCombinations;
    }

    public List<GeneratedFileFormat> generateConcatenatedListCombinations(ConcatenatedList concatenatedList, List<FileExportFormat> formats) {
        List<GeneratedFileFormat> concatenatedListCombinations = new ArrayList<>();
        List<FileExportFormat> concatenatedFormat = flterConcatenatedListFormat(concatenatedList, formats);
        for (FileExportFormat format : concatenatedFormat) {
            concatenatedListCombinations.addAll(generateCombinations(format, FileExportMode.FULL));
        }
        return concatenatedListCombinations;
    }

    private List<GeneratedFileFormat> generateCombinations(FileExportFormat format, FileExportMode fileExportMode) {
        return format.getExtensions().stream()
                .map(fileExportExtension -> new GeneratedFileFormat(format, fileExportMode, fileExportExtension))
                .collect(Collectors.toList());
    }

    private List<FileExportFormat>  getConcatenatedFormat(ConcatenatedList concatenatedList){
        List<FileExportFormat> concatenatedFormats = new ArrayList<>();
        if(concatenatedList.isFircosoftFormat() )
            concatenatedFormats.add(FileExportFormat.FIRCOSOFT);
        if(concatenatedList.isGenericFormat() )
            concatenatedFormats.add(FileExportFormat.GENERIC);
        if(concatenatedList.isReglissFormat() )
            concatenatedFormats.add(FileExportFormat.REGLISS);
        if(concatenatedList.isSafewatchFormat() )
            concatenatedFormats.add(FileExportFormat.SAFEWATCH);
        if(concatenatedList.isVigilanceFormat() )
            concatenatedFormats.add(FileExportFormat.VIGILANCE);

        return concatenatedFormats;
    }

    private List<FileExportFormat>  flterConcatenatedListFormat(ConcatenatedList concatenatedList, List<FileExportFormat> formats){
        List<FileExportFormat> finalFormats = new ArrayList<>();
        List<FileExportFormat> concatenatedListFormats = getConcatenatedFormat(concatenatedList);
        for(FileExportFormat format : formats){
            if(concatenatedListFormats.contains(format))
                finalFormats.add(format);
        }

        return finalFormats;
    }
}
