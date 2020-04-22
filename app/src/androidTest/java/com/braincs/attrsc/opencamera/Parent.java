package com.braincs.attrsc.opencamera;

/**
 * Created by Shuai
 * 22/04/2020.
 */
abstract class Parent {
    private static ImportantObject importantObject;

    public void setImportantObject(ImportantObject importantObject) {
        this.importantObject = importantObject;
    }

    public ImportantObject getImportantObject(){
        return importantObject;
    }
}
