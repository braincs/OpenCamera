package com.braincs.attrsc.opencamera;

import android.util.Log;

import org.junit.Test;

/**
 * Created by Shuai
 * 22/04/2020.
 */
public class TestImportantObject {

    @Test
    public void Test(){
        Parent man = new Man("man");
        Woman woman = new Woman("woman");
        man.setImportantObject(new ImportantObject("man important"));
        woman.setImportantObject(new ImportantObject("woman important"));

        ImportantObject importantObject = man.getImportantObject();
        Log.d("dbg","importantObject: " + importantObject.getName());
    }
}
