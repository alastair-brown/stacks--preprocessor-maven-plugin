package com.ensono.stacks.filter;


import lombok.Data;

@Data
public class FilterItem {

    private String packageName;
    private String properties;


    public FilterItem(String packageName, String properties) {
        this.packageName = packageName;
        this.properties = properties;
    }

    public FilterItem(String packageName) {
        this.packageName = packageName;
    }

    public boolean hasProperties() {
        return (this.properties !=null)?true:false;
    }
}
