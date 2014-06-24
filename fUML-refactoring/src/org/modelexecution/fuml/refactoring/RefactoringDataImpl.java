package org.modelexecution.fuml.refactoring;

import java.util.HashMap;
import java.util.Map;

public class RefactoringDataImpl implements RefactoringData {

    public Map<String, Object> refactoringData = new HashMap<>();

    @Override
    public Object get(String key) {
        return refactoringData.get(key);
    }

    @Override
    public void set(String key, Object value) {
        refactoringData.put(key, value);
    }

}
