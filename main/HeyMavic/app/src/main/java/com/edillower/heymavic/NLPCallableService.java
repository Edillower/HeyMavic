package com.edillower.heymavic;

import java.util.concurrent.Callable;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;

/**
 * Created by David on 2/5/2017.
 */

public class NLPCallableService implements Callable<String> {
    private NaturalLanguageClassifier service;
    private String classfier_id;
    private String command_text;

    NLPCallableService(NaturalLanguageClassifier service, String classfier_id, String command_text) {
        this.service = service;
        this.classfier_id = classfier_id;
        this.command_text = command_text;
    }

    public String call() throws Exception {
        String command = this.service
                .classify(this.classfier_id, this.command_text).execute()
                .getTopClass();
        return command;
    }
}
