package com.inipage.homelylauncher.search;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root
public class AutoCompleteResult {
    @ElementList(inline = true)
    List<EntryWrapper> list;

    @Root(name = "CompleteSuggestion")
    public static class EntryWrapper {
        @Element(name = "suggestion")
        Entry entry;

        public Entry getEntry() {
            return entry;
        }
    }

    @Root
    public static class Entry {
        @Attribute(name = "data")
        String data;

        public String getData() {
            return data;
        }
    }

    public List<String> getAutocompleteResults(){
        List<String> result = new ArrayList<>();
        if(list == null) return result;

        for(EntryWrapper entry : list){
            {
                result.add(entry.getEntry().getData());
            }
        }
        return result;
    }
}
